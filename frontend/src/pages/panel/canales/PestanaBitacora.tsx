import { useState } from 'react'
import { ScrollText } from 'lucide-react'
import { useBitacoraCanales, type EventoBitacoraResponse, type FiltrosBitacora } from '@/api/canales'
import {
  AvisoError,
  Campo,
  Fecha,
  Paginador,
  TablaResponsive,
  Vacio,
  type ColumnaTabla,
} from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { EtiquetaResultado } from './etiquetas'
import { PATRON_FECHA_HORA, TIPO_EVENTO } from './textos'

/** Bitácora de mensajes con los canales (7.8), incluidos los intentos con credencial inválida */
export function PestanaBitacora() {
  const [filtros, setFiltros] = useState<FiltrosBitacora>({})
  const [pagina, setPagina] = useState(0)
  const consulta = useBitacoraCanales(filtros, pagina)

  const filtrar = (clave: keyof FiltrosBitacora, valor: string) => {
    setFiltros((f) => ({ ...f, [clave]: valor.trim() || undefined }))
    setPagina(0)
  }
  const hayFiltros = Object.values(filtros).some(Boolean)
  const fechasInvertidas = !!filtros.desde && !!filtros.hasta && filtros.desde > filtros.hasta

  const columnas: ColumnaTabla<EventoBitacoraResponse>[] = [
    {
      clave: 'fecha',
      titulo: 'Fecha',
      ocultarEnTarjeta: true,
      celda: (e) => <Fecha valor={e.fecha} patron={PATRON_FECHA_HORA} />,
    },
    { clave: 'canal', titulo: 'Canal', celda: (e) => <span className="font-mono">{e.canal}</span> },
    { clave: 'tipo', titulo: 'Operación', celda: (e) => TIPO_EVENTO[e.tipoEvento] },
    {
      clave: 'identificador',
      titulo: 'Reserva del canal',
      celda: (e) => e.identificadorExterno ?? <span className="text-muted-foreground">—</span>,
    },
    { clave: 'resultado', titulo: 'Resultado', celda: (e) => <EtiquetaResultado resultado={e.resultado} /> },
  ]

  return (
    <>
      <div className="mb-4 grid grid-cols-2 gap-3 sm:flex sm:flex-wrap sm:items-end">
        <Campo id="filtro-canal" etiqueta="Canal" className="col-span-2 sm:w-48">
          <Input
            autoComplete="off"
            placeholder="Todos"
            value={filtros.canal ?? ''}
            onChange={(e) => filtrar('canal', e.target.value)}
          />
        </Campo>
        <Campo id="filtro-desde" etiqueta="Desde" className="sm:w-44">
          <Input type="date" value={filtros.desde ?? ''} onChange={(e) => filtrar('desde', e.target.value)} />
        </Campo>
        <Campo
          id="filtro-hasta"
          etiqueta="Hasta"
          className="sm:w-44"
          error={fechasInvertidas ? 'Debe ser igual o posterior a la fecha desde' : undefined}
        >
          <Input type="date" value={filtros.hasta ?? ''} onChange={(e) => filtrar('hasta', e.target.value)} />
        </Campo>
        {hayFiltros && (
          <Button
            variant="ghost"
            className="col-span-2 sm:col-span-1"
            onClick={() => {
              setFiltros({})
              setPagina(0)
            }}
          >
            Limpiar filtros
          </Button>
        )}
      </div>

      {consulta.error ? (
        <AvisoError titulo="No se pudo consultar la bitácora" error={consulta.error} />
      ) : !consulta.data ? (
        <Skeleton className="h-60" />
      ) : consulta.data.contenido.length === 0 ? (
        <Vacio
          icono={ScrollText}
          titulo="Sin eventos"
          descripcion={hayFiltros ? 'No hay eventos con estos filtros.' : 'Ningún canal ha enviado mensajes todavía.'}
        />
      ) : (
        <>
          <TablaResponsive
            descripcion="Bitácora de los canales"
            columnas={columnas}
            filas={consulta.data.contenido}
            claveFila={(e) => e.id}
            tituloTarjeta={(e) => (
              <span className="flex items-center justify-between gap-2">
                <Fecha valor={e.fecha} patron={PATRON_FECHA_HORA} />
                <EtiquetaResultado resultado={e.resultado} />
              </span>
            )}
          />
          <Paginador
            className="mt-3"
            pagina={pagina}
            totalPaginas={consulta.data.totalPaginas}
            totalElementos={consulta.data.totalElementos}
            alCambiar={setPagina}
          />
        </>
      )}
    </>
  )
}
