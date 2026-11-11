import { useState } from 'react'
import { Clock } from 'lucide-react'
import type { FolioResponse, ReservaDetalleResponse } from '@/api/reservas'
import { Dinero } from '@/components/coral'
import { fechaLimiteAnticipo, limiteAnticipo } from './formato'
import { calcularAnticipo, textoPorcentajeAnticipo, type ReglasAlojamiento } from '@/pages/publico/comun/reglas'

interface Props {
  reserva: ReservaDetalleResponse
  folio: FolioResponse | undefined
  reglas: ReglasAlojamiento
  /** Texto para el huésped o para la recepción */
  modo: 'huesped' | 'panel'
}

/** Reserva pendiente: el anticipo que falta y su plazo */
export function AvisoAnticipo({ reserva, folio, reglas, modo }: Props) {
  // Hora de referencia fijada al montar, para no recalcular en cada render
  const [ahora] = useState(() => Date.now())
  if (reserva.estado !== 'PENDIENTE') return null
  const anticipo = calcularAnticipo(reserva.valorTotal, reglas.porcentajeAnticipo)
  const pagado = folio?.totalPagado ?? 0
  // Sin porcentaje configurado no se sabe cuánto falta: se avisa sin monto
  const falta = anticipo !== null ? Math.max(0, anticipo - pagado) : null
  const limite = limiteAnticipo(reserva.fechaCreacion, reglas.plazoConfirmacionHoras)
  const fechaLimite = fechaLimiteAnticipo(reserva.fechaCreacion, reglas.plazoConfirmacionHoras)
  const vencido = fechaLimite !== null && fechaLimite.getTime() < ahora
  const pendiente = falta === null || falta > 0

  return (
    <div
      role="status"
      className="flex gap-3 rounded-campo border px-4 py-3 text-sm"
      style={{ backgroundColor: '#FFF1C2', borderColor: '#F0D27A', color: '#6B4800' }}
    >
      <Clock aria-hidden="true" className="mt-0.5 size-5 shrink-0" />
      <div className="flex flex-col gap-1">
        <p className="font-semibold">
          {!pendiente ? (
            'El anticipo ya está cubierto'
          ) : falta !== null ? (
            <>
              Falta el anticipo: <Dinero valor={falta} />
            </>
          ) : (
            'Falta el anticipo'
          )}
        </p>
        {pendiente ? (
          <p>
            {modo === 'huesped' ? 'Transfiere' : 'El huésped debe transferir'}{' '}
            {textoPorcentajeAnticipo(reglas.porcentajeAnticipo)} de la estancia
            {anticipo !== null && (
              <>
                {' '}
                (<Dinero valor={anticipo} />
                {pagado > 0 && (
                  <>
                    , ya hay <Dinero valor={pagado} /> pagados
                  </>
                )}
                )
              </>
            )}{' '}
            {limite ? (
              <>
                {vencido ? 'y el plazo venció a las' : 'antes de las'} <strong>{limite}</strong>
              </>
            ) : (
              'dentro del plazo de confirmación'
            )}
            .{modo === 'huesped' && ' Luego envía el comprobante a recepción con el código de la reserva.'}
          </p>
        ) : (
          <p>{modo === 'huesped' ? 'Recepción confirmará tu reserva pronto.' : 'Ya puedes confirmar la reserva.'}</p>
        )}
      </div>
    </div>
  )
}
