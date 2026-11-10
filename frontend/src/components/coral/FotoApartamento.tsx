import { useState } from 'react'
import { cn } from '@/lib/utils'
import { Onda } from './Onda'

interface Props {
  src?: string | null
  alt: string
  className?: string
}

/** Foto de un apartamento; si no hay URL o no carga, muestra un fondo crema con ondas */
export function FotoApartamento({ src, alt, className }: Props) {
  const [fallo, setFallo] = useState(false)
  const [srcAnterior, setSrcAnterior] = useState(src)

  // Si cambia la URL, se vuelve a intentar
  if (src !== srcAnterior) {
    setSrcAnterior(src)
    setFallo(false)
  }

  if (!src || fallo) {
    return (
      <div
        role="img"
        aria-label={alt}
        className={cn('relative flex items-end overflow-hidden bg-crema', className)}
      >
        <Onda className="h-1/2" />
      </div>
    )
  }

  return (
    <img
      src={src}
      alt={alt}
      loading="lazy"
      onError={() => setFallo(true)}
      className={cn('object-cover', className)}
    />
  )
}
