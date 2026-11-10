import { cn } from '@/lib/utils'
import { formatearDinero } from './formato'

interface Props {
  valor: number
  /** Precio destacado: Playfair, grande y en coral */
  grande?: boolean
  className?: string
}

/** Pesos colombianos sin decimales: "$1.240.000" */
export function Dinero({ valor, grande = false, className }: Props) {
  return (
    <span
      className={cn(
        'tabular-nums whitespace-nowrap',
        grande && 'font-titulo text-3xl font-semibold text-coral',
        className,
      )}
    >
      {formatearDinero(valor)}
    </span>
  )
}
