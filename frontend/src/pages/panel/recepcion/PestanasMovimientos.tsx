import { useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { CalendarCheck2, DoorOpen, Hourglass, LogIn, LogOut, ReceiptText, type LucideIcon } from 'lucide-react'
import type { ReservaResumenResponse } from '@/api/ocupacion'
import type { Pagina } from '@/api/tipos'
import {
  AvisoError,
  Dinero,
  EstadoReserva,
  Fecha,
  Paginador,
  TablaResponsive,
  Vacio,
  formatearFecha,
  type ColumnaTabla,
} from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { DialogoRegistrarLlegada, DialogoRegistrarSalida } from './DialogosRecepcion'

export type ClavePestana = 'llegadas' | 'salidas' | 'en-casa' | 'por-confirmar'

export interface DatosPestana {
  consulta: { data?: Pagina<ReservaResumenResponse>; isPending: boolean; error: unknown }
  pagina: number
  alCambiarPagina: (pagina: number) => void
  /** Texto del estado vacío, con la próxima llegada o salida si se conoce */
  vacio: { titulo: string; descripcion?: ReactNode }
}

interface Props {
  fecha: string
  porcentajeAnticipo: number
  pestanas: Record<ClavePestana, DatosPestana>
}

const DEFINICION: { clave: ClavePestana; etiqueta: string; icono: LucideIcon }[] = [
  { clave: 'llegadas', etiqueta: 'Llegadas', icono: LogIn },
  { clave: 'salidas', etiqueta: 'Salidas', icono: LogOut },
  { clave: 'en-casa', etiqueta: 'En casa', icono: DoorOpen },
  { clave: 'por-confirmar', etiqueta: 'Por confirmar', icono: Hourglass },
]

const enlaceReserva = (codigo: string) => `/panel/reservas/${encodeURIComponent(codigo)}`

/** Columnas comunes a todas las pestañas */
const colApartamento: ColumnaTabla<ReservaResumenResponse> = {
  clave: 'apartamento',
  titulo: 'Apto.',
  ocultarEnTarjeta: true,
  celda: (r) => <span className="font-semibold">{r.apartamento}</span>,
}
const colTitular: ColumnaTabla<ReservaResumenResponse> = {
  clave: 'titular',
  titulo: 'Titular',
  celda: (r) => (
    <span className="flex flex-col md:items-start">
      <span className="font-medium">{r.nombreTitular}</span>
      <Link to={enlaceReserva(r.codigo)} className="text-xs text-muted-foreground hover:underline">
        {r.codigo}
      </Link>
    </span>
  ),
}
const colEstancia: ColumnaTabla<ReservaResumenResponse> = {
  clave: 'estancia',
  titulo: 'Estancia',
  celda: (r) => (
    <span className="whitespace-nowrap">
      <Fecha valor={r.fechaEntrada} /> → <Fecha valor={r.fechaSalida} />
    </span>
  ),
}
const colOcupantes: ColumnaTabla<ReservaResumenResponse> = {
  clave: 'ocupantes',
  titulo: 'Ocup.',
  className: 'text-center',
  celda: (r) => <span className="tabular-nums">{r.totalOcupantes}</span>,
}
const colEstado: ColumnaTabla<ReservaResumenResponse> = {
  clave: 'estado',
  titulo: 'Estado',
  celda: (r) => <EstadoReserva estado={r.estado} />,
}

/** Llegadas, salidas, en casa y por confirmar en una tabla con pestañas */
export function PestanasMovimientos({ fecha, porcentajeAnticipo, pestanas }: Props) {
  const [activa, setActiva] = useState<ClavePestana>('llegadas')
  const [llegada, setLlegada] = useState<ReservaResumenResponse | null>(null)
  const [salida, setSalida] = useState<ReservaResumenResponse | null>(null)
  const botones = useRef<Record<string, HTMLButtonElement | null>>({})

  // Flechas izquierda y derecha para moverse entre pestañas
  const teclado = (evento: KeyboardEvent, indice: number) => {
    const delta = evento.key === 'ArrowRight' ? 1 : evento.key === 'ArrowLeft' ? -1 : 0
    if (!delta) return
    evento.preventDefault()
    const siguiente = DEFINICION[(indice + delta + DEFINICION.length) % DEFINICION.length].clave
    setActiva(siguiente)
    botones.current[siguiente]?.focus()
  }

  const columnas: Record<ClavePestana, ColumnaTabla<ReservaResumenResponse>[]> = {
    llegadas: [colApartamento, colTitular, colEstancia, colOcupantes, colEstado],
    salidas: [colApartamento, colTitular, colEstancia, colOcupantes, colEstado],
    'en-casa': [
      colApartamento,
      colTitular,
      { ...colEstancia, titulo: 'Sale', celda: (r) => <Fecha valor={r.fechaSalida} /> },
      colOcupantes,
    ],
    'por-confirmar': [
      colApartamento,
      colTitular,
      colEstancia,
      {
        clave: 'valor',
        titulo: 'Estancia',
        className: 'text-right',
        celda: (r) => <Dinero valor={r.valorTotal} />,
      },
      {
        clave: 'anticipo',
        titulo: `Anticipo ${porcentajeAnticipo} %`,
        className: 'text-right',
        celda: (r) => <Dinero valor={(r.valorTotal * porcentajeAnticipo) / 100} className="font-semibold" />,
      },
    ],
  }

  const acciones: Record<ClavePestana, (r: ReservaResumenResponse) => ReactNode> = {
    llegadas: (r) => (
      <Button size="sm" onClick={() => setLlegada(r)}>
        <CalendarCheck2 aria-hidden="true" /> Registrar llegada
      </Button>
    ),
    salidas: (r) => (
      <div className="flex flex-wrap justify-end gap-2">
        <Button asChild size="sm" variant="outline">
          <Link to={`${enlaceReserva(r.codigo)}/folio`}>
            <ReceiptText aria-hidden="true" /> Ver folio
          </Link>
        </Button>
        <Button size="sm" onClick={() => setSalida(r)}>
          <LogOut aria-hidden="true" /> Registrar salida
        </Button>
      </div>
    ),
    'en-casa': (r) => (
      <Button asChild size="sm" variant="ghost">
        <Link to={enlaceReserva(r.codigo)}>Ver reserva</Link>
      </Button>
    ),
    'por-confirmar': (r) => (
      <Button asChild size="sm" variant="outline">
        <Link to={enlaceReserva(r.codigo)}>Ver detalle</Link>
      </Button>
    ),
  }

  const datos = pestanas[activa]
  const pagina = datos.consulta.data

  return (
    <section aria-labelledby="titulo-movimientos" className="rounded-tarjeta border border-border bg-card shadow-tarjeta">
      <h2 id="titulo-movimientos" className="sr-only">
        Movimientos del {formatearFecha(fecha, "d 'de' MMMM")}
      </h2>
      <div role="tablist" aria-label="Movimientos" className="flex gap-1 overflow-x-auto border-b border-border px-2 pt-2">
        {DEFINICION.map((p, i) => {
          const Icono = p.icono
          const total = pestanas[p.clave].consulta.data?.totalElementos
          const seleccionada = activa === p.clave
          return (
            <button
              key={p.clave}
              ref={(el) => {
                botones.current[p.clave] = el
              }}
              id={`pestana-${p.clave}`}
              role="tab"
              type="button"
              aria-selected={seleccionada}
              aria-controls={`panel-${p.clave}`}
              tabIndex={seleccionada ? 0 : -1}
              onClick={() => setActiva(p.clave)}
              onKeyDown={(e) => teclado(e, i)}
              className={cn(
                'relative flex min-h-11 shrink-0 items-center gap-2 rounded-t-campo px-3 text-sm font-medium transition-colors duration-150 outline-none focus-visible:ring-3 focus-visible:ring-ring/50',
                'after:absolute after:inset-x-2 after:-bottom-px after:h-0.5 after:rounded-full',
                seleccionada
                  ? 'text-foreground after:bg-coral'
                  : 'text-muted-foreground after:bg-transparent hover:text-foreground',
              )}
            >
              <Icono aria-hidden="true" className="size-4" />
              {p.etiqueta}
              {total !== undefined && (
                <span
                  className={cn(
                    'rounded-full px-1.5 text-xs tabular-nums',
                    seleccionada ? 'bg-coral/15 text-coral-texto' : 'bg-muted',
                  )}
                >
                  {total}
                </span>
              )}
            </button>
          )
        })}
      </div>

      <div
        id={`panel-${activa}`}
        role="tabpanel"
        aria-labelledby={`pestana-${activa}`}
        tabIndex={0}
        className="p-3 outline-none md:p-4"
      >
        {datos.consulta.error ? (
          <AvisoError error={datos.consulta.error} />
        ) : !pagina ? (
          <div className="flex flex-col gap-2">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : pagina.contenido.length === 0 ? (
          <Vacio titulo={datos.vacio.titulo} descripcion={datos.vacio.descripcion} className="py-8" />
        ) : (
          <>
            <TablaResponsive
              columnas={columnas[activa]}
              filas={pagina.contenido}
              claveFila={(r) => r.codigo}
              tituloTarjeta={(r) => (
                <span className="flex items-center justify-between gap-2">
                  <span>
                    {r.apartamento} · {r.nombreTitular}
                  </span>
                  <EstadoReserva estado={r.estado} />
                </span>
              )}
              acciones={acciones[activa]}
              descripcion={DEFINICION.find((p) => p.clave === activa)?.etiqueta}
            />
            <Paginador
              className="mt-3"
              pagina={datos.pagina}
              totalPaginas={pagina.totalPaginas}
              totalElementos={pagina.totalElementos}
              alCambiar={datos.alCambiarPagina}
            />
          </>
        )}
      </div>

      <DialogoRegistrarLlegada reserva={llegada} alCerrar={() => setLlegada(null)} />
      <DialogoRegistrarSalida reserva={salida} alCerrar={() => setSalida(null)} />
    </section>
  )
}
