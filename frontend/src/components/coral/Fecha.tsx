import { isValid } from 'date-fns'
import { cn } from '@/lib/utils'
import { aFecha, formatearFecha } from './formato'

interface Props {
  valor: string | Date
  /** Patrón de date-fns; por defecto "sáb 12 dic" */
  patron?: string
  className?: string
}

/** Fecha en español de Colombia: "sáb 12 dic" */
export function Fecha({ valor, patron, className }: Props) {
  const fecha = aFecha(valor)
  return (
    <time dateTime={isValid(fecha) ? fecha.toISOString() : undefined} className={cn('whitespace-nowrap', className)}>
      {formatearFecha(fecha, patron)}
    </time>
  )
}
