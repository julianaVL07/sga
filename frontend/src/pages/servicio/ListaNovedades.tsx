import { useState } from 'react'
import { ScrollText } from 'lucide-react'
import { useNovedades, type GravedadNovedad } from '@/api/apartamentos'
import { AvisoError, Fecha, Paginador, Vacio } from '@/components/coral'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { GRAVEDADES } from './gravedades'

export function EtiquetaGravedad({ gravedad }: { gravedad: GravedadNovedad }) {
  const g = GRAVEDADES.find((x) => x.valor === gravedad) ?? GRAVEDADES[0]
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold whitespace-nowrap"
      style={{ backgroundColor: g.fondo, color: g.texto }}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      Gravedad {g.etiqueta.toLowerCase()}
    </span>
  )
}

/** Historial de novedades de un apartamento, de la más reciente a la más antigua, paginado */
export function ListaNovedades({ identificacion, className }: { identificacion: string; className?: string }) {
  const [pagina, setPagina] = useState(0)
  const consulta = useNovedades(identificacion, pagina)

  if (consulta.error) return <AvisoError className={className} error={consulta.error} />
  if (!consulta.data) {
    return (
      <div className={cn('flex flex-col gap-3', className)}>
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-24 w-full rounded-tarjeta" />
        ))}
      </div>
    )
  }
  if (consulta.data.contenido.length === 0) {
    return (
      <Vacio
        icono={ScrollText}
        titulo="Sin novedades"
        descripcion="Este apartamento no tiene daños, faltantes ni incidencias registradas."
        className={className}
      />
    )
  }

  return (
    <div className={className}>
      <ul className="flex flex-col gap-3">
        {consulta.data.contenido.map((n) => (
          <li key={n.id} className="rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <EtiquetaGravedad gravedad={n.gravedad} />
              <Fecha valor={n.fecha} patron="EEE d MMM yyyy" className="text-xs text-muted-foreground" />
            </div>
            <p className="mt-3 text-base">{n.descripcion}</p>
            <p className="mt-2 flex flex-wrap gap-x-3 text-xs text-muted-foreground">
              <span>Reportó {n.autor}</span>
              {n.fechaAtencion ? (
                <span>
                  Atendida el <Fecha valor={n.fechaAtencion} />
                </span>
              ) : (
                <span>Sin atender</span>
              )}
            </p>
          </li>
        ))}
      </ul>
      <Paginador
        className="mt-4"
        pagina={pagina}
        totalPaginas={consulta.data.totalPaginas}
        totalElementos={consulta.data.totalElementos}
        alCambiar={setPagina}
      />
    </div>
  )
}
