import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { CircleCheck } from 'lucide-react'
import type { ReservaDetalleResponse } from '@/api/reservas'
import type { ApartamentoDetalleResponse } from '@/api/tipos'
import { Dinero, EstadoReserva, Fecha } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { limiteAnticipo } from '@/pages/huesped/reserva/formato'
import { calcularAnticipo, type ReglasAlojamiento } from '@/pages/publico/comun/reglas'

interface Props {
  reserva: ReservaDetalleResponse
  modo: 'huesped' | 'panel'
  reglas: ReglasAlojamiento
  apartamento: ApartamentoDetalleResponse | undefined
}

/** Reserva creada: código y siguientes pasos */
export function ExitoReserva({ reserva, modo, reglas, apartamento }: Props) {
  const anticipo = calcularAnticipo(reserva.valorTotal, reglas.porcentajeAnticipo)
  const limite = limiteAnticipo(reserva.fechaCreacion, reglas.plazoConfirmacionHoras)
  const codigo = encodeURIComponent(reserva.codigo)
  const montoAnticipo =
    anticipo !== null ? (
      <>
        {' '}
        de <Dinero valor={anticipo} className="font-semibold" />
      </>
    ) : null
  const cuando = limite ? (
    <>
      antes de las <strong>{limite}</strong>
    </>
  ) : (
    'dentro del plazo de confirmación'
  )

  return (
    <section className="mx-auto flex w-full max-w-2xl flex-col gap-6" aria-labelledby="titulo-exito">
      <div className="flex flex-col items-center gap-3 rounded-tarjeta border border-border bg-card p-8 text-center shadow-tarjeta">
        <span className="flex size-16 items-center justify-center rounded-full bg-[#DDF2EA]">
          <CircleCheck aria-hidden="true" className="size-8 text-[#1C5E45]" />
        </span>
        <h1 id="titulo-exito" className="font-titulo text-3xl font-semibold">
          ¡Reserva creada!
        </h1>
        <p className="text-sm text-muted-foreground">Código de la reserva</p>
        <p className="font-titulo text-3xl font-semibold tracking-wide text-coral-texto">{reserva.codigo}</p>
        <EstadoReserva estado={reserva.estado} />
        <p className="text-sm">
          {apartamento?.nombre ?? reserva.identificacionApartamento} · <Fecha valor={reserva.fechaEntrada} /> →{' '}
          <Fecha valor={reserva.fechaSalida} /> · {reserva.totalOcupantes}{' '}
          {reserva.totalOcupantes === 1 ? 'huésped' : 'huéspedes'}
        </p>
        <p className="text-sm">
          Valor de la estancia: <Dinero valor={reserva.valorTotal} className="font-semibold" />
        </p>
      </div>

      <div className="rounded-tarjeta border border-border bg-card p-6 shadow-tarjeta">
        <h2 className="font-titulo text-xl font-semibold">Siguientes pasos</h2>
        <ol className="mt-4 flex flex-col gap-4 text-sm">
          {modo === 'huesped' ? (
            <>
              <Paso n={1}>
                Transfiere el anticipo{montoAnticipo}
                {reglas.porcentajeAnticipo !== null && ` (el ${reglas.porcentajeAnticipo} % de la estancia)`} {cuando}. Si no,
                la reserva vence.
              </Paso>
              <Paso n={2}>Envía el comprobante a recepción indicando el código {reserva.codigo}; ellos registran el pago.</Paso>
              <Paso n={3}>Con el anticipo registrado, recepción confirma tu reserva y te avisamos.</Paso>
              <Paso n={4}>
                Al llegar se cobra el saldo. El depósito de garantía
                {reglas.depositoGarantia !== null && (
                  <>
                    {' '}
                    de <Dinero valor={reglas.depositoGarantia} />
                  </>
                )}{' '}
                se te devuelve al salir.
              </Paso>
            </>
          ) : (
            <>
              <Paso n={1}>
                El huésped transfiere el anticipo{montoAnticipo} {cuando}.
              </Paso>
              <Paso n={2}>Registra el pago en el folio de la reserva.</Paso>
              <Paso n={3}>Confirma la reserva {reserva.horaEstimadaLlegada ? '' : 'con la hora estimada de llegada '}desde su detalle.</Paso>
            </>
          )}
        </ol>
      </div>

      <div className="flex flex-wrap justify-center gap-3">
        {modo === 'huesped' ? (
          <>
            <Button asChild>
              <Link to={`/mis-reservas/${codigo}`}>Ver mi reserva</Link>
            </Button>
            <Button asChild variant="outline">
              <Link to="/mis-reservas">Mis reservas</Link>
            </Button>
          </>
        ) : (
          <>
            <Button asChild>
              <Link to={`/panel/reservas/${codigo}`}>Ver la reserva</Link>
            </Button>
            <Button asChild variant="outline">
              <Link to={`/panel/reservas/${codigo}/folio`}>Ir al folio</Link>
            </Button>
          </>
        )}
      </div>
    </section>
  )
}

function Paso({ n, children }: { n: number; children: ReactNode }) {
  return (
    <li className="flex gap-3">
      <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-crema text-xs font-semibold text-coral-texto">
        {n}
      </span>
      <span className="pt-1">{children}</span>
    </li>
  )
}
