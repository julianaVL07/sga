import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CircleX } from 'lucide-react'
import { useApartamento } from '@/api/publico'
import { useFolio, useReserva, type RetencionResponse } from '@/api/reservas'
import { AvisoError, Dinero, EstadoReserva } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useReglasAlojamiento } from '@/pages/publico/comun/consultas'
import { AvisoAnticipo } from './reserva/AvisoAnticipo'
import { DatosReserva } from './reserva/DatosReserva'
import { DialogoCancelar } from './reserva/DialogoCancelar'
import { FolioSoloLectura } from './reserva/FolioSoloLectura'

export default function DetalleMiReserva() {
  const { codigo } = useParams()
  const reserva = useReserva(codigo)
  const folio = useFolio(codigo)
  const apartamento = useApartamento(reserva.data?.identificacionApartamento)
  const { reglas } = useReglasAlojamiento()
  const [cancelando, setCancelando] = useState(false)
  const [retencion, setRetencion] = useState<RetencionResponse | null>(null)

  const volver = (
    <Link to="/mis-reservas" className="inline-flex w-fit items-center gap-1 text-sm text-muted-foreground hover:underline">
      <ArrowLeft aria-hidden="true" className="size-4" /> Mis reservas
    </Link>
  )

  if (reserva.error) {
    return (
      <section className="mx-auto flex w-full max-w-4xl flex-col gap-4">
        {volver}
        <AvisoError error={reserva.error} />
      </section>
    )
  }

  const r = reserva.data
  if (!r) {
    return (
      <section className="mx-auto flex w-full max-w-4xl flex-col gap-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-72" />
      </section>
    )
  }

  const cancelable = r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA'

  return (
    <section className="mx-auto flex w-full max-w-4xl flex-col gap-6">
      {volver}
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-titulo text-3xl font-semibold">{r.codigo}</h1>
          <EstadoReserva estado={r.estado} />
        </div>
        {cancelable && (
          <Button variant="outline" onClick={() => setCancelando(true)} className="text-error">
            <CircleX aria-hidden="true" /> Cancelar reserva
          </Button>
        )}
      </header>

      {retencion && (
        <div role="status" className="rounded-campo border border-border bg-card p-4 text-sm">
          <p className="font-semibold">Reserva cancelada</p>
          <p className="mt-1">
            Se retuvo <Dinero valor={retencion.retencion} className="font-semibold" /> según la política de cancelación.
            La retención quedó asentada en tu folio.
          </p>
        </div>
      )}

      <AvisoAnticipo reserva={r} folio={folio.data} reglas={reglas} modo="huesped" />

      <div className="rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
        <h2 className="mb-4 font-titulo text-xl font-semibold">Detalle de la reserva</h2>
        <DatosReserva reserva={r} nombreApartamento={apartamento.data?.nombre} umbralEdad={reglas.umbralEdad} />
      </div>

      <div className="rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
        <h2 className="mb-4 font-titulo text-xl font-semibold">Folio</h2>
        <FolioSoloLectura folio={folio.data} error={folio.error} />
      </div>

      <DialogoCancelar reserva={r} abierto={cancelando} alCambiar={setCancelando} alCancelar={setRetencion} />
    </section>
  )
}
