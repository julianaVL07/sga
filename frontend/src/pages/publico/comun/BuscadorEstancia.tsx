import { useState, type FormEvent } from 'react'
import { Search } from 'lucide-react'
import { Campo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import type { Estancia } from './estancia'
import { hoyISO, nochesEntre, sumarDias } from './reglas'
import { SelectorHuespedes } from './SelectorHuespedes'

interface Props {
  inicial: Estancia
  umbralEdad: number | null
  alBuscar: (estancia: Estancia) => void
  textoBoton?: string
  className?: string
}

/** Llegada, salida y huéspedes; valida que la salida sea posterior a la llegada */
export function BuscadorEstancia({ inicial, umbralEdad, alBuscar, textoBoton = 'Buscar disponibles', className }: Props) {
  const [entrada, setEntrada] = useState(inicial.entrada)
  const [salida, setSalida] = useState(inicial.salida)
  const [huespedes, setHuespedes] = useState(inicial.huespedes)
  const [errores, setErrores] = useState<{ entrada?: string; salida?: string }>({})

  const enviar = (e: FormEvent) => {
    e.preventDefault()
    const nuevos: typeof errores = {}
    if (!entrada) nuevos.entrada = 'Elige la fecha de llegada.'
    else if (entrada < hoyISO()) nuevos.entrada = 'La llegada no puede ser en el pasado.'
    if (!salida) nuevos.salida = 'Elige la fecha de salida.'
    else if (entrada && nochesEntre(entrada, salida) < 1) nuevos.salida = 'La salida debe ser después de la llegada.'
    setErrores(nuevos)
    if (Object.keys(nuevos).length === 0) alBuscar({ entrada, salida, huespedes })
  }

  return (
    <form
      noValidate
      onSubmit={enviar}
      aria-label="Buscar apartamentos disponibles"
      className={cn(
        'grid gap-4 rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta sm:grid-cols-2 md:p-5 lg:grid-cols-[1fr_1fr_1.3fr_auto] lg:items-start',
        className,
      )}
    >
      <Campo id="buscar-entrada" etiqueta="Llegada" error={errores.entrada}>
        <Input
          type="date"
          min={hoyISO()}
          value={entrada}
          onChange={(e) => {
            const valor = e.target.value
            setEntrada(valor)
            // Si la salida queda antes de la llegada, se propone una noche
            if (valor && (!salida || salida <= valor)) setSalida(sumarDias(valor, 1))
          }}
        />
      </Campo>
      <Campo id="buscar-salida" etiqueta="Salida" error={errores.salida}>
        <Input
          type="date"
          min={entrada ? sumarDias(entrada, 1) : hoyISO()}
          value={salida}
          onChange={(e) => setSalida(e.target.value)}
        />
      </Campo>
      <div className="flex flex-col gap-1.5 sm:col-span-2 lg:col-span-1">
        <span className="text-sm font-medium" id="buscar-huespedes">
          Huéspedes
        </span>
        <div role="group" aria-labelledby="buscar-huespedes">
          <SelectorHuespedes
            desplegable
            huespedes={huespedes}
            alCambiar={setHuespedes}
            umbralEdad={umbralEdad}
            referencia={entrada}
          />
        </div>
      </div>
      <Button type="submit" className="h-11 sm:col-span-2 lg:col-span-1 lg:mt-[1.625rem]">
        <Search aria-hidden="true" /> {textoBoton}
      </Button>
    </form>
  )
}
