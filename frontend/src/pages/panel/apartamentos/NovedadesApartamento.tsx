import { Link, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { useApartamento } from '@/api/publico'
import { ListaNovedades } from '@/pages/servicio/ListaNovedades'

export default function NovedadesApartamento() {
  const { id = '' } = useParams()
  const apartamento = useApartamento(id)

  return (
    <section className="mx-auto w-full max-w-3xl">
      <Link
        to={`/panel/apartamentos/${encodeURIComponent(id)}`}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft aria-hidden="true" className="size-4" /> {id}
      </Link>
      <h1 className="mt-2 font-titulo text-3xl font-semibold">Novedades</h1>
      <p className="text-sm text-muted-foreground">
        {apartamento.data ? `${apartamento.data.identificacion} · ${apartamento.data.nombre}` : id}
      </p>
      <ListaNovedades identificacion={id} className="mt-6" />
    </section>
  )
}
