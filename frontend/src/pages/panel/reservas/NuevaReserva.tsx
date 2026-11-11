import { AsistenteReserva } from '@/pages/huesped/asistente/AsistenteReserva'

/** /panel/reservas/nueva — la recepción reserva por el canal directo; el administrador elige el canal */
export default function NuevaReserva() {
  return (
    <div className="mx-auto w-full max-w-6xl">
      <AsistenteReserva modo="panel" />
    </div>
  )
}
