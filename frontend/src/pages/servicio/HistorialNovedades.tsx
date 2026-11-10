import { Link, useParams } from 'react-router-dom'
import { FilePlus2 } from 'lucide-react'
import { useApartamento } from '@/api/publico'
import { EstadoOperativo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { ListaNovedades } from './ListaNovedades'

export default function HistorialNovedades() {
  const { id = '' } = useParams()
  const apartamento = useApartamento(id)

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="font-titulo text-2xl font-semibold">{id}</h1>
          {apartamento.data && <p className="text-sm text-muted-foreground">{apartamento.data.nombre}</p>}
        </div>
        {apartamento.data && <EstadoOperativo estado={apartamento.data.estadoOperativo} />}
      </div>

      <Button asChild className="h-14 w-full text-base">
        <Link to={`/servicio/novedad?apartamento=${encodeURIComponent(id)}`}>
          <FilePlus2 aria-hidden="true" className="size-5" /> Reportar novedad
        </Link>
      </Button>

      <ListaNovedades identificacion={id} />
    </section>
  )
}
