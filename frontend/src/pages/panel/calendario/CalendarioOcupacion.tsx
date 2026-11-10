import type { CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import type { CalendarioOcupacionResponse } from '@/api/ocupacion'
import type { EstadoReserva, TemporadaResponse, TipoTemporada } from '@/api/tipos'
import { formatearFecha } from '@/components/coral'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { esNocheDeFinDeSemana, hoyISO, noches, temporadaDeFecha, tramosDelCalendario } from './fechas'

/** Colores de las barras: los mismos de la píldora EstadoReserva */
const COLOR_RESERVA: Partial<Record<EstadoReserva, { fondo: string; borde: string; texto: string; etiqueta: string }>> = {
  PENDIENTE: { fondo: '#FFF1C2', borde: '#E3BC4A', texto: '#6B4800', etiqueta: 'Pendiente' },
  CONFIRMADA: { fondo: '#DDF2EA', borde: '#7FC3A6', texto: '#1C5E45', etiqueta: 'Confirmada' },
  EN_CURSO: { fondo: '#FCE1EA', borde: '#E75480', texto: '#8F2450', etiqueta: 'En curso' },
}

const RAYAS_BLOQUEO: CSSProperties = {
  backgroundImage: 'repeating-linear-gradient(135deg, #D6CFD3 0 6px, #B9B0B5 6px 12px)',
  color: '#3A3236',
}

const COLOR_TEMPORADA: Record<TipoTemporada, string> = {
  BASE: 'bg-muted text-muted-foreground',
  MEDIA: 'bg-durazno/30 text-foreground',
  ALTA: 'bg-coral/20 text-coral-texto',
}

export interface FilaCalendario {
  identificacion: string
  nombre: string
}

interface Props {
  desde: string
  cantidadNoches: number
  filas: FilaCalendario[]
  /** Calendario de cada apartamento, por identificación */
  calendarios: Record<string, CalendarioOcupacionResponse | undefined>
  temporadas: TemporadaResponse[]
  /** Titular de cada reserva, por código, si se conoce: se muestra en la barra */
  titulares?: Record<string, string>
  cargando?: boolean
  className?: string
}

/** Calendario de ocupación: una fila por apartamento y una columna por noche */
export function CalendarioOcupacion({
  desde,
  cantidadNoches,
  filas,
  calendarios,
  temporadas,
  titulares = {},
  cargando = false,
  className,
}: Props) {
  const fechas = noches(desde, cantidadNoches)
  const hoy = hoyISO()
  const columnas: CSSProperties = {
    gridTemplateColumns: `minmax(8.5rem, 10rem) repeat(${cantidadNoches}, minmax(2.6rem, 1fr))`,
  }

  // Tramos de temporada: noches seguidas con la misma temporada
  const tramosTemporada: { inicio: number; longitud: number; temporada?: TemporadaResponse }[] = []
  fechas.forEach((fecha, i) => {
    const temporada = temporadaDeFecha(fecha, temporadas)
    const ultimo = tramosTemporada[tramosTemporada.length - 1]
    if (ultimo && ultimo.temporada?.id === temporada?.id) ultimo.longitud++
    else tramosTemporada.push({ inicio: i, longitud: 1, temporada })
  })

  return (
    <div className={className}>
      <div className="overflow-x-auto rounded-tabla border border-border bg-card">
        <div role="table" aria-label="Calendario de ocupación" className="min-w-max text-xs">
          {/* Temporadas */}
          <div role="row" className="grid border-b border-border" style={columnas}>
            <div role="rowheader" className="sticky left-0 z-20 bg-card px-3 py-1.5 font-semibold text-muted-foreground">
              Temporada
            </div>
            {tramosTemporada.map((tramo) => (
              <div
                key={tramo.inicio}
                role="cell"
                style={{ gridColumn: `${tramo.inicio + 2} / span ${tramo.longitud}` }}
                className={cn(
                  'm-1 truncate rounded-full px-2 py-0.5 text-center font-semibold',
                  tramo.temporada ? COLOR_TEMPORADA[tramo.temporada.tipo] : 'bg-muted',
                )}
                title={tramo.temporada?.nombre}
              >
                {tramo.temporada?.nombre ?? '—'}
              </div>
            ))}
          </div>

          {/* Noches */}
          <div role="row" className="grid border-b border-border" style={columnas}>
            <div role="columnheader" className="sticky left-0 z-20 bg-card px-3 py-2 font-semibold text-muted-foreground">
              Apartamento
            </div>
            {fechas.map((fecha) => (
              <div
                key={fecha}
                role="columnheader"
                aria-label={formatearFecha(fecha, "EEEE d 'de' MMMM")}
                className={cn(
                  'flex flex-col items-center justify-center border-l border-border py-1.5 leading-tight',
                  esNocheDeFinDeSemana(fecha) && 'bg-sol/20',
                )}
              >
                <span className="text-[0.65rem] text-muted-foreground uppercase">{formatearFecha(fecha, 'EEEEEE')}</span>
                <span
                  className={cn(
                    'flex size-6 items-center justify-center rounded-full text-sm font-semibold tabular-nums',
                    fecha === hoy && 'bg-primary text-primary-foreground',
                  )}
                >
                  {formatearFecha(fecha, 'd')}
                </span>
              </div>
            ))}
          </div>

          {/* Apartamentos */}
          {filas.map((fila) => {
            const calendario = calendarios[fila.identificacion]
            const tramos = calendario ? tramosDelCalendario(calendario.noches.slice(0, cantidadNoches)) : []
            return (
              <div key={fila.identificacion} role="row" className="grid border-b border-border last:border-b-0" style={columnas}>
                <div
                  role="rowheader"
                  className="sticky left-0 z-20 flex flex-col justify-center bg-card px-3 py-2"
                  style={{ gridRow: 1, gridColumn: 1 }}
                >
                  <Link
                    to={`/panel/apartamentos/${encodeURIComponent(fila.identificacion)}`}
                    className="font-semibold hover:underline"
                  >
                    {fila.identificacion}
                  </Link>
                  <span className="truncate text-muted-foreground">{fila.nombre}</span>
                </div>

                {fechas.map((fecha, i) => (
                  <div
                    key={fecha}
                    aria-hidden="true"
                    style={{ gridRow: 1, gridColumn: i + 2 }}
                    className={cn('min-h-12 border-l border-border', esNocheDeFinDeSemana(fecha) && 'bg-sol/20')}
                  />
                ))}

                {!calendario && cargando && (
                  <div style={{ gridRow: 1, gridColumn: `2 / span ${cantidadNoches}` }} className="flex items-center px-2">
                    <Skeleton className="h-6 w-full rounded-full" />
                  </div>
                )}

                {tramos.map((tramo) => {
                  const posicion: CSSProperties = {
                    gridRow: 1,
                    gridColumn: `${tramo.inicio + 2} / span ${tramo.longitud}`,
                  }
                  const fechasTexto = `del ${formatearFecha(tramo.desde, "d 'de' MMMM")} al ${formatearFecha(tramo.hasta, "d 'de' MMMM")}`

                  if (tramo.tipo === 'bloqueo') {
                    return (
                      <div
                        key={`b-${tramo.inicio}`}
                        role="cell"
                        style={{ ...posicion, ...RAYAS_BLOQUEO }}
                        title={`Bloqueo: ${tramo.motivo}`}
                        className="relative z-10 mx-0.5 my-2.5 flex items-center overflow-hidden rounded-full px-2.5 font-semibold"
                      >
                        <span className="truncate">Bloqueo</span>
                        <span className="sr-only">: {tramo.motivo}, {fechasTexto}</span>
                      </div>
                    )
                  }

                  const color = COLOR_RESERVA[tramo.estado] ?? COLOR_RESERVA.CONFIRMADA!
                  const titular = titulares[tramo.codigo]
                  return (
                    <Link
                      key={`r-${tramo.codigo}-${tramo.inicio}`}
                      role="cell"
                      to={`/panel/reservas/${encodeURIComponent(tramo.codigo)}`}
                      style={{ ...posicion, backgroundColor: color.fondo, borderColor: color.borde, color: color.texto }}
                      title={`${tramo.codigo}${titular ? ` · ${titular}` : ''} · ${color.etiqueta}`}
                      aria-label={`Reserva ${tramo.codigo}${titular ? ` de ${titular}` : ''}, ${color.etiqueta.toLowerCase()}, ${fechasTexto}`}
                      className="relative z-10 mx-0.5 my-2.5 flex items-center overflow-hidden rounded-full border px-2.5 font-semibold transition-[filter] duration-150 hover:brightness-95 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                    >
                      <span className="truncate">{titular ?? tramo.codigo}</span>
                    </Link>
                  )
                })}
              </div>
            )
          })}
        </div>
      </div>

      <Leyenda />
    </div>
  )
}

function Leyenda() {
  return (
    <ul aria-label="Convenciones" className="mt-3 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted-foreground">
      {Object.entries(COLOR_RESERVA).map(([estado, color]) => (
        <li key={estado} className="flex items-center gap-1.5">
          <span
            aria-hidden="true"
            className="h-3 w-6 rounded-full border"
            style={{ backgroundColor: color.fondo, borderColor: color.borde }}
          />
          {color.etiqueta}
        </li>
      ))}
      <li className="flex items-center gap-1.5">
        <span aria-hidden="true" className="h-3 w-6 rounded-full" style={RAYAS_BLOQUEO} />
        Bloqueo
      </li>
      <li className="flex items-center gap-1.5">
        <span aria-hidden="true" className="h-3 w-6 rounded-sm bg-sol/40" />
        Viernes y sábado
      </li>
    </ul>
  )
}
