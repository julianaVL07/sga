import { useRef, useState, type KeyboardEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Network, Plus, ScrollText, TriangleAlert, type LucideIcon } from 'lucide-react'
import type { CredencialCanalResponse } from '@/api/canales'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { DialogoCrearCanal, DialogoCredencial } from './DialogosCanal'
import { PestanaBitacora } from './PestanaBitacora'
import { PestanaCanales } from './PestanaCanales'
import { PestanaConflictos } from './PestanaConflictos'

type ClavePestana = 'canales' | 'conflictos' | 'bitacora'

const PESTANAS: { clave: ClavePestana; etiqueta: string; icono: LucideIcon }[] = [
  { clave: 'canales', etiqueta: 'Canales', icono: Network },
  { clave: 'conflictos', etiqueta: 'Conflictos', icono: TriangleAlert },
  { clave: 'bitacora', etiqueta: 'Bitácora', icono: ScrollText },
]

const esPestana = (valor: string | null): valor is ClavePestana => PESTANAS.some((p) => p.clave === valor)

/** Canales de venta externos: los canales y su credencial, los conflictos (RN-18) y la bitácora (7.8) */
export default function Canales() {
  const [params, setParams] = useSearchParams()
  const pestanaUrl = params.get('pestana')
  const activa: ClavePestana = esPestana(pestanaUrl) ? pestanaUrl : 'canales'
  const botones = useRef<Record<string, HTMLButtonElement | null>>({})

  const [creando, setCreando] = useState(false)
  // La credencial solo vive aquí mientras se muestra; al cerrar el diálogo se descarta
  const [credencial, setCredencial] = useState<CredencialCanalResponse | null>(null)

  const elegir = (clave: ClavePestana) => {
    const siguientes = new URLSearchParams(params)
    if (clave === 'canales') siguientes.delete('pestana')
    else siguientes.set('pestana', clave)
    setParams(siguientes, { replace: true })
  }

  // Flechas izquierda y derecha para moverse entre pestañas
  const teclado = (evento: KeyboardEvent, indice: number) => {
    const delta = evento.key === 'ArrowRight' ? 1 : evento.key === 'ArrowLeft' ? -1 : 0
    if (!delta) return
    evento.preventDefault()
    const siguiente = PESTANAS[(indice + delta + PESTANAS.length) % PESTANAS.length].clave
    elegir(siguiente)
    botones.current[siguiente]?.focus()
  }

  return (
    <section className="mx-auto flex w-full max-w-6xl flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-titulo text-3xl font-semibold">Canales de venta</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Plataformas externas que venden el inventario. Cada una se autentica con su propia credencial.
          </p>
        </div>
        <Button onClick={() => setCreando(true)}>
          <Plus aria-hidden="true" /> Nuevo canal
        </Button>
      </header>

      <div className="rounded-tarjeta border border-border bg-card shadow-tarjeta">
        <div role="tablist" aria-label="Canales de venta" className="flex gap-1 overflow-x-auto border-b border-border px-2 pt-2">
          {PESTANAS.map((p, i) => {
            const Icono = p.icono
            const seleccionada = activa === p.clave
            return (
              <button
                key={p.clave}
                ref={(el) => {
                  botones.current[p.clave] = el
                }}
                id={`pestana-${p.clave}`}
                role="tab"
                type="button"
                aria-selected={seleccionada}
                aria-controls={`panel-${p.clave}`}
                tabIndex={seleccionada ? 0 : -1}
                onClick={() => elegir(p.clave)}
                onKeyDown={(e) => teclado(e, i)}
                className={cn(
                  'relative flex min-h-11 shrink-0 items-center gap-2 rounded-t-campo px-3 text-sm font-medium transition-colors duration-150 outline-none focus-visible:ring-3 focus-visible:ring-ring/50',
                  'after:absolute after:inset-x-2 after:-bottom-px after:h-0.5 after:rounded-full',
                  seleccionada
                    ? 'text-foreground after:bg-coral'
                    : 'text-muted-foreground after:bg-transparent hover:text-foreground',
                )}
              >
                <Icono aria-hidden="true" className="size-4" />
                {p.etiqueta}
              </button>
            )
          })}
        </div>

        <div
          id={`panel-${activa}`}
          role="tabpanel"
          aria-labelledby={`pestana-${activa}`}
          tabIndex={0}
          className="p-3 outline-none md:p-4"
        >
          {activa === 'canales' && <PestanaCanales alEmitir={setCredencial} />}
          {activa === 'conflictos' && <PestanaConflictos />}
          {activa === 'bitacora' && <PestanaBitacora />}
        </div>
      </div>

      <DialogoCrearCanal abierto={creando} alCambiar={setCreando} alCrear={setCredencial} />
      <DialogoCredencial credencial={credencial} alCerrar={() => setCredencial(null)} />
    </section>
  )
}
