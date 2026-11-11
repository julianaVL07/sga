import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BedDouble, Users } from 'lucide-react'
import { FotoApartamento } from '@/components/coral'
import { Button } from '@/components/ui/button'

interface Props {
  enlace: string
  foto: string | null | undefined
  identificacion: string
  nombre: string
  capacidad: number
  dormitorios?: number
  /** Precio o valor estimado, ya formateado */
  precio?: ReactNode
  textoBoton?: string
}

/** Tarjeta de un apartamento para el inicio y la búsqueda */
export function TarjetaApartamento({
  enlace,
  foto,
  identificacion,
  nombre,
  capacidad,
  dormitorios,
  precio,
  textoBoton = 'Ver detalle',
}: Props) {
  return (
    <article className="group flex flex-col overflow-hidden rounded-tarjeta border border-border bg-card shadow-tarjeta">
      <div className="relative aspect-[4/3] overflow-hidden">
        <FotoApartamento
          src={foto}
          alt={`Foto del apartamento ${nombre}`}
          className="size-full transition-transform duration-300 group-hover:scale-105"
        />
        <span className="absolute top-3 left-3 rounded-full bg-ciruela/85 px-2.5 py-1 text-xs font-semibold text-white">
          {identificacion}
        </span>
      </div>
      <div className="flex flex-1 flex-col gap-3 p-5">
        <h3 className="font-titulo text-xl font-semibold">{nombre}</h3>
        <p className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-1.5">
            <Users aria-hidden="true" className="size-4 text-coral" /> Hasta {capacidad}{' '}
            {capacidad === 1 ? 'persona' : 'personas'}
          </span>
          {dormitorios !== undefined && (
            <span className="inline-flex items-center gap-1.5">
              <BedDouble aria-hidden="true" className="size-4 text-coral" /> {dormitorios}{' '}
              {dormitorios === 1 ? 'dormitorio' : 'dormitorios'}
            </span>
          )}
        </p>
        <div className="mt-auto flex flex-wrap items-end justify-between gap-3 pt-2">
          <div className="text-sm">{precio}</div>
          <Button asChild variant="outline">
            <Link to={enlace} aria-label={`${textoBoton} de ${nombre}`}>
              {textoBoton}
            </Link>
          </Button>
        </div>
      </div>
    </article>
  )
}
