import { useEffect, useId, useRef, useState } from 'react'
import { ChevronDown, Plus, Trash2, Users } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import { resumenHuespedes } from './estancia'
import { esMenorNoPaga, hoyISO, textoMenores } from './reglas'

interface Props {
  /** Fecha de nacimiento de cada huésped ('' si no se indicó) */
  huespedes: string[]
  alCambiar: (huespedes: string[]) => void
  umbralEdad: number | null
  /** Fecha con la que se calcula la edad (la llegada); por defecto, hoy */
  referencia?: string
  maximo?: number
  /** Muestra un botón con el resumen que abre la lista; si no, la lista va siempre visible */
  desplegable?: boolean
  className?: string
}

/** Lista de huéspedes con su fecha de nacimiento; los menores del umbral no pagan noche */
export function SelectorHuespedes({
  huespedes,
  alCambiar,
  umbralEdad,
  referencia,
  maximo,
  desplegable = false,
  className,
}: Props) {
  const id = useId()
  const [abierto, setAbierto] = useState(false)
  const contenedor = useRef<HTMLDivElement>(null)

  // Cierra el desplegable al hacer clic afuera o con Escape
  useEffect(() => {
    if (!desplegable || !abierto) return
    const alClic = (e: MouseEvent) => {
      if (contenedor.current && !contenedor.current.contains(e.target as Node)) setAbierto(false)
    }
    const alTecla = (e: KeyboardEvent) => e.key === 'Escape' && setAbierto(false)
    document.addEventListener('mousedown', alClic)
    document.addEventListener('keydown', alTecla)
    return () => {
      document.removeEventListener('mousedown', alClic)
      document.removeEventListener('keydown', alTecla)
    }
  }, [desplegable, abierto])

  const cambiar = (i: number, valor: string) => alCambiar(huespedes.map((h, j) => (j === i ? valor : h)))
  const quitar = (i: number) => alCambiar(huespedes.filter((_, j) => j !== i))
  const agregar = () => alCambiar([...huespedes, ''])

  const lista = (
    <div className="flex flex-col gap-3">
      <ul className="flex flex-col gap-3">
        {huespedes.map((fecha, i) => {
          const menor = esMenorNoPaga(fecha, umbralEdad, referencia)
          return (
            <li key={i} className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between gap-2">
                <label htmlFor={`${id}-${i}`} className="text-sm font-medium">
                  Huésped {i + 1} <span className="font-normal text-muted-foreground">· fecha de nacimiento</span>
                </label>
                {fecha && umbralEdad !== null && (
                  <span
                    className={cn(
                      'rounded-full px-2 py-0.5 text-xs font-semibold whitespace-nowrap',
                      menor ? 'bg-[#DDF2EA] text-[#1C5E45]' : 'bg-muted text-muted-foreground',
                    )}
                  >
                    {menor ? `Menor de ${umbralEdad} · no paga noche` : 'Paga noche'}
                  </span>
                )}
              </div>
              <div className="flex gap-2">
                <Input
                  id={`${id}-${i}`}
                  type="date"
                  max={hoyISO()}
                  value={fecha}
                  onChange={(e) => cambiar(i, e.target.value)}
                />
                {huespedes.length > 1 && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label={`Quitar al huésped ${i + 1}`}
                    onClick={() => quitar(i)}
                  >
                    <Trash2 aria-hidden="true" />
                  </Button>
                )}
              </div>
            </li>
          )
        })}
      </ul>
      <Button type="button" variant="outline" onClick={agregar} disabled={maximo !== undefined && huespedes.length >= maximo}>
        <Plus aria-hidden="true" /> Agregar huésped
      </Button>
      <p className="text-xs text-muted-foreground">
        {textoMenores(umbralEdad)}. Sin fecha, el huésped se cuenta como adulto.
      </p>
    </div>
  )

  if (!desplegable) return <div className={className}>{lista}</div>

  return (
    <div ref={contenedor} className={cn('relative', className)}>
      <button
        type="button"
        aria-expanded={abierto}
        aria-controls={`${id}-panel`}
        onClick={() => setAbierto((a) => !a)}
        className="flex h-11 w-full items-center gap-2 rounded-campo border border-input bg-card px-3.5 text-left text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30"
      >
        <Users aria-hidden="true" className="size-4 shrink-0 text-coral" />
        <span className="flex-1 truncate">{resumenHuespedes(huespedes, umbralEdad, referencia)}</span>
        <ChevronDown aria-hidden="true" className={cn('size-4 transition-transform', abierto && 'rotate-180')} />
      </button>
      {abierto && (
        <div
          id={`${id}-panel`}
          className="absolute right-0 left-0 z-30 mt-2 rounded-tarjeta border border-border bg-popover p-4 shadow-tarjeta sm:left-auto sm:w-96"
        >
          {lista}
          <Button type="button" className="mt-3 w-full" onClick={() => setAbierto(false)}>
            Listo
          </Button>
        </div>
      )}
    </div>
  )
}
