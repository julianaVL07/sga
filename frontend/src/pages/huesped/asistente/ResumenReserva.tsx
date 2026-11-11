import type { ReactNode } from 'react'
import type { ApartamentoDetalleResponse, CotizacionResponse, TemporadaResponse } from '@/api/tipos'
import { AvisoError, Dinero, Fecha, FotoApartamento } from '@/components/coral'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { PildoraTemporada } from '@/pages/publico/comun/PildoraTemporada'
import {
  calcularAnticipo,
  formatearHora,
  horaDesde,
  nombreDeTemporada,
  textoPlazo,
  tipoDeTemporada,
  type ReglasAlojamiento,
} from '@/pages/publico/comun/reglas'

interface Props {
  apartamento: ApartamentoDetalleResponse | undefined
  entrada: string
  salida: string
  cotizacion: CotizacionResponse | undefined
  cargandoCotizacion: boolean
  errorCotizacion: unknown
  temporadas: TemporadaResponse[]
  reglas: ReglasAlojamiento
  hora: string
  mascotas: number
  className?: string
}

/** Valores de la reserva que se recalculan con cada cambio del asistente */
function calcularValores(
  cotizacion: CotizacionResponse | undefined,
  reglas: ReglasAlojamiento,
  hora: string,
  mascotas: number,
) {
  const estancia = cotizacion?.valorTotal ?? 0
  const aplicaNocturna = horaDesde(hora, reglas.horaNocturna)
  const aplicaAseo = mascotas > 0
  // Un cargo que aplica pero no trae valor en el alojamiento se muestra sin monto y no entra al total
  const nocturna = aplicaNocturna ? reglas.recargoNocturno : null
  const aseo = aplicaAseo ? reglas.cargoAseoMascota : null
  const deposito = reglas.depositoGarantia
  const sinMonto = (aplicaNocturna && nocturna === null) || (aplicaAseo && aseo === null) || deposito === null
  const anticipo = calcularAnticipo(estancia, reglas.porcentajeAnticipo)
  const total = estancia + (nocturna ?? 0) + (aseo ?? 0) + (deposito ?? 0)
  return { estancia, aplicaNocturna, nocturna, aplicaAseo, aseo, sinMonto, anticipo, deposito, total }
}

/** Resumen fijo a la derecha (abajo en el celular) */
export function ResumenReserva({
  apartamento,
  entrada,
  salida,
  cotizacion,
  cargandoCotizacion,
  errorCotizacion,
  temporadas,
  reglas,
  hora,
  mascotas,
  className,
}: Props) {
  const valores = calcularValores(cotizacion, reglas, hora, mascotas)

  return (
    <aside
      aria-labelledby="titulo-resumen"
      aria-live="polite"
      className={cn('flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta', className)}
    >
      <h2 id="titulo-resumen" className="font-titulo text-xl font-semibold">
        Resumen
      </h2>

      {apartamento ? (
        <div className="flex items-center gap-3">
          <FotoApartamento src={apartamento.imagenPrincipal} alt="" className="size-14 shrink-0 rounded-campo" />
          <div className="min-w-0">
            <p className="truncate font-semibold">{apartamento.nombre}</p>
            <p className="text-xs text-muted-foreground">{apartamento.identificacion}</p>
          </div>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">Elige un apartamento.</p>
      )}

      {entrada && salida && (
        <p className="text-sm">
          <Fecha valor={entrada} /> → <Fecha valor={salida} />
        </p>
      )}

      {errorCotizacion ? (
        <AvisoError error={errorCotizacion} />
      ) : !cotizacion ? (
        cargandoCotizacion ? (
          <Skeleton className="h-32" />
        ) : (
          <p className="text-sm text-muted-foreground">Completa las fechas y los huéspedes para ver el valor.</p>
        )
      ) : (
        <div className={cn('flex flex-col gap-4', cargandoCotizacion && 'opacity-60')}>
          <ul className="flex max-h-56 flex-col gap-2 overflow-y-auto pr-1" aria-label="Valor de cada noche">
            {cotizacion.detalles.map((n) => (
              <li key={n.fecha} className="flex items-center justify-between gap-2 text-sm">
                <span className="flex flex-wrap items-center gap-2">
                  <Fecha valor={n.fecha} />
                  <PildoraTemporada
                    tipo={tipoDeTemporada(n.temporada, temporadas)}
                    nombre={nombreDeTemporada(n.temporada, temporadas)}
                  />
                </span>
                <span className="flex flex-col items-end">
                  <span className="text-xs text-muted-foreground tabular-nums">
                    {n.ocupantesFacturables} × <Dinero valor={n.tarifaAplicada} />
                  </span>
                  <Dinero valor={n.subtotal} />
                </span>
              </li>
            ))}
          </ul>

          <dl className="flex flex-col gap-2 border-t border-border pt-3 text-sm">
            <Fila etiqueta={`Estancia · ${cotizacion.noches} ${cotizacion.noches === 1 ? 'noche' : 'noches'}`}>
              <Dinero valor={valores.estancia} className="font-semibold" />
            </Fila>
            {valores.aplicaNocturna && (
              <Fila etiqueta={`Recepción nocturna (llegada ${formatearHora(hora)})`}>
                {valores.nocturna !== null ? (
                  <Dinero valor={valores.nocturna} />
                ) : (
                  <span className="text-xs text-muted-foreground">Se suma el cargo</span>
                )}
              </Fila>
            )}
            {valores.aplicaAseo && (
              <Fila etiqueta="Aseo por mascotas">
                {valores.aseo !== null ? (
                  <Dinero valor={valores.aseo} />
                ) : (
                  <span className="text-xs text-muted-foreground">Se suma el cargo</span>
                )}
              </Fila>
            )}
            {valores.deposito !== null ? (
              <Fila etiqueta="Depósito de garantía" ayuda="Reembolsable al salir">
                <Dinero valor={valores.deposito} />
              </Fila>
            ) : (
              <Fila etiqueta="Depósito de garantía reembolsable">{null}</Fila>
            )}
            <div className="flex items-baseline justify-between gap-3 border-t border-border pt-3">
              <dt className="font-semibold">Total del folio</dt>
              <dd>
                <Dinero valor={valores.total} grande className="text-2xl" />
              </dd>
            </div>
            {valores.sinMonto && (
              <p className="text-xs text-muted-foreground">No incluye los cargos que se suman sin monto.</p>
            )}
          </dl>

          <div className="rounded-campo bg-crema p-3 text-sm">
            <p className="flex items-baseline justify-between gap-2 font-semibold">
              <span>
                Anticipo{reglas.porcentajeAnticipo !== null && ` del ${reglas.porcentajeAnticipo} %`}
              </span>
              {valores.anticipo !== null && <Dinero valor={valores.anticipo} />}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Se paga por transferencia {textoPlazo(reglas.plazoConfirmacionHoras)} para poder confirmarla.
            </p>
          </div>
        </div>
      )}
    </aside>
  )
}

function Fila({ etiqueta, ayuda, children }: { etiqueta: string; ayuda?: string; children: ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-3">
      <dt>
        {etiqueta}
        {ayuda && <span className="block text-xs text-muted-foreground">{ayuda}</span>}
      </dt>
      <dd>{children}</dd>
    </div>
  )
}
