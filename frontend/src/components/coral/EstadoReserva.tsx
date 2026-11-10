import type { EstadoReserva as Estado } from '@/api/tipos'
import { cn } from '@/lib/utils'

const ESTILOS: Record<Estado, { fondo: string; texto: string; etiqueta: string }> = {
  PENDIENTE: { fondo: '#FFF1C2', texto: '#6B4800', etiqueta: 'Pendiente' },
  CONFIRMADA: { fondo: '#DDF2EA', texto: '#1C5E45', etiqueta: 'Confirmada' },
  EN_CURSO: { fondo: '#FCE1EA', texto: '#8F2450', etiqueta: 'En curso' },
  FINALIZADA: { fondo: '#EFE7EC', texto: '#57434F', etiqueta: 'Finalizada' },
  CANCELADA: { fondo: '#F5E3E3', texto: '#7D2E2E', etiqueta: 'Cancelada' },
  NO_SHOW: { fondo: '#3D1F33', texto: '#FFFFFF', etiqueta: 'No se presentó' },
}

interface Props {
  estado: Estado
  className?: string
}

/** Píldora con punto para el estado de una reserva */
export function EstadoReserva({ estado, className }: Props) {
  const estilo = ESTILOS[estado]
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold whitespace-nowrap',
        className,
      )}
      style={{ backgroundColor: estilo.fondo, color: estilo.texto }}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {estilo.etiqueta}
    </span>
  )
}
