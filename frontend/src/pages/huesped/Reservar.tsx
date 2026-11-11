import { AsistenteReserva } from './asistente/AsistenteReserva'

/** /reservar/:id — el huésped reserva por el portal y a su nombre */
export default function Reservar() {
  return <AsistenteReserva modo="huesped" />
}
