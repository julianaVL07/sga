import type { ReactNode } from 'react'
import { TriangleAlert } from 'lucide-react'
import { mensajeDeError } from '@/api/cliente'
import { cn } from '@/lib/utils'

interface Props {
  /** Error recibido (por ejemplo, el ErrorApi de un 409); se muestra su mensaje */
  error?: unknown
  /** Mensaje directo, si no se pasa un error */
  mensaje?: ReactNode
  titulo?: string
  className?: string
}

/** Aviso amarillo para los mensajes de negocio del backend (409) */
export function AvisoError({ error, mensaje, titulo, className }: Props) {
  const texto = mensaje ?? (error !== undefined ? mensajeDeError(error) : null)
  if (!texto) return null

  return (
    <div
      role="alert"
      className={cn('flex gap-3 rounded-campo border px-4 py-3 text-sm', className)}
      style={{ backgroundColor: '#FFF1C2', borderColor: '#F0D27A', color: '#6B4800' }}
    >
      <TriangleAlert aria-hidden="true" className="mt-0.5 size-5 shrink-0" />
      <div>
        {titulo && <p className="font-semibold">{titulo}</p>}
        <p>{texto}</p>
      </div>
    </div>
  )
}
