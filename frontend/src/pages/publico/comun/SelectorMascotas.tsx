import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

interface Props {
  id: string
  etiqueta: string
  cantidad: number
  alCambiar: (cantidad: number) => void
  /** Máximo del alojamiento (L-16); si no vino, se escribe la cantidad y el backend la valida */
  maximo: number | null
}

/** Cantidad de mascotas: botones de 0 al máximo, o un campo numérico si el máximo no se conoce */
export function SelectorMascotas({ id, etiqueta, cantidad, alCambiar, maximo }: Props) {
  if (maximo === null) {
    return (
      <div className="flex flex-col gap-1.5 sm:max-w-60">
        <label htmlFor={id} className="text-sm font-medium">
          {etiqueta}
        </label>
        <Input
          id={id}
          type="number"
          inputMode="numeric"
          min={0}
          value={cantidad}
          onChange={(e) => alCambiar(Math.max(0, Number(e.target.value) || 0))}
        />
      </div>
    )
  }

  return (
    <fieldset>
      <legend className="mb-2 text-sm font-medium">{etiqueta}</legend>
      <div className="flex flex-wrap gap-2">
        {Array.from({ length: maximo + 1 }, (_, n) => (
          <button
            key={n}
            type="button"
            aria-pressed={cantidad === n}
            onClick={() => alCambiar(n)}
            className={cn(
              'size-11 rounded-full border text-sm font-semibold transition-colors',
              cantidad === n ? 'border-primary bg-primary text-primary-foreground' : 'border-border bg-card hover:bg-muted',
            )}
          >
            {n}
          </button>
        ))}
      </div>
    </fieldset>
  )
}
