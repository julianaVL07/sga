import { cn } from '@/lib/utils'

interface Props {
  className?: string
  /** "abajo": las ondas suben desde el borde inferior; "arriba": cuelgan del borde superior */
  posicion?: 'abajo' | 'arriba'
}

/** Ondas decorativas en coral, durazno y ciruela con transparencia */
export function Onda({ className, posicion = 'abajo' }: Props) {
  return (
    <svg
      aria-hidden="true"
      focusable="false"
      viewBox="0 0 1440 220"
      preserveAspectRatio="none"
      className={cn('pointer-events-none block w-full', posicion === 'arriba' && 'rotate-180', className)}
    >
      <path
        d="M0,120 C180,60 360,170 560,130 C760,90 900,20 1100,60 C1260,92 1360,140 1440,120 L1440,220 L0,220 Z"
        fill="#FBAA6B"
        fillOpacity="0.35"
      />
      <path
        d="M0,150 C200,110 340,200 600,160 C820,126 980,80 1180,120 C1300,144 1380,170 1440,160 L1440,220 L0,220 Z"
        fill="#E75480"
        fillOpacity="0.45"
      />
      <path
        d="M0,185 C240,160 420,215 700,190 C940,168 1120,150 1300,176 C1370,186 1410,196 1440,192 L1440,220 L0,220 Z"
        fill="#3D1F33"
        fillOpacity="0.55"
      />
    </svg>
  )
}
