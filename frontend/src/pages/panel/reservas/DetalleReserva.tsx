import { useEffect, useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CircleCheck, CircleX, MoonStar, PencilLine, Receipt } from 'lucide-react'
import { usePoliticaVigente, useApartamento } from '@/api/publico'
import {
  useConfirmarReserva,
  useDeclararNoShow,
  useFolio,
  useReserva,
  type ModificacionReservaResponse,
  type RetencionResponse,
} from '@/api/reservas'
import { AvisoError, ConfirmarDialogo, Dinero, EstadoReserva } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { AvisoAnticipo } from '@/pages/huesped/reserva/AvisoAnticipo'
import { DatosReserva } from '@/pages/huesped/reserva/DatosReserva'
import { DialogoCancelar } from '@/pages/huesped/reserva/DialogoCancelar'
import { FolioSoloLectura } from '@/pages/huesped/reserva/FolioSoloLectura'
import { useReglasAlojamiento } from '@/pages/publico/comun/consultas'
import { formatearHora, horaDesde, tramosOrdenados } from '@/pages/publico/comun/reglas'
import { TextoCargo } from '@/pages/publico/comun/TextoCargo'
import { DialogoModificar } from './DialogoModificar'

type Dialogo = 'modificar' | 'confirmar' | 'cancelar' | 'noshow' | null

type Resultado =
  | { tipo: 'modificada'; datos: ModificacionReservaResponse }
  | { tipo: 'confirmada' }
  | { tipo: 'cancelada' | 'noshow'; datos: RetencionResponse }

export default function DetalleReserva() {
  const { codigo = '' } = useParams()
  const reserva = useReserva(codigo)
  const folio = useFolio(codigo)
  const apartamento = useApartamento(reserva.data?.identificacionApartamento)
  const politica = usePoliticaVigente()
  const { reglas } = useReglasAlojamiento()
  const confirmar = useConfirmarReserva(codigo)
  const noShow = useDeclararNoShow(codigo)

  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [hora, setHora] = useState('')
  const [resultado, setResultado] = useState<Resultado | null>(null)

  // Reloj por minuto: el botón de no-show se activa solo al llegar la hora límite
  const [ahora, setAhora] = useState(() => Date.now())
  useEffect(() => {
    const reloj = window.setInterval(() => setAhora(Date.now()), 60_000)
    return () => window.clearInterval(reloj)
  }, [])

  const volver = (
    <Link to="/panel/reservas" className="inline-flex w-fit items-center gap-1 text-sm text-muted-foreground hover:underline">
      <ArrowLeft aria-hidden="true" className="size-4" /> Reservas
    </Link>
  )

  if (reserva.error) {
    return (
      <section className="mx-auto flex w-full max-w-5xl flex-col gap-4">
        {volver}
        <AvisoError error={reserva.error} />
      </section>
    )
  }
  const r = reserva.data
  if (!r) {
    return (
      <section className="mx-auto flex w-full max-w-5xl flex-col gap-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-80" />
      </section>
    )
  }

  const activa = r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA'
  // L-15: el no-show se activa desde la hora límite (22:00) del día de llegada; si aun así no procede, el 409 lo explica
  // Sin la hora límite configurada se activa desde el inicio del día de llegada y el backend decide
  const inicioNoShow = new Date(`${r.fechaEntrada}T${reglas.horaLimiteNoShow ?? '00:00'}:00`).getTime()
  const noShowDisponible = ahora >= inicioNoShow
  const tramoCero = tramosOrdenados(politica.data).find((t) => t.antelacionMinimaDias === 0)
  const retencionNoShow = tramoCero ? Math.round((r.valorTotal * tramoCero.porcentajeRetencion) / 100) : null

  const cerrar = (abierto: boolean) => {
    if (!abierto) {
      setDialogo(null)
      confirmar.reset()
      noShow.reset()
    }
  }

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-6">
      {volver}
      <header className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-titulo text-3xl font-semibold">{r.codigo}</h1>
          <EstadoReserva estado={r.estado} />
        </div>
        {/* En el celular, cuadrícula de dos columnas; desde sm, una fila que se acomoda */}
        <div className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
          {activa && (
            <Button variant="outline" onClick={() => setDialogo('modificar')}>
              <PencilLine aria-hidden="true" /> Modificar
            </Button>
          )}
          {r.estado === 'PENDIENTE' && (
            <Button
              onClick={() => {
                setHora(r.horaEstimadaLlegada?.slice(0, 5) ?? '')
                setDialogo('confirmar')
              }}
            >
              <CircleCheck aria-hidden="true" /> Confirmar
            </Button>
          )}
          {activa && (
            <Button variant="outline" className="text-error" onClick={() => setDialogo('cancelar')}>
              <CircleX aria-hidden="true" /> Cancelar
            </Button>
          )}
          <Button variant="ghost" asChild>
            <Link to={`/panel/reservas/${encodeURIComponent(r.codigo)}/folio`}>
              <Receipt aria-hidden="true" /> Folio
            </Link>
          </Button>
          {/* Va al final para que su ayuda quede justo debajo */}
          {activa && (
            <Button
              variant="outline"
              className="col-span-2 sm:col-span-1"
              disabled={!noShowDisponible}
              aria-describedby={noShowDisponible ? undefined : 'ayuda-no-show'}
              onClick={() => setDialogo('noshow')}
            >
              <MoonStar aria-hidden="true" /> Declarar no-show
            </Button>
          )}
        </div>
        {activa && !noShowDisponible && (
          <p id="ayuda-no-show" className="-mt-2 text-xs text-muted-foreground">
            {reglas.horaLimiteNoShow
              ? `Disponible desde las ${reglas.horaLimiteNoShow} del día de llegada`
              : 'Disponible desde la hora límite del día de llegada'}
          </p>
        )}
      </header>

      {resultado && <AvisoResultado resultado={resultado} />}

      <AvisoAnticipo reserva={r} folio={folio.data} reglas={reglas} modo="panel" />

      <div className="grid gap-6 lg:grid-cols-[1fr_22rem] lg:items-start">
        <div className="rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
          <h2 className="mb-4 font-titulo text-xl font-semibold">Detalle</h2>
          <DatosReserva reserva={r} nombreApartamento={apartamento.data?.nombre} umbralEdad={reglas.umbralEdad} />
        </div>
        <div className="rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta">
          <div className="mb-4 flex items-center justify-between gap-2">
            <h2 className="font-titulo text-xl font-semibold">Folio</h2>
            <Link
              to={`/panel/reservas/${encodeURIComponent(r.codigo)}/folio`}
              className="text-sm font-medium text-primary hover:underline"
            >
              Abrir
            </Link>
          </div>
          <FolioSoloLectura folio={folio.data} error={folio.error} />
        </div>
      </div>

      {/* ---------- Diálogos ---------- */}
      <DialogoModificar
        reserva={r}
        abierto={dialogo === 'modificar'}
        alCambiar={cerrar}
        alModificar={(datos) => setResultado({ tipo: 'modificada', datos })}
      />

      <ConfirmarDialogo
        abierto={dialogo === 'confirmar'}
        alCambiar={cerrar}
        titulo={`Confirmar ${r.codigo}`}
        descripcion="Exige la hora estimada de llegada, el plazo vigente y el anticipo cubierto en el folio."
        textoConfirmar="Confirmar reserva"
        textoCancelar="Volver"
        cargando={confirmar.isPending}
        alConfirmar={() =>
          confirmar.mutate(hora || undefined, {
            onSuccess: () => {
              setDialogo(null)
              setResultado({ tipo: 'confirmada' })
            },
          })
        }
      >
        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="confirmar-hora">Hora estimada de llegada (opcional si ya la tiene)</Label>
            <Input id="confirmar-hora" type="time" value={hora} onChange={(e) => setHora(e.target.value)} />
            {reglas.horaNocturna && horaDesde(hora, reglas.horaNocturna) && (
              <p className="text-xs text-muted-foreground">
                Con llegada desde las {formatearHora(reglas.horaNocturna)},{' '}
                <TextoCargo tipo="nocturna" valor={reglas.recargoNocturno} minuscula />.
              </p>
            )}
          </div>
          {confirmar.error && <AvisoError titulo="No se pudo confirmar" error={confirmar.error} />}
        </div>
      </ConfirmarDialogo>

      <DialogoCancelar
        reserva={r}
        abierto={dialogo === 'cancelar'}
        alCambiar={cerrar}
        alCancelar={(datos) => setResultado({ tipo: 'cancelada', datos })}
      />

      <ConfirmarDialogo
        abierto={dialogo === 'noshow'}
        alCambiar={cerrar}
        titulo={`Declarar no-show de ${r.codigo}`}
        descripcion={`Solo es posible ${reglas.horaLimiteNoShow ? `desde las ${formatearHora(reglas.horaLimiteNoShow)}` : 'desde la hora límite'} del día de llegada. Las noches quedan libres.`}
        textoConfirmar="Declarar no-show"
        textoCancelar="Volver"
        peligroso
        cargando={noShow.isPending}
        alConfirmar={() =>
          noShow.mutate(undefined, {
            onSuccess: (datos) => {
              setDialogo(null)
              setResultado({ tipo: 'noshow', datos })
            },
          })
        }
      >
        <div className="flex flex-col gap-3">
          {retencionNoShow !== null && (
            <p className="flex items-baseline justify-between gap-3 rounded-campo bg-muted/60 p-4 text-sm font-semibold">
              <span>Retención estimada ({tramoCero?.porcentajeRetencion} %)</span>
              <Dinero valor={retencionNoShow} grande className="text-2xl" />
            </p>
          )}
          {noShow.error && <AvisoError titulo="No se pudo declarar el no-show" error={noShow.error} />}
        </div>
      </ConfirmarDialogo>
    </section>
  )
}

function AvisoResultado({ resultado }: { resultado: Resultado }) {
  let texto: ReactNode
  if (resultado.tipo === 'modificada') {
    const d = resultado.datos.diferencia
    texto =
      d === 0 ? (
        'Reserva modificada. El valor no cambió.'
      ) : (
        <>
          Reserva modificada. Se asentó en el folio una diferencia de{' '}
          <strong>
            {d > 0 ? '+' : ''}
            <Dinero valor={d} />
          </strong>
          ; nuevo valor <Dinero valor={resultado.datos.reserva.valorTotal} className="font-semibold" />.
        </>
      )
  } else if (resultado.tipo === 'confirmada') {
    texto = 'Reserva confirmada.'
  } else {
    texto = (
      <>
        {resultado.tipo === 'cancelada' ? 'Reserva cancelada.' : 'No-show declarado.'} Retención aplicada:{' '}
        <strong>
          <Dinero valor={resultado.datos.retencion} />
        </strong>
        .
      </>
    )
  }
  return (
    <div role="status" className="rounded-campo border px-4 py-3 text-sm" style={{ backgroundColor: '#DDF2EA', borderColor: '#A9DCC7', color: '#1C5E45' }}>
      {texto}
    </div>
  )
}
