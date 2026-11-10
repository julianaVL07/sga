import { Link } from 'react-router-dom'
import { Compass } from 'lucide-react'
import { Vacio } from '@/components/coral'
import { Button } from '@/components/ui/button'

export default function NoEncontrada() {
  return (
    <div className="mx-auto max-w-xl rounded-tarjeta border border-border bg-card shadow-tarjeta">
      <h1 className="sr-only">Página no encontrada</h1>
      <p aria-hidden="true" className="pt-10 text-center font-titulo text-6xl font-semibold text-coral">
        404
      </p>
      <Vacio
        icono={Compass}
        titulo="No encontramos esta página"
        descripcion="Puede que la dirección esté mal escrita o que la página ya no exista."
        accion={
          <Button asChild>
            <Link to="/">Ir al inicio</Link>
          </Button>
        }
        className="pt-4"
      />
    </div>
  )
}
