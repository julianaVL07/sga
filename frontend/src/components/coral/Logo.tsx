import logoCoral from '@/assets/logo-coral.png'
import { cn } from '@/lib/utils'

interface Props {
  /** Muestra "SISTEMA DE ALOJAMIENTOS" debajo del nombre */
  conSubtitulo?: boolean
  tamano?: 'sm' | 'md' | 'lg'
  /** Texto claro, para fondos ciruela */
  claro?: boolean
  className?: string
}

const TAMANOS = {
  sm: { imagen: 'size-8', nombre: 'text-xl', subtitulo: 'text-[0.55rem]' },
  md: { imagen: 'size-10', nombre: 'text-2xl', subtitulo: 'text-[0.6rem]' },
  lg: { imagen: 'size-16', nombre: 'text-4xl', subtitulo: 'text-xs' },
}

export function Logo({ conSubtitulo = false, tamano = 'md', claro = false, className }: Props) {
  const t = TAMANOS[tamano]
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <img src={logoCoral} alt="" className={cn(t.imagen, 'shrink-0 object-contain')} />
      <span className="flex flex-col leading-none">
        <span className={cn('font-titulo font-semibold', t.nombre, claro ? 'text-white' : 'text-foreground')}>
          Coral
        </span>
        {conSubtitulo && (
          <span
            className={cn(
              'mt-1 font-sans font-semibold tracking-[0.28em] uppercase',
              t.subtitulo,
              claro ? 'text-ciruela-texto/80' : 'text-muted-foreground',
            )}
          >
            Sistema de alojamientos
          </span>
        )}
      </span>
    </span>
  )
}
