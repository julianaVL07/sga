import type { ReactNode } from 'react'
import type { ReservaDetalleResponse } from '@/api/reservas'
import { Dinero, Fecha } from '@/components/coral'
import { EtiquetaPago } from '@/pages/publico/comun/EtiquetaPago'
import { formatearHora } from '@/pages/publico/comun/reglas'
import { formatearInstante, NOMBRE_CANAL } from './formato'

interface Props {
  reserva: ReservaDetalleResponse
  nombreApartamento?: string
  umbralEdad: number | null
}

/** Estancia, titular, ocupantes y detalles de una reserva */
export function DatosReserva({ reserva, nombreApartamento, umbralEdad }: Props) {
  return (
    <div className="flex flex-col gap-6">
      <dl className="grid gap-4 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <Dato
          etiqueta="Apartamento"
          valor={nombreApartamento ? `${reserva.identificacionApartamento} · ${nombreApartamento}` : reserva.identificacionApartamento}
        />
        <Dato
          etiqueta="Estancia"
          valor={
            <>
              <Fecha valor={reserva.fechaEntrada} /> → <Fecha valor={reserva.fechaSalida} /> · {reserva.noches}{' '}
              {reserva.noches === 1 ? 'noche' : 'noches'}
            </>
          }
        />
        <Dato etiqueta="Valor de la estancia" valor={<Dinero valor={reserva.valorTotal} className="font-semibold" />} />
        <Dato
          etiqueta="Hora estimada de llegada"
          valor={reserva.horaEstimadaLlegada ? formatearHora(reserva.horaEstimadaLlegada) : 'Sin indicar'}
        />
        <Dato
          etiqueta="Mascotas"
          valor={
            reserva.cantidadMascotas === 0
              ? 'Ninguna'
              : `${reserva.cantidadMascotas} · la más pesada de ${reserva.pesoMascotaMasPesadaKg} kg`
          }
        />
        <Dato etiqueta="Canal" valor={NOMBRE_CANAL[reserva.canalOrigen]} />
        <Dato etiqueta="Creada" valor={formatearInstante(reserva.fechaCreacion)} />
        {reserva.fechaConfirmacion && <Dato etiqueta="Confirmada" valor={formatearInstante(reserva.fechaConfirmacion)} />}
        <Dato etiqueta="Política de cancelación" valor={`Versión ${reserva.versionPoliticaCancelacion}`} />
        {reserva.motivoCancelacion && <Dato etiqueta="Motivo de cancelación" valor={reserva.motivoCancelacion} />}
      </dl>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-campo border border-border p-4">
          <h3 className="text-sm font-semibold">Titular</h3>
          <dl className="mt-2 grid gap-2 text-sm">
            <Dato etiqueta="Nombre" valor={reserva.titular.nombre} />
            <Dato etiqueta="Documento" valor={`${reserva.titular.tipoDocumento} ${reserva.titular.numeroDocumento}`} />
            <Dato etiqueta="Contacto" valor={`${reserva.titular.email} · ${reserva.titular.telefono}`} />
          </dl>
        </div>
        <div className="rounded-campo border border-border p-4">
          <h3 className="text-sm font-semibold">
            Ocupantes <span className="font-normal text-muted-foreground">· {reserva.totalOcupantes}</span>
          </h3>
          <ul className="mt-2 flex flex-col gap-2 text-sm">
            {reserva.ocupantes.map((o, i) => (
              <li key={`${o.nombre}-${i}`} className="flex flex-wrap items-center justify-between gap-2">
                <span>
                  {o.nombre}
                  <span className="block text-xs text-muted-foreground">
                    <Fecha valor={o.fechaNacimiento} patron="d MMM yyyy" />
                  </span>
                </span>
                <EtiquetaPago fecha={o.fechaNacimiento} umbral={umbralEdad} referencia={reserva.fechaEntrada} />
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd className="font-medium break-words">{valor}</dd>
    </div>
  )
}
