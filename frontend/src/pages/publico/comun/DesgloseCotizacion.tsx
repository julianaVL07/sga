import type { CotizacionResponse, TemporadaResponse } from '@/api/tipos'
import { Dinero, Fecha } from '@/components/coral'
import { cn } from '@/lib/utils'
import { PildoraTemporada } from './PildoraTemporada'
import { nombreDeTemporada, tipoDeTemporada } from './reglas'

interface Props {
  cotizacion: CotizacionResponse
  temporadas: TemporadaResponse[]
  /** Oculta la fila del total (cuando el resumen la muestra aparte) */
  sinTotal?: boolean
  className?: string
}

/** Noche por noche: temporada, ocupantes que pagan × tarifa y subtotal */
export function DesgloseCotizacion({ cotizacion, temporadas, sinTotal = false, className }: Props) {
  return (
    <div className={cn('flex flex-col gap-2', className)}>
      <ul className="flex flex-col divide-y divide-border" aria-label="Valor de cada noche">
        {cotizacion.detalles.map((noche) => (
          <li key={noche.fecha} className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 py-2 text-sm">
            <div className="flex flex-col gap-1">
              <Fecha valor={noche.fecha} className="font-medium" />
              <PildoraTemporada
                tipo={tipoDeTemporada(noche.temporada, temporadas)}
                nombre={nombreDeTemporada(noche.temporada, temporadas)}
                className="w-fit"
              />
            </div>
            <div className="flex flex-col items-end">
              <span className="text-xs text-muted-foreground tabular-nums">
                {noche.ocupantesFacturables} × <Dinero valor={noche.tarifaAplicada} />
              </span>
              <Dinero valor={noche.subtotal} className="font-semibold" />
            </div>
          </li>
        ))}
      </ul>
      {!sinTotal && (
        <div className="flex items-baseline justify-between border-t border-border pt-3">
          <span className="font-semibold">
            Total · {cotizacion.noches} {cotizacion.noches === 1 ? 'noche' : 'noches'}
          </span>
          <Dinero valor={cotizacion.valorTotal} grande className="text-2xl" />
        </div>
      )}
    </div>
  )
}
