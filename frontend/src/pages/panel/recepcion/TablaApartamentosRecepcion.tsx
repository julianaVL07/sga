import { Link } from 'react-router-dom'
import type { CalendarioOcupacionResponse, ReservaResumenResponse } from '@/api/ocupacion'
import type { ApartamentoDetalleResponse, ApartamentoResponse } from '@/api/tipos'
import { AvisoError, EstadoOperativo, Fecha, TablaResponsive, type ColumnaTabla } from '@/components/coral'
import { Skeleton } from '@/components/ui/skeleton'
import { MenuAccionesApartamento } from '../apartamentos/AccionesApartamento'
import { tramosDelCalendario } from '../calendario/fechas'

interface Props {
  fecha: string
  apartamentos: ApartamentoResponse[] | undefined
  detalles: Record<string, ApartamentoDetalleResponse | undefined>
  calendarios: Record<string, CalendarioOcupacionResponse | undefined>
  enCurso: ReservaResumenResponse[]
  titulares: Record<string, string>
  error?: unknown
}

type Movimiento =
  | { tipo: 'llegada'; fecha: string; codigo: string; titular?: string }
  | { tipo: 'bloqueo'; fecha: string; hasta: string; motivo: string; enCurso: boolean }

/**
 * Próximo movimiento: la próxima llegada que muestra el calendario (21 noches) o el próximo bloqueo
 * vigente del detalle, el que ocurra primero.
 */
function proximoMovimiento(
  fecha: string,
  calendario: CalendarioOcupacionResponse | undefined,
  detalle: ApartamentoDetalleResponse | undefined,
  codigoEnCasa: string | undefined,
  titulares: Record<string, string>,
): Movimiento | null {
  const llegada = calendario
    ? tramosDelCalendario(calendario.noches).find(
        (t) => t.tipo === 'reserva' && t.codigo !== codigoEnCasa && t.estado !== 'EN_CURSO',
      )
    : undefined

  const bloqueo = (detalle?.bloqueosVigentes ?? [])
    .filter((b) => b.fechaFin > fecha)
    .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio))[0]

  const candidatos: Movimiento[] = []
  if (llegada && llegada.tipo === 'reserva') {
    candidatos.push({ tipo: 'llegada', fecha: llegada.desde, codigo: llegada.codigo, titular: titulares[llegada.codigo] })
  }
  if (bloqueo) {
    candidatos.push({
      tipo: 'bloqueo',
      fecha: bloqueo.fechaInicio,
      hasta: bloqueo.fechaFin,
      motivo: bloqueo.motivo,
      enCurso: bloqueo.fechaInicio <= fecha,
    })
  }
  return candidatos.sort((a, b) => a.fecha.localeCompare(b.fecha))[0] ?? null
}

/** Apartamentos con su estado, quién está ahora y el próximo movimiento */
export function TablaApartamentosRecepcion({ fecha, apartamentos, detalles, calendarios, enCurso, titulares, error }: Props) {
  const enCasaPorApartamento = new Map(enCurso.map((r) => [r.apartamento, r]))

  const columnas: ColumnaTabla<ApartamentoResponse>[] = [
    {
      clave: 'apartamento',
      titulo: 'Apartamento',
      ocultarEnTarjeta: true,
      celda: (a) => (
        <Link to={`/panel/apartamentos/${encodeURIComponent(a.identificacion)}`} className="flex flex-col hover:underline">
          <span className="font-semibold">{a.identificacion}</span>
          <span className="text-xs text-muted-foreground">{a.nombre}</span>
        </Link>
      ),
    },
    {
      clave: 'capacidad',
      titulo: 'Capacidad',
      celda: (a) => (
        <span className="tabular-nums">
          {a.capacidadMaxima} {a.capacidadMaxima === 1 ? 'persona' : 'personas'}
        </span>
      ),
    },
    {
      clave: 'estado',
      titulo: 'Estado',
      celda: (a) => (
        <span className="inline-flex flex-wrap items-center justify-end gap-1.5 md:justify-start">
          <EstadoOperativo estado={a.estadoOperativo} />
          {detalles[a.identificacion]?.activo === false && (
            <span className="rounded-full border border-border px-2 py-0.5 text-xs font-semibold text-muted-foreground">
              Inactivo
            </span>
          )}
        </span>
      ),
    },
    {
      clave: 'ahora',
      titulo: 'Ahora',
      celda: (a) => {
        const reserva = enCasaPorApartamento.get(a.identificacion)
        if (!reserva) return <span className="text-muted-foreground">Libre</span>
        return (
          <Link
            to={`/panel/reservas/${encodeURIComponent(reserva.codigo)}`}
            className="inline-flex flex-col hover:underline md:items-start"
          >
            <span className="font-medium">{reserva.nombreTitular}</span>
            <span className="text-xs text-muted-foreground">
              Sale <Fecha valor={reserva.fechaSalida} />
            </span>
          </Link>
        )
      },
    },
    {
      clave: 'proximo',
      titulo: 'Próximo movimiento',
      celda: (a) => {
        const calendario = calendarios[a.identificacion]
        if (!calendario && !detalles[a.identificacion]) return <Skeleton className="ml-auto h-4 w-28 md:ml-0" />
        const mov = proximoMovimiento(
          fecha,
          calendario,
          detalles[a.identificacion],
          enCasaPorApartamento.get(a.identificacion)?.codigo,
          titulares,
        )
        if (!mov) return <span className="text-muted-foreground">Sin movimientos en 3 semanas</span>
        if (mov.tipo === 'llegada') {
          return (
            <Link
              to={`/panel/reservas/${encodeURIComponent(mov.codigo)}`}
              className="inline-flex flex-col hover:underline md:items-start"
            >
              <span>
                Llegada · <Fecha valor={mov.fecha} />
              </span>
              <span className="text-xs text-muted-foreground">{mov.titular ?? mov.codigo}</span>
            </Link>
          )
        }
        return (
          <span className="inline-flex flex-col md:items-start" title={mov.motivo}>
            <span>
              {mov.enCurso ? (
                <>
                  Bloqueado hasta <Fecha valor={mov.hasta} />
                </>
              ) : (
                <>
                  Bloqueo · <Fecha valor={mov.fecha} />
                </>
              )}
            </span>
            <span className="block max-w-40 truncate text-xs text-muted-foreground sm:max-w-56">{mov.motivo}</span>
          </span>
        )
      },
    },
  ]

  return (
    <section aria-labelledby="titulo-apartamentos" className="rounded-tarjeta border border-border bg-card p-3 shadow-tarjeta md:p-4">
      <div className="mb-3 flex items-center justify-between gap-3 px-1">
        <h2 id="titulo-apartamentos" className="font-titulo text-xl font-semibold">
          Apartamentos
        </h2>
        <Link to="/panel/apartamentos" className="text-sm font-medium text-coral-texto hover:underline">
          Ver todos
        </Link>
      </div>
      {error ? (
        <AvisoError error={error} />
      ) : !apartamentos ? (
        <div className="flex flex-col gap-2">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : (
        <TablaResponsive
          columnas={columnas}
          filas={apartamentos}
          claveFila={(a) => a.identificacion}
          tituloTarjeta={(a) => (
            <Link to={`/panel/apartamentos/${encodeURIComponent(a.identificacion)}`} className="hover:underline">
              {a.identificacion} · {a.nombre}
            </Link>
          )}
          acciones={(a) => <MenuAccionesApartamento apartamento={a} />}
          descripcion="Estado de los apartamentos"
        />
      )}
    </section>
  )
}
