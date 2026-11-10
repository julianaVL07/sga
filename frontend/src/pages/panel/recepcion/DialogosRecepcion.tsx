import { toast } from 'sonner'
import { useRegistrarLlegada, useRegistrarSalida, type ReservaResumenResponse } from '@/api/ocupacion'
import { AvisoError, ConfirmarDialogo, formatearFecha } from '@/components/coral'

interface Props {
  /** Reserva elegida; null cierra el diálogo */
  reserva: ReservaResumenResponse | null
  alCerrar: () => void
}

/** Confirma la llegada: la reserva pasa a EN_CURSO y el apartamento a OCUPADO */
export function DialogoRegistrarLlegada({ reserva, alCerrar }: Props) {
  const llegada = useRegistrarLlegada()

  const cerrar = () => {
    llegada.reset()
    alCerrar()
  }

  return (
    <ConfirmarDialogo
      abierto={!!reserva}
      alCambiar={(abierto) => !abierto && !llegada.isPending && cerrar()}
      titulo="Registrar llegada"
      descripcion={
        reserva && (
          <>
            {reserva.nombreTitular} llega a <strong>{reserva.apartamento}</strong> con {reserva.totalOcupantes}{' '}
            {reserva.totalOcupantes === 1 ? 'ocupante' : 'ocupantes'} hasta el {formatearFecha(reserva.fechaSalida, "EEEE d 'de' MMMM")}.
            La reserva {reserva.codigo} quedará en curso y el apartamento, ocupado.
          </>
        )
      }
      textoConfirmar="Registrar llegada"
      cargando={llegada.isPending}
      alConfirmar={() =>
        reserva &&
        llegada.mutate(reserva.codigo, {
          onSuccess: () => {
            toast.success('Llegada registrada', { description: `${reserva.codigo} · ${reserva.apartamento}` })
            cerrar()
          },
        })
      }
    >
      {llegada.error && <AvisoError error={llegada.error} />}
    </ConfirmarDialogo>
  )
}

/** Confirma la salida: la reserva pasa a FINALIZADA y el apartamento queda pendiente de preparación */
export function DialogoRegistrarSalida({ reserva, alCerrar }: Props) {
  const salida = useRegistrarSalida()

  const cerrar = () => {
    salida.reset()
    alCerrar()
  }

  return (
    <ConfirmarDialogo
      abierto={!!reserva}
      alCambiar={(abierto) => !abierto && !salida.isPending && cerrar()}
      titulo="Registrar salida"
      descripcion={
        reserva && (
          <>
            {reserva.nombreTitular} deja <strong>{reserva.apartamento}</strong>. La reserva {reserva.codigo} quedará
            finalizada y el apartamento pasará a pendiente de preparación. El folio debe estar cerrado.
          </>
        )
      }
      textoConfirmar="Registrar salida"
      cargando={salida.isPending}
      alConfirmar={() =>
        reserva &&
        salida.mutate(reserva.codigo, {
          onSuccess: () => {
            toast.success('Salida registrada', { description: `${reserva.codigo} · ${reserva.apartamento}` })
            cerrar()
          },
        })
      }
    >
      {salida.error && <AvisoError error={salida.error} />}
    </ConfirmarDialogo>
  )
}
