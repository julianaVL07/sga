import { Construction } from 'lucide-react'
import { Vacio } from './Vacio'

interface Props {
  titulo: string
}

/** Página que todavía no tiene contenido */
export function EnConstruccion({ titulo }: Props) {
  return (
    <section className="mx-auto w-full max-w-5xl">
      <h1 className="font-titulo text-3xl font-semibold">{titulo}</h1>
      <div className="mt-6 rounded-tarjeta border border-border bg-card shadow-tarjeta">
        <Vacio icono={Construction} titulo="En construcción" descripcion="Esta página estará disponible pronto." />
      </div>
    </section>
  )
}
