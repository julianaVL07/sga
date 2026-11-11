import { differenceInCalendarDays, parseISO } from 'date-fns'
import type { FechaISO, RangoTemporadaResponse, TipoTemporada } from '@/api/tipos'
import { formatearFecha } from '@/components/coral'
import { sumarDias } from '@/pages/publico/comun/reglas'

/** Colores de la línea de tiempo: Base ciruela suave, Media durazno y Alta coral */
export const COLOR_TEMPORADA: Record<TipoTemporada, { barra: string; texto: string; etiqueta: string }> = {
  BASE: { barra: '#C9AFBD', texto: '#3D1F33', etiqueta: 'Base' },
  MEDIA: { barra: '#FBAA6B', texto: '#3D1F33', etiqueta: 'Media' },
  ALTA: { barra: '#E75480', texto: '#FFFFFF', etiqueta: 'Alta' },
}

export const TIPOS_TEMPORADA: TipoTemporada[] = ['BASE', 'MEDIA', 'ALTA']

/*
 * El backend guarda la fecha final EXCLUIDA (nota de A.3): el rango 2026-12-15 → 2027-01-16 cubre hasta el 15 de
 * enero. En pantalla se muestra y se pide la fecha final INCLUIDA, y se convierte al enviar.
 */

/** Fecha final excluida del backend → último día incluido, para mostrar */
export const finIncluido = (fechaFinExcluida: FechaISO): FechaISO => sumarDias(fechaFinExcluida, -1)

/** Último día incluido escrito por el usuario → fecha final excluida, para enviar */
export const finExcluido = (fechaFinIncluida: FechaISO): FechaISO => sumarDias(fechaFinIncluida, 1)

/** "15 dic 2026 – 15 ene 2027", con la fecha final incluida */
export function textoRango(rango: RangoTemporadaResponse): string {
  return `${formatearFecha(rango.fechaInicio, 'd MMM yyyy')} – ${formatearFecha(finIncluido(rango.fechaFin), 'd MMM yyyy')}`
}

/** Noches que cubre un rango (fecha final excluida) */
export const diasDeRango = (rango: RangoTemporadaResponse): number =>
  differenceInCalendarDays(parseISO(rango.fechaFin), parseISO(rango.fechaInicio))

/** Posición de un rango dentro de un año, en porcentaje; null si no toca ese año */
export function segmentoEnAnio(rango: RangoTemporadaResponse, anio: number): { izquierda: number; ancho: number } | null {
  const inicioAnio = parseISO(`${anio}-01-01`)
  const diasAnio = differenceInCalendarDays(parseISO(`${anio + 1}-01-01`), inicioAnio)
  const desde = Math.max(0, differenceInCalendarDays(parseISO(rango.fechaInicio), inicioAnio))
  const hasta = Math.min(diasAnio, differenceInCalendarDays(parseISO(rango.fechaFin), inicioAnio))
  if (hasta <= desde) return null
  return { izquierda: (desde / diasAnio) * 100, ancho: ((hasta - desde) / diasAnio) * 100 }
}
