import { useState } from 'react'
import { CheckCircle2, ShieldCheck } from 'lucide-react'
import { useConflictosCanal, type ConflictoCanalResponse, type EstadoConflictoCanal } from '@/api/canales'
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
import { Skeleton } from '@/components/ui/skeleton'
import { claseSelect } from '@/pages/publico/comun/estilos'
import { DialogoResolverConflicto } from './DialogosCanal'
import { EtiquetaEstadoConflicto } from './etiquetas'
import { ESTADO_CONFLICTO } from './textos'

/** Reservas externas rechazadas por chocar con una vigente (RN-18); empieza mostrando las pendientes */
export function PestanaConflictos() {
  const [estado, setEstado] = useState<EstadoConflictoCanal | undefined>('PENDIENTE')
  const [pagina, setPagina] = useState(0)
  const consulta = useConflictosCanal(estado, pagina)
  const [elegido, setElegido] = useState<ConflictoCanalResponse | null>(null)

  const columnas: ColumnaTabla<ConflictoCanalResponse>[] = [
    { clave: 'fecha', titulo: 'Fecha', celda: (c) => <Fecha valor={c.fecha} /> },
    { clave: 'canal', titulo: 'Canal', celda: (c) => <span className="font-mono">{c.canal}</span> },
    {
      clave: 'reserva',
      titulo: 'Reserva del canal',
      ocultarEnTarjeta: true,
      celda: (c) => <span className="font-medium">{c.identificadorExterno}</span>,
    },
    {
      clave: 'detalle',
      titulo: 'Con qué chocó',
      className: 'whitespace-normal max-w-sm',
      celda: (c) => <span className="text-sm">{c.detalle}</span>,
    },
    {
      clave: 'estado',
      titulo: 'Estado',
      className: 'whitespace-normal',
      celda: (c) => (
        <span className="flex flex-col items-end gap-1 md:items-start">
          <EtiquetaEstadoConflicto estado={c.estado} />
          {c.estado === 'RESUELTO' && (
            <span className="text-xs text-muted-foreground">
              {c.resueltoPor}
              {c.fechaResolucion && (
                <>
                  {' · '}
                  <Fecha valor={c.fechaResolucion} />
                </>
              )}
              {c.motivoResolucion && <span className="block max-w-xs">“{c.motivoResolucion}”</span>}
            </span>
          )}
        </span>
      ),
    },
  ]

  return (
    <>
      <Campo id="filtro-estado-conflicto" etiqueta="Estado" className="mb-4 sm:w-48">
        <select
          className={claseSelect}
          value={estado ?? ''}
          onChange={(e) => {
            setEstado((e.target.value || undefined) as EstadoConflictoCanal | undefined)
            setPagina(0)
          }}
        >
          <option value="">Todos</option>
          {(Object.keys(ESTADO_CONFLICTO) as EstadoConflictoCanal[]).map((e) => (
            <option key={e} value={e}>
              {ESTADO_CONFLICTO[e]}
            </option>
          ))}
        </select>
      </Campo>

      {consulta.error ? (
        <AvisoError titulo="No se pudieron consultar los conflictos" error={consulta.error} />
      ) : !consulta.data ? (
        <Skeleton className="h-60" />
      ) : consulta.data.contenido.length === 0 ? (
        <Vacio
          icono={ShieldCheck}
          titulo={estado === 'PENDIENTE' ? 'Sin conflictos pendientes' : 'Sin conflictos'}
          descripcion="Cuando un canal envía una reserva sobre noches ya vendidas, se rechaza y aparece aquí."
        />
      ) : (
        <>
          <TablaResponsive
            descripcion="Conflictos de canal"
            columnas={columnas}
            filas={consulta.data.contenido}
            claveFila={(c) => c.id}
            tituloTarjeta={(c) => (
              <span className="flex items-center justify-between gap-2">
                <span>{c.identificadorExterno}</span>
                <EtiquetaEstadoConflicto estado={c.estado} />
              </span>
            )}
            acciones={(c) =>
              c.estado === 'PENDIENTE' ? (
                <Button size="sm" variant="outline" onClick={() => setElegido(c)}>
                  <CheckCircle2 aria-hidden="true" /> Resolver
                </Button>
              ) : null
            }
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

      <DialogoResolverConflicto
        conflicto={elegido}
        abierto={elegido !== null}
        alCambiar={(abierto) => !abierto && setElegido(null)}
      />
    </>
  )
}
