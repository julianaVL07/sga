import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CircleCheck, FilePlus2, Loader2, ScrollText, Sparkles } from 'lucide-react'
import { toast } from 'sonner'
import { useIniciarPreparacion, usePendientesPreparacion, useTerminarPreparacion } from '@/api/apartamentos'
import type { ApartamentoResponse } from '@/api/tipos'
import { AvisoError, EstadoOperativo, FotoApartamento, Paginador, Vacio } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

export default function Pendientes() {
  const [pagina, setPagina] = useState(0)
  const consulta = usePendientesPreparacion(pagina)

  if (consulta.error) return <AvisoError error={consulta.error} />
  if (!consulta.data) {
    return (
      <div className="flex flex-col gap-4">
        {[0, 1].map((i) => (
          <Skeleton key={i} className="h-56 w-full rounded-tarjeta" />
        ))}
      </div>
    )
  }
  if (consulta.data.contenido.length === 0) {
    return (
      <Vacio
        icono={CircleCheck}
        titulo="Todo está preparado"
        descripcion="No hay apartamentos pendientes de preparación."
        accion={
          <Button asChild variant="outline" className="h-14 px-6 text-base">
            <Link to="/servicio/novedad">
              <FilePlus2 aria-hidden="true" className="size-5" /> Reportar novedad
            </Link>
          </Button>
        }
      />
    )
  }

  return (
    <section className="flex flex-col gap-4">
      <h1 className="sr-only">Apartamentos pendientes de preparación</h1>
      <p className="text-sm text-muted-foreground">
        {consulta.data.totalElementos} {consulta.data.totalElementos === 1 ? 'apartamento por preparar' : 'apartamentos por preparar'}
      </p>
      <ul className="flex flex-col gap-4">
        {consulta.data.contenido.map((a) => (
          <li key={a.identificacion}>
            <TarjetaPendiente apartamento={a} />
          </li>
        ))}
      </ul>
      <Paginador
        pagina={pagina}
        totalPaginas={consulta.data.totalPaginas}
        totalElementos={consulta.data.totalElementos}
        alCambiar={setPagina}
      />
    </section>
  )
}

function TarjetaPendiente({ apartamento: a }: { apartamento: ApartamentoResponse }) {
  const iniciar = useIniciarPreparacion()
  const terminar = useTerminarPreparacion()
  const enPreparacion = a.estadoOperativo === 'EN_PREPARACION'
  const accion = enPreparacion ? terminar : iniciar
  const id = encodeURIComponent(a.identificacion)

  const ejecutar = () =>
    accion.mutate(a.identificacion, {
      onSuccess: () =>
        toast.success(enPreparacion ? 'Apartamento preparado' : 'Preparación iniciada', { description: a.identificacion }),
    })

  return (
    <article className="overflow-hidden rounded-tarjeta border border-border bg-card shadow-tarjeta">
      <div className="flex items-center gap-4 p-4">
        <FotoApartamento src={a.imagenes[0]} alt="" className="size-16 shrink-0 rounded-campo bg-crema" />
        <div className="min-w-0 flex-1">
          <h2 className="font-titulo text-2xl leading-tight font-semibold">{a.identificacion}</h2>
          <p className="truncate text-sm text-muted-foreground">{a.nombre}</p>
          <EstadoOperativo estado={a.estadoOperativo} className="mt-1.5" />
        </div>
      </div>

      <div className="flex flex-col gap-3 px-4 pb-4">
        {accion.error && <AvisoError error={accion.error} />}
        <Button className="h-14 w-full text-base" disabled={accion.isPending} onClick={ejecutar}>
          {accion.isPending ? (
            <Loader2 aria-hidden="true" className="size-5 animate-spin" />
          ) : enPreparacion ? (
            <CircleCheck aria-hidden="true" className="size-5" />
          ) : (
            <Sparkles aria-hidden="true" className="size-5" />
          )}
          {enPreparacion ? 'Terminar preparación' : 'Iniciar preparación'}
        </Button>
        <div className="grid grid-cols-2 gap-3">
          <Button asChild variant="outline" className="h-14 text-base">
            <Link to={`/servicio/novedad?apartamento=${id}`}>
              <FilePlus2 aria-hidden="true" className="size-5" /> Novedad
            </Link>
          </Button>
          <Button asChild variant="outline" className="h-14 text-base">
            <Link to={`/servicio/apartamentos/${id}`}>
              <ScrollText aria-hidden="true" className="size-5" /> Historial
            </Link>
          </Button>
        </div>
      </div>
    </article>
  )
}
