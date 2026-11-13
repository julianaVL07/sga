import type { CellHookData } from 'jspdf-autotable'
import type {
  PeriodoReporte,
  ReporteIngresosResponse,
  ReporteOcupacionResponse,
  ReporteReservasPorCanalResponse,
} from '@/api/reportes'
import { formatearDinero, formatearFecha } from '@/components/coral'
import { NOMBRE_CANAL } from '@/pages/huesped/reserva/formato'
import { NOMBRE_MEDIO } from '@/pages/panel/folio/movimientos'
import { formatearPorcentaje } from './formato'

export interface DatosReportePdf {
  alojamiento: string
  periodo: PeriodoReporte
  ocupacion: ReporteOcupacionResponse
  ingresos: ReporteIngresosResponse
  canales: ReporteReservasPorCanalResponse
}

// Color de la marca (#8e3b6f, el mismo del mapa) para los encabezados de las tablas
const COLOR_ENCABEZADO: [number, number, number] = [142, 59, 111]
const MARGEN = 14

/** Las fuentes estándar del PDF no traen los espacios finos que usa Intl; se cambian por espacios normales */
function texto(valor: string): string {
  return valor.replace(/[  ]/g, ' ')
}

const dinero = (valor: number) => texto(formatearDinero(valor))
const porcentaje = (valor: number) => texto(formatearPorcentaje(valor))

/**
 * Genera y descarga el PDF con las tres tablas de la pantalla de reportes. jspdf y jspdf-autotable se cargan aquí,
 * con import dinámico, solo cuando el administrador pulsa "Descargar PDF".
 */
export async function descargarReportePdf(datos: DatosReportePdf): Promise<void> {
  const [{ jsPDF }, { autoTable }] = await Promise.all([import('jspdf'), import('jspdf-autotable')])
  const { alojamiento, periodo, ocupacion, ingresos, canales } = datos
  const doc = new jsPDF({ unit: 'mm', format: 'a4' })

  doc.setFontSize(18)
  doc.text(texto(`Reporte de ${alojamiento}`), MARGEN, 20)
  doc.setFontSize(11)
  doc.setTextColor(90)
  doc.text(
    texto(
      `Del ${formatearFecha(periodo.desde, "d 'de' MMMM yyyy")} al ${formatearFecha(periodo.hasta, "d 'de' MMMM yyyy")}` +
        ' (las dos fechas incluidas)',
    ),
    MARGEN,
    27,
  )
  doc.setTextColor(0)

  let y = 36
  /** Título y resumen de cada tabla; devuelve dónde empieza la tabla */
  const seccion = (titulo: string, resumen: string) => {
    if (y > 250) {
      doc.addPage()
      y = 20
    }
    doc.setFontSize(14)
    doc.text(titulo, MARGEN, y)
    doc.setFontSize(10)
    doc.setTextColor(90)
    doc.text(texto(resumen), MARGEN, y + 6)
    doc.setTextColor(0)
    return y + 10
  }
  const estilos = {
    margin: { left: MARGEN, right: MARGEN },
    headStyles: { fillColor: COLOR_ENCABEZADO },
    footStyles: { fillColor: [235, 240, 242] as [number, number, number], textColor: 20 },
    styles: { fontSize: 9 },
    // Todas las columnas menos la primera son números: encabezado, valores y total alineados a la derecha
    didParseCell: (celda: CellHookData) => {
      if (celda.column.index > 0) celda.cell.styles.halign = 'right'
    },
  }
  /** jspdf-autotable deja en el documento dónde terminó la última tabla */
  const finTabla = () => (doc as unknown as { lastAutoTable: { finalY: number } }).lastAutoTable.finalY + 12

  // ---------- Ocupación ----------
  autoTable(doc, {
    ...estilos,
    startY: seccion(
      'Ocupación',
      `${ocupacion.nochesVendidas} de ${ocupacion.nochesDisponibles} noches vendidas ` +
        `(${ocupacion.apartamentosActivos} apartamentos × ${ocupacion.nochesPeriodo} noches): ` +
        `${porcentaje(ocupacion.porcentajeOcupacion)} de ocupación total`,
    ),
    head: [['Apartamento', 'Noches vendidas', 'Ocupación']],
    body: ocupacion.apartamentos.map((a) => [
      a.activo ? a.apartamento : `${a.apartamento} (no está a la venta)`,
      String(a.nochesVendidas),
      porcentaje(a.porcentajeOcupacion),
    ]),
  })
  y = finTabla()

  // ---------- Ingresos ----------
  autoTable(doc, {
    ...estilos,
    startY: seccion('Ingresos', `${ingresos.cantidadPagos} ${ingresos.cantidadPagos === 1 ? 'pago' : 'pagos'} en el periodo`),
    head: [['Medio de pago', 'Pagos', 'Total', 'Participación']],
    body: ingresos.medios.map((m) => [
      NOMBRE_MEDIO[m.medio],
      String(m.cantidadPagos),
      dinero(m.total),
      porcentaje(m.porcentaje),
    ]),
    foot: [['Total', String(ingresos.cantidadPagos), dinero(ingresos.total), '']],
  })
  y = finTabla()

  // ---------- Reservas por canal ----------
  autoTable(doc, {
    ...estilos,
    startY: seccion(
      'Reservas por canal',
      `${canales.totalReservas} ${canales.totalReservas === 1 ? 'reserva' : 'reservas'} con entrada en el periodo`,
    ),
    head: [['Canal', 'Reservas', 'Activas', 'Finalizadas', 'Canceladas', 'No-show', 'Participación']],
    body: canales.canales.map((c) => [
      NOMBRE_CANAL[c.canal],
      String(c.total),
      String(c.activas),
      String(c.finalizadas),
      String(c.canceladas),
      String(c.noShow),
      porcentaje(c.porcentaje),
    ]),
  })

  doc.save(`reporte-coral-${periodo.desde}-${periodo.hasta}.pdf`)
}
