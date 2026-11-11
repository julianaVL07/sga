import type { TipoTemporada } from '@/api/tipos'
import { cn } from '@/lib/utils'

const ESTILOS: Record<TipoTemporada, { fondo: string; texto: string; etiqueta: string }> = {
  BASE: { fondo: '#EFE7EC', texto: '#57434F', etiqueta: 'Base' },
  MEDIA: { fondo: '#FFE4CF', texto: '#7A3E0C', etiqueta: 'Media' },
  ALTA: { fondo: '#FCE1EA', texto: '#8F2450', etiqueta: 'Alta' },
}

interface Props {
  tipo: TipoTemporada | null
  /** Texto a mostrar; por defecto "Temporada alta", "Temporada media"... */
  nombre?: string
  className?: string
}

/** Píldora con el color de la temporada de una noche */
export function PildoraTemporada({ tipo, nombre, className }: Props) {
  const estilo = tipo ? ESTILOS[tipo] : { fondo: '#EFE7EC', texto: '#57434F', etiqueta: '' }
  return (
    <span
      className={cn('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap', className)}
      style={{ backgroundColor: estilo.fondo, color: estilo.texto }}
    >
      {nombre ?? (tipo ? `Temporada ${estilo.etiqueta.toLowerCase()}` : 'Temporada')}
    </span>
  )
}
