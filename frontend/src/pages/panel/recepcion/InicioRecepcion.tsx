import { useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useDetallesApartamentos, useTodasLasTemporadas, useTodosLosApartamentos } from '@/api/apartamentos'
import {
  useCalendarios,
  useLlegadas,
  useReservas,
  useSalidas,
  useTodasLasReservas,
  type ReservaResumenResponse,
} from '@/api/ocupacion'
import { useAlojamiento } from '@/api/publico'
import type { TipoTemporada } from '@/api/tipos'
import { AvisoError, formatearFecha } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { CalendarioOcupacion } from '../calendario/CalendarioOcupacion'
import { hoyISO, sumarDias, temporadaDeFecha } from '../calendario/fechas'
import { useTitulares } from '../calendario/useTitulares'
import { PestanasMovimientos, type ClavePestana, type DatosPestana } from './PestanasMovimientos'
import { TablaApartamentosRecepcion } from './TablaApartamentosRecepcion'

const NOCHES_CALENDARIO = 21

const COLOR_TEMPORADA: Record<TipoTemporada, string> = {
  BASE: 'bg-muted text-muted-foreground',
  MEDIA: 'bg-durazno/30 text-foreground',
  ALTA: 'bg-coral/20 text-coral-texto',
}

/** La reserva con la fecha más temprana según el campo dado */
function primeraPor(reservas: ReservaResumenResponse[], campo: 'fechaEntrada' | 'fechaSalida') {
  return [...reservas].sort((a, b) => a[campo].localeCompare(b[campo]))[0]
}

function textoReserva(prefijo: string, r: ReservaResumenResponse | undefined, campo: 'fechaEntrada' | 'fechaSalida') {
  if (!r) return undefined
  return `${prefijo} es el ${formatearFecha(r[campo], "EEEE d 'de' MMMM")}: ${r.nombreTitular} en ${r.apartamento}.`
}

export default function InicioRecepcion() {
  const hoy = hoyISO()
  const [fecha, setFecha] = useState(hoy)
  const [paginas, setPaginas] = useState<Record<ClavePestana, number>>({
    llegadas: 0,
    salidas: 0,
    'en-casa': 0,
    'por-confirmar': 0,
  })
  const irAPagina = (clave: ClavePestana) => (pagina: number) => setPaginas((p) => ({ ...p, [clave]: pagina }))

  const cambiarFecha = (nueva: string) => {
    setFecha(nueva)
    setPaginas((p) => ({ ...p, llegadas: 0, salidas: 0 }))
  }

  // ---------- Consultas ----------
  const temporadas = useTodasLasTemporadas()
  const alojamiento = useAlojamiento()
  const apartamentos = useTodosLosApartamentos()
  const ids = (apartamentos.data ?? []).map((a) => a.identificacion)
  const detalles = useDetallesApartamentos(ids)
  const calendarios = useCalendarios(ids, fecha, sumarDias(fecha, NOCHES_CALENDARIO))

  const llegadas = useLlegadas(fecha, paginas.llegadas)
  const salidas = useSalidas(fecha, paginas.salidas)
  const enCasa = useReservas({ estado: 'EN_CURSO' }, paginas['en-casa'])
  const porConfirmar = useReservas({ estado: 'PENDIENTE' }, paginas['por-confirmar'])
  const enCursoTodas = useTodasLasReservas({ estado: 'EN_CURSO' })
  const titulares = useTitulares()

  // Próxima llegada confirmada después de la fecha elegida, para los estados vacíos
  const sinLlegadas = llegadas.data?.totalElementos === 0
  const sinEnCasa = enCasa.data?.totalElementos === 0
  const proximas = useTodasLasReservas({ estado: 'CONFIRMADA', desde: sumarDias(fecha, 1) }, sinLlegadas || sinEnCasa)
  const proximaLlegada = primeraPor(proximas.data ?? [], 'fechaEntrada')
  const proximaSalida = primeraPor(
    (enCursoTodas.data ?? []).filter((r) => r.fechaSalida > fecha),
    'fechaSalida',
  )

  // ---------- Datos derivados ----------
  const porId = Object.fromEntries(ids.map((id, i) => [id, detalles[i]?.data]))
  const calendariosPorId = Object.fromEntries(ids.map((id, i) => [id, calendarios[i]?.data]))
  const aLaVenta = ids.filter((id) => porId[id]?.activo).length
  const fueraDeServicio = (apartamentos.data ?? []).filter((a) => a.estadoOperativo === 'FUERA_DE_SERVICIO').length
  const temporada = temporadaDeFecha(fecha, temporadas.data ?? [])
  const porcentajeAnticipo = alojamiento.data?.parametros.porcentajeAnticipo ?? 50

  const pestanas: Record<ClavePestana, DatosPestana> = {
    llegadas: {
      consulta: llegadas,
      pagina: paginas.llegadas,
      alCambiarPagina: irAPagina('llegadas'),
      vacio: {
        titulo: 'No hay llegadas este día',
        descripcion: textoReserva('La próxima llegada', proximaLlegada, 'fechaEntrada') ?? 'No hay llegadas confirmadas próximas.',
      },
    },
    salidas: {
      consulta: salidas,
      pagina: paginas.salidas,
      alCambiarPagina: irAPagina('salidas'),
      vacio: {
        titulo: 'No hay salidas este día',
        descripcion: textoReserva('La próxima salida', proximaSalida, 'fechaSalida') ?? 'No hay huéspedes en casa.',
      },
    },
    'en-casa': {
      consulta: enCasa,
      pagina: paginas['en-casa'],
      alCambiarPagina: irAPagina('en-casa'),
      vacio: {
        titulo: 'No hay huéspedes en casa',
        descripcion: textoReserva('La próxima llegada', proximaLlegada, 'fechaEntrada'),
      },
    },
    'por-confirmar': {
      consulta: porConfirmar,
      pagina: paginas['por-confirmar'],
      alCambiarPagina: irAPagina('por-confirmar'),
      vacio: { titulo: 'No hay reservas por confirmar', descripcion: 'Todas las reservas tienen su anticipo.' },
    },
  }

  return (
    <section className="mx-auto flex w-full max-w-7xl flex-col gap-5">
      <h1 className="sr-only">Recepción</h1>

      {/* a) Barra del día */}
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="outline" size="icon" aria-label="Día anterior" onClick={() => cambiarFecha(sumarDias(fecha, -1))}>
          <ChevronLeft aria-hidden="true" />
        </Button>
        <div className="flex min-w-0 flex-1 flex-wrap items-center justify-center gap-x-3 gap-y-1 sm:flex-none" aria-live="polite">
          <p className="font-titulo text-xl font-semibold first-letter:uppercase">
            {formatearFecha(fecha, "EEEE d 'de' MMMM")}
          </p>
          {temporada && (
            <span className={cn('rounded-full px-2.5 py-0.5 text-xs font-semibold', COLOR_TEMPORADA[temporada.tipo])}>
              Temporada {temporada.nombre.toLowerCase()}
            </span>
          )}
        </div>
        <Button variant="outline" size="icon" aria-label="Día siguiente" onClick={() => cambiarFecha(sumarDias(fecha, 1))}>
          <ChevronRight aria-hidden="true" />
        </Button>
        <Button variant="ghost" disabled={fecha === hoy} onClick={() => cambiarFecha(hoy)}>
          Hoy
        </Button>
      </div>

      {/* b) Franja de indicadores */}
      <dl className="grid grid-cols-5 divide-x divide-border rounded-tarjeta border border-border bg-card py-3 shadow-tarjeta">
        <Indicador etiqueta="En casa" valor={enCursoTodas.data?.length} detalle={aLaVenta ? `/${aLaVenta}` : undefined}
          titulo={aLaVenta ? `Huéspedes en casa, de ${aLaVenta} apartamentos a la venta` : undefined} />
        <Indicador etiqueta="Llegadas" valor={llegadas.data?.totalElementos} />
        <Indicador etiqueta="Salidas" valor={salidas.data?.totalElementos} />
        <Indicador etiqueta="Por confirmar" valor={porConfirmar.data?.totalElementos} />
        <Indicador etiqueta="Fuera de servicio" valor={apartamentos.data ? fueraDeServicio : undefined} />
      </dl>

      {/* c) Movimientos */}
      <PestanasMovimientos fecha={fecha} porcentajeAnticipo={porcentajeAnticipo} pestanas={pestanas} />

      {/* d) Apartamentos */}
      <TablaApartamentosRecepcion
        fecha={fecha}
        apartamentos={apartamentos.data}
        detalles={porId}
        calendarios={calendariosPorId}
        enCurso={enCursoTodas.data ?? []}
        titulares={titulares}
        error={apartamentos.error ?? undefined}
      />

      {/* e) Calendario de 21 noches */}
      <section aria-labelledby="titulo-calendario" className="rounded-tarjeta border border-border bg-card p-3 shadow-tarjeta md:p-4">
        <div className="mb-3 flex items-center justify-between gap-3 px-1">
          <h2 id="titulo-calendario" className="font-titulo text-xl font-semibold">
            Próximas 3 semanas
          </h2>
          <Link to="/panel/calendario" className="text-sm font-medium text-coral-texto hover:underline">
            Abrir calendario
          </Link>
        </div>
        {calendarios.find((c) => c.error) && <AvisoError className="mb-3" error={calendarios.find((c) => c.error)?.error} />}
        <CalendarioOcupacion
          desde={fecha}
          cantidadNoches={NOCHES_CALENDARIO}
          filas={apartamentos.data ?? []}
          calendarios={calendariosPorId}
          temporadas={temporadas.data ?? []}
          titulares={titulares}
          cargando={apartamentos.isPending || calendarios.some((c) => c.isPending)}
        />
      </section>
    </section>
  )
}

function Indicador({
  etiqueta,
  valor,
  detalle,
  titulo,
}: {
  etiqueta: string
  valor?: number
  detalle?: ReactNode
  titulo?: string
}) {
  return (
    <div className="flex min-w-0 flex-col-reverse items-center gap-0.5 px-1 text-center" title={titulo}>
      <dt className="text-[0.7rem] leading-tight text-muted-foreground sm:text-xs">{etiqueta}</dt>
      <dd className="flex items-baseline gap-1">
        <span className="font-titulo text-2xl font-semibold tabular-nums sm:text-3xl">{valor ?? '–'}</span>
        {detalle && <span className="text-xs text-muted-foreground sm:text-sm">{detalle}</span>}
      </dd>
    </div>
  )
}
