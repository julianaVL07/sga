import { useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Banknote, CircleCheckBig, Lock, PackagePlus, ShieldCheck, Undo2 } from 'lucide-react'
import { toast } from 'sonner'
import { useAlojamiento, useApartamento } from '@/api/publico'
import { useFolio, useReserva, type FolioResponse } from '@/api/reservas'
import { useCerrarFolio } from '@/api/folio'
import { useAuth } from '@/auth/AuthContext'
import {
  AvisoError,
  ConfirmarDialogo,
  Dinero,
  EstadoReserva,
  Fecha,
  TablaResponsive,
  Vacio,
  type ColumnaTabla,
} from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { DialogoAutorizarCierre } from './DialogoAutorizarCierre'
import { DialogoCorreccion, DialogoPago, DialogoServicio } from './DialogosFolio'
import { cargosCorregibles, movimientosDe, type ClaseMovimiento, type Movimiento } from './movimientos'

type Dialogo = 'pago' | 'servicio' | 'correccion' | 'cerrar' | 'autorizar' | null

const ESTILO_CLASE: Record<ClaseMovimiento, { fondo: string; texto: string; etiqueta: string }> = {
  cargo: { fondo: '#F7E7E1', texto: '#57434F', etiqueta: 'Cargo' },
  correccion: { fondo: '#FFF1C2', texto: '#6B4800', etiqueta: 'Corrección' },
  pago: { fondo: '#DDF2EA', texto: '#1C5E45', etiqueta: 'Pago' },
}

export default function Folio() {
  const { codigo = '' } = useParams()
  const folio = useFolio(codigo)
  const reserva = useReserva(codigo)
  const apartamento = useApartamento(reserva.data?.identificacionApartamento)
  const alojamiento = useAlojamiento()
  const { roles, usuario } = useAuth()
  const esAdministrador = roles.includes('ADMINISTRADOR')
  const cerrar = useCerrarFolio(codigo)

  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [indiceACorregir, setIndiceACorregir] = useState<number | null>(null)

  const alCambiar = (abierto: boolean) => {
    if (!abierto) {
      setDialogo(null)
      cerrar.reset()
    }
  }

  const volver = (
    <Link
      to={`/panel/reservas/${encodeURIComponent(codigo)}`}
      className="inline-flex w-fit items-center gap-1 text-sm text-muted-foreground hover:underline"
    >
      <ArrowLeft aria-hidden="true" className="size-4" /> Reserva {codigo}
    </Link>
  )

  if (folio.error) {
    return (
      <section className="mx-auto flex w-full max-w-5xl flex-col gap-4">
        {volver}
        <AvisoError titulo="No se pudo abrir el folio" error={folio.error} />
      </section>
    )
  }
  const f = folio.data
  if (!f) {
    return (
      <section className="mx-auto flex w-full max-w-5xl flex-col gap-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-28" />
        <Skeleton className="h-80" />
      </section>
    )
  }

  const r = reserva.data
  const movimientos = movimientosDe(f)
  const corregibles = cargosCorregibles(movimientos)
  const abierto = !f.cerrado

  const columnas: ColumnaTabla<Movimiento>[] = [
    { clave: 'numero', titulo: 'N.º', celda: (m) => <span className="font-mono text-xs">{m.numero}</span>, ocultarEnTarjeta: true },
    { clave: 'fecha', titulo: 'Fecha', celda: (m) => <Fecha valor={m.fecha} patron="d MMM yyyy" /> },
    { clave: 'tipo', titulo: 'Tipo', celda: (m) => <PildoraMovimiento movimiento={m} /> },
    {
      clave: 'concepto',
      titulo: 'Concepto',
      className: 'whitespace-normal',
      ocultarEnTarjeta: true,
      celda: (m) => <ConceptoMovimiento movimiento={m} />,
    },
    {
      clave: 'valor',
      titulo: 'Valor',
      className: 'text-right',
      celda: (m) => (
        <Dinero
          valor={m.valor}
          className={cn('font-semibold', m.valor < 0 && 'text-[#1C5E45] dark:text-[#8FD9B8]', m.corregidoPor && 'line-through opacity-60')}
        />
      ),
    },
  ]

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-6">
      {volver}

      {/* ---------- Encabezado ---------- */}
      <header className="flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-titulo text-3xl font-semibold">Folio {f.codigoReserva}</h1>
          {r && <EstadoReserva estado={r.estado} />}
          {f.cerrado && (
            <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-1 text-xs font-semibold">
              <Lock aria-hidden="true" className="size-3.5" /> Cerrado
            </span>
          )}
        </div>
        {r ? (
          <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-3">
            <Dato titulo="Titular">{r.titular.nombre}</Dato>
            <Dato titulo="Apartamento">
              {apartamento.data?.nombre ?? r.identificacionApartamento}
              <span className="text-muted-foreground"> · {r.identificacionApartamento}</span>
            </Dato>
            <Dato titulo="Estancia">
              <Fecha valor={r.fechaEntrada} /> → <Fecha valor={r.fechaSalida} />
              <span className="text-muted-foreground">
                {' '}
                · {r.noches} {r.noches === 1 ? 'noche' : 'noches'}
              </span>
            </Dato>
          </dl>
        ) : reserva.error ? (
          <AvisoError error={reserva.error} />
        ) : (
          <Skeleton className="h-12" />
        )}
      </header>

      {/* Lado a lado solo en pantallas muy anchas: con la barra lateral del panel, a 1280 px no caben las dos */}
      <div className="grid gap-6 2xl:grid-cols-[minmax(0,1fr)_20rem] 2xl:items-start">
        {/* ---------- Movimientos ---------- */}
        <div className="order-2 flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6 2xl:order-1">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <h2 className="font-titulo text-xl font-semibold">Movimientos</h2>
            <p className="text-xs text-muted-foreground">
              Abierto el <Fecha valor={f.fechaApertura} patron="d MMM yyyy" />
            </p>
          </div>
          {movimientos.length === 0 ? (
            <Vacio titulo="Sin movimientos" descripcion="Este folio todavía no tiene cargos ni pagos." />
          ) : (
            <TablaResponsive
              descripcion="Movimientos del folio"
              columnas={columnas}
              filas={movimientos}
              claveFila={(m) => m.clave}
              tituloTarjeta={(m) => (
                <div className="flex items-start justify-between gap-2">
                  <ConceptoMovimiento movimiento={m} />
                  <span className="font-mono text-xs text-muted-foreground">{m.numero}</span>
                </div>
              )}
              acciones={
                abierto
                  ? (m) =>
                      m.clase === 'cargo' && !m.corregidoPor ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setIndiceACorregir(m.indiceCargo ?? null)
                            setDialogo('correccion')
                          }}
                        >
                          <Undo2 aria-hidden="true" /> Corregir
                        </Button>
                      ) : null
                  : undefined
              }
            />
          )}
        </div>

        {/* ---------- Saldo y acciones ---------- */}
        <aside className="order-1 flex flex-col gap-4 2xl:order-2">
          <Saldo folio={f} />

          {abierto ? (
            <div className="grid grid-cols-2 gap-2 2xl:grid-cols-1">
              <Button onClick={() => setDialogo('pago')}>
                <Banknote aria-hidden="true" /> Registrar pago
              </Button>
              <Button variant="outline" onClick={() => setDialogo('servicio')}>
                <PackagePlus aria-hidden="true" /> Cargar servicio
              </Button>
              <Button
                variant="outline"
                disabled={corregibles.length === 0}
                onClick={() => {
                  setIndiceACorregir(null)
                  setDialogo('correccion')
                }}
              >
                <Undo2 aria-hidden="true" /> Corregir un cargo
              </Button>
              <Button variant="outline" onClick={() => setDialogo('cerrar')}>
                <CircleCheckBig aria-hidden="true" /> Cerrar folio
              </Button>
              {esAdministrador && (
                <Button variant="outline" className="col-span-2 2xl:col-span-1" onClick={() => setDialogo('autorizar')}>
                  <ShieldCheck aria-hidden="true" /> Autorizar cierre con saldo
                </Button>
              )}
              {f.saldo !== 0 && (
                <p className="col-span-2 text-xs text-muted-foreground 2xl:col-span-1">
                  El folio se cierra solo con saldo en cero.
                  {esAdministrador ? ' Con saldo, autoriza el cierre indicando el motivo.' : ''}
                </p>
              )}
            </div>
          ) : (
            <CierreFolio folio={f} />
          )}
        </aside>
      </div>

      {/* ---------- Diálogos ---------- */}
      <DialogoPago codigo={codigo} saldo={f.saldo} abierto={dialogo === 'pago'} alCambiar={alCambiar} />
      <DialogoServicio
        codigo={codigo}
        servicios={alojamiento.data?.serviciosAdicionales}
        errorServicios={alojamiento.error}
        abierto={dialogo === 'servicio'}
        alCambiar={alCambiar}
      />
      <DialogoCorreccion
        codigo={codigo}
        cargos={corregibles}
        indiceInicial={indiceACorregir}
        abierto={dialogo === 'correccion'}
        alCambiar={alCambiar}
      />
      {esAdministrador && (
        <DialogoAutorizarCierre
          codigo={codigo}
          saldo={f.saldo}
          autorizadoPor={usuario?.nombre ?? ''}
          abierto={dialogo === 'autorizar'}
          alCambiar={alCambiar}
        />
      )}
      <ConfirmarDialogo
        abierto={dialogo === 'cerrar'}
        alCambiar={alCambiar}
        titulo={`Cerrar el folio ${f.codigoReserva}`}
        descripcion={
          f.saldo === 0
            ? 'El saldo está en cero. Al cerrarlo ya no se podrán registrar cargos, pagos ni correcciones.'
            : 'El folio solo se cierra con saldo en cero. Si todavía hay saldo, el sistema no lo dejará cerrar.'
        }
        textoConfirmar="Cerrar folio"
        textoCancelar="Volver"
        cargando={cerrar.isPending}
        alConfirmar={() =>
          cerrar.mutate(undefined, {
            onSuccess: () => {
              setDialogo(null)
              toast.success('Folio cerrado', { description: f.codigoReserva })
            },
          })
        }
      >
        {cerrar.error && <AvisoError titulo="No se pudo cerrar el folio" error={cerrar.error} />}
      </ConfirmarDialogo>
    </section>
  )
}

function Dato({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs font-medium tracking-wide text-muted-foreground uppercase">{titulo}</dt>
      <dd className="font-medium">{children}</dd>
    </div>
  )
}

function PildoraMovimiento({ movimiento }: { movimiento: Movimiento }) {
  const estilo = ESTILO_CLASE[movimiento.clase]
  return (
    <span
      className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap"
      style={{ backgroundColor: estilo.fondo, color: estilo.texto }}
    >
      {movimiento.clase === 'cargo' ? movimiento.tipo : estilo.etiqueta}
    </span>
  )
}

function ConceptoMovimiento({ movimiento: m }: { movimiento: Movimiento }) {
  if (m.clase === 'correccion') {
    return (
      <span className="flex flex-col gap-0.5">
        <span className="inline-flex items-center gap-1">
          <Undo2 aria-hidden="true" className="size-3.5 shrink-0 text-muted-foreground" />
          {m.corrige ? `Reverso de ${m.corrige}: ${m.concepto}` : m.concepto}
        </span>
        {m.motivo && <span className="text-xs text-muted-foreground">Motivo: {m.motivo}</span>}
      </span>
    )
  }
  return (
    <span className="flex flex-col gap-0.5">
      <span>{m.concepto}</span>
      {m.corregidoPor && <span className="text-xs text-muted-foreground">Corregido por {m.corregidoPor}</span>}
    </span>
  )
}

function Saldo({ folio }: { folio: FolioResponse }) {
  const aFavor = folio.saldo < 0
  return (
    <div className="flex flex-col gap-3 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta">
      <p className="text-sm font-medium text-muted-foreground">{aFavor ? 'Saldo a favor del huésped' : 'Saldo'}</p>
      <Dinero valor={Math.abs(folio.saldo)} grande className="text-4xl md:text-5xl" />
      <dl className="flex flex-col gap-1.5 border-t border-border pt-3 text-sm">
        <div className="flex justify-between gap-3">
          <dt className="text-muted-foreground">Total de cargos</dt>
          <dd>
            <Dinero valor={folio.totalCargos} />
          </dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-muted-foreground">Total pagado</dt>
          <dd>
            <Dinero valor={folio.totalPagado} />
          </dd>
        </div>
        {folio.depositoGarantia > 0 && (
          <div className="flex justify-between gap-3">
            <dt className="text-muted-foreground">Depósito de garantía</dt>
            <dd>
              <Dinero valor={folio.depositoGarantia} />
            </dd>
          </div>
        )}
      </dl>
    </div>
  )
}

function CierreFolio({ folio }: { folio: FolioResponse }) {
  return (
    <div className="flex flex-col gap-2 rounded-tarjeta border border-border bg-muted/60 p-5 text-sm">
      <p className="inline-flex items-center gap-2 font-semibold">
        <Lock aria-hidden="true" className="size-4" />
        Folio cerrado{folio.fechaCierre && <> el <Fecha valor={folio.fechaCierre} patron="d MMM yyyy" /></>}
      </p>
      {folio.autorizadoPor ? (
        <>
          <p>
            Cierre con saldo autorizado por <strong>{folio.autorizadoPor}</strong>.
          </p>
          {folio.motivoAutorizacion && <p className="text-muted-foreground">Motivo: {folio.motivoAutorizacion}</p>}
        </>
      ) : (
        <p className="text-muted-foreground">Se cerró con saldo en cero. Ya no admite movimientos.</p>
      )}
    </div>
  )
}
