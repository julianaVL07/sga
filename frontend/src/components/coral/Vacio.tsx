import type { ReactNode } from 'react'
import { Inbox, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface Props {
  icono?: LucideIcon
  titulo: string
  descripcion?: ReactNode
  /** Botón o enlace opcional */
  accion?: ReactNode
  className?: string
}

/** Estado vacío: cuando una lista no tiene nada que mostrar */
export function Vacio({ icono: Icono = Inbox, titulo, descripcion, accion, className }: Props) {
  return (
    <div className={cn('flex flex-col items-center justify-center gap-3 px-6 py-12 text-center', className)}>
      <span className="flex size-16 items-center justify-center rounded-full bg-crema">
        <Icono aria-hidden="true" className="size-7 text-coral" />
      </span>
      <p className="font-titulo text-xl font-semibold">{titulo}</p>
      {descripcion && <p className="max-w-sm text-sm text-muted-foreground">{descripcion}</p>}
      {accion && <div className="mt-2">{accion}</div>}
    </div>
  )
}
