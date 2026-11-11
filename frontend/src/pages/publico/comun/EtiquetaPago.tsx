import { cn } from '@/lib/utils'
import { esMenorNoPaga } from './reglas'

interface Props {
  fecha: string
  /** Umbral de edad del alojamiento; si no vino, no se puede saber quién paga y no se muestra la etiqueta */
  umbral: number | null
  /** Fecha con la que se calcula la edad (la llegada) */
  referencia?: string
}

/** "Paga noche" o "Menor de 6 · $0" (RN-06) */
export function EtiquetaPago({ fecha, umbral, referencia }: Props) {
  if (umbral === null) return null
  const menor = esMenorNoPaga(fecha, umbral, referencia)
  return (
    <span
      className={cn(
        'rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap',
        menor ? 'bg-[#DDF2EA] text-[#1C5E45]' : 'bg-muted text-muted-foreground',
      )}
    >
      {menor ? `Menor de ${umbral} · $0` : 'Paga noche'}
    </span>
  )
}
