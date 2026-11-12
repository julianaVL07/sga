import { cn } from '@/lib/utils'
import { formatearPorcentaje } from './formato'

interface Props {
  /** De 0 a 100 */
  valor: number
  /** Nombre de lo que se mide, para lectores de pantalla */
  etiqueta: string
  className?: string
}

/** Barra horizontal hecha solo con CSS, con el porcentaje al lado */
export function BarraPorcentaje({ valor, etiqueta, className }: Props) {
  const ancho = Math.min(100, Math.max(0, valor))
  return (
    <div className={cn('flex min-w-36 items-center gap-3', className)}>
      <div
        role="meter"
        aria-label={etiqueta}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={ancho}
        aria-valuetext={formatearPorcentaje(valor)}
        className="h-2.5 flex-1 overflow-hidden rounded-full bg-muted"
      >
        <div className="h-full rounded-full bg-coral transition-[width] duration-300" style={{ width: `${ancho}%` }} />
      </div>
      <span className="w-16 shrink-0 text-right text-sm tabular-nums">{formatearPorcentaje(valor)}</span>
    </div>
  )
}
