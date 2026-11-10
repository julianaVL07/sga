import { BedDouble, CircleCheck, Clock, Sparkles, Wrench, type LucideIcon } from 'lucide-react'
import type { EstadoOperativo as Estado } from '@/api/tipos'
import { cn } from '@/lib/utils'

interface Estilo {
  fondo: string
  texto: string
  icono: LucideIcon
  colorIcono: string
  etiqueta: string
}

const ESTILOS: Record<Estado, Estilo> = {
  PREPARADO: { fondo: '#DDF2EA', texto: '#1C5E45', icono: CircleCheck, colorIcono: '#1C5E45', etiqueta: 'Preparado' },
  OCUPADO: { fondo: '#FCE1EA', texto: '#8F2450', icono: BedDouble, colorIcono: '#E75480', etiqueta: 'Ocupado' },
  PENDIENTE_PREPARACION: {
    fondo: '#FFF1C2',
    texto: '#6B4800',
    icono: Clock,
    colorIcono: '#B07D00',
    etiqueta: 'Pendiente de preparación',
  },
  EN_PREPARACION: { fondo: '#FFE4CF', texto: '#7A3E0C', icono: Sparkles, colorIcono: '#D9792F', etiqueta: 'En preparación' },
  FUERA_DE_SERVICIO: {
    fondo: '#4A3542',
    texto: '#FFFFFF',
    icono: Wrench,
    colorIcono: '#FFFFFF',
    etiqueta: 'Fuera de servicio',
  },
}

interface Props {
  estado: Estado
  className?: string
}

/** Estado operativo de un apartamento, con ícono */
export function EstadoOperativo({ estado, className }: Props) {
  const estilo = ESTILOS[estado]
  const Icono = estilo.icono
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold whitespace-nowrap',
        className,
      )}
      style={{ backgroundColor: estilo.fondo, color: estilo.texto }}
    >
      <Icono aria-hidden="true" className="size-3.5" style={{ color: estilo.colorIcono }} />
      {estilo.etiqueta}
    </span>
  )
}
