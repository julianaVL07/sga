import { useState, type ReactNode } from 'react'
import { endOfMonth, format, startOfMonth } from 'date-fns'
import { BedDouble, HandCoins, Share2 } from 'lucide-react'
import {
  useReporteIngresos,
  useReporteOcupacion,
  useReporteReservasPorCanal,
  type PeriodoReporte,
} from '@/api/reportes'
import { AvisoError, Campo, Dinero, formatearFecha } from '@/components/coral'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { NOMBRE_MEDIO } from '@/pages/panel/folio/movimientos'
import { NOMBRE_CANAL } from '@/pages/huesped/reserva/formato'
import { BarraPorcentaje } from './BarraPorcentaje'
import { formatearPorcentaje } from './formato'

/** Por defecto, el mes en curso completo */
function periodoInicial(): PeriodoReporte {
  const hoy = new Date()
  return { desde: format(startOfMonth(hoy), 'yyyy-MM-dd'), hasta: format(endOfMonth(hoy), 'yyyy-MM-dd') }
}

export default function Reportes() {
  const [periodo, setPeriodo] = useState<PeriodoReporte>(periodoInicial)
  const completo = !!periodo.desde && !!periodo.hasta
  const invertido = completo && periodo.desde > periodo.hasta
  const valido = completo && !invertido

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-6">
      <header>
        <h1 className="font-titulo text-3xl font-semibold">Reportes</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Ocupación, ingresos y reservas por canal del periodo elegido. Las dos fechas van incluidas.
        </p>
      </header>

      {/* ---------- Selector de fechas ---------- */}
      <div className="flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta sm:flex-row sm:items-start">
        <Campo id="reporte-desde" etiqueta="Desde" className="sm:w-52">
          <Input type="date" value={periodo.desde} onChange={(e) => setPeriodo({ ...periodo, desde: e.target.value })} />
        </Campo>
        <Campo
          id="reporte-hasta"
          etiqueta="Hasta"
          className="sm:w-52"
          error={invertido ? 'Debe ser igual o posterior a la fecha desde' : !completo ? 'Elige las dos fechas' : undefined}
        >
          <Input
            type="date"
            value={periodo.hasta}
            min={periodo.desde || undefined}
            onChange={(e) => setPeriodo({ ...periodo, hasta: e.target.value })}
          />
        </Campo>
        {valido && (
          <p className="text-sm text-muted-foreground sm:mt-8" aria-live="polite">
            Del {formatearFecha(periodo.desde, "d 'de' MMMM yyyy")} al {formatearFecha(periodo.hasta, "d 'de' MMMM yyyy")}
          </p>
        )}
      </div>

      <TablaOcupacion periodo={periodo} habilitada={valido} />
      <TablaIngresos periodo={periodo} habilitada={valido} />
      <TablaCanales periodo={periodo} habilitada={valido} />
    </section>
  )
}

interface PropsTabla {
  periodo: PeriodoReporte
  habilitada: boolean
}

/** Marco común de cada reporte: título, resumen y estados de carga o error */
function Tarjeta({
  titulo,
  icono: Icono,
  resumen,
  error,
  cargando,
  children,
}: {
  titulo: string
  icono: typeof BedDouble
  resumen?: ReactNode
  error: unknown
  cargando: boolean
  children: ReactNode
}) {
  return (
    <article className="flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="flex items-center gap-2 font-titulo text-xl font-semibold">
          <Icono aria-hidden="true" className="size-5 text-coral" /> {titulo}
        </h2>
        {resumen && <p className="text-sm text-muted-foreground">{resumen}</p>}
      </div>
      {error ? <AvisoError error={error} /> : cargando ? <Skeleton className="h-40" /> : children}
    </article>
  )
}

function TablaOcupacion({ periodo, habilitada }: PropsTabla) {
  const reporte = useReporteOcupacion(periodo, habilitada)
  const datos = reporte.data

  return (
    <Tarjeta
      titulo="Ocupación"
      icono={BedDouble}
      error={reporte.error}
      cargando={habilitada && !datos}
      resumen={
        datos && (
          <>
            {datos.nochesVendidas} de {datos.nochesDisponibles} noches vendidas ({datos.apartamentosActivos}{' '}
            apartamentos × {datos.nochesPeriodo} noches)
          </>
        )
      }
    >
      {datos && (
        <>
          <BarraPorcentaje valor={datos.porcentajeOcupacion} etiqueta="Ocupación total del periodo" />
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Apartamento</TableHead>
                  <TableHead className="text-right">Noches vendidas</TableHead>
                  <TableHead className="w-1/2">Ocupación</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {datos.apartamentos.map((a) => (
                  <TableRow key={a.apartamento}>
                    <TableCell className="font-medium">
                      {a.apartamento}
                      {!a.activo && <span className="ml-2 text-xs text-muted-foreground">(no está a la venta)</span>}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{a.nochesVendidas}</TableCell>
                    <TableCell>
                      <BarraPorcentaje valor={a.porcentajeOcupacion} etiqueta={`Ocupación de ${a.apartamento}`} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <p className="text-xs text-muted-foreground">
            Cuentan las noches de reservas confirmadas, en curso o finalizadas.
          </p>
        </>
      )}
    </Tarjeta>
  )
}

function TablaIngresos({ periodo, habilitada }: PropsTabla) {
  const reporte = useReporteIngresos(periodo, habilitada)
  const datos = reporte.data

  return (
    <Tarjeta
      titulo="Ingresos"
      icono={HandCoins}
      error={reporte.error}
      cargando={habilitada && !datos}
      resumen={datos && `${datos.cantidadPagos} ${datos.cantidadPagos === 1 ? 'pago' : 'pagos'} en el periodo`}
    >
      {datos && (
        <>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Medio de pago</TableHead>
                  <TableHead className="text-right">Pagos</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                  <TableHead className="w-2/5">Participación</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {datos.medios.map((m) => (
                  <TableRow key={m.medio}>
                    <TableCell className="font-medium">{NOMBRE_MEDIO[m.medio]}</TableCell>
                    <TableCell className="text-right tabular-nums">{m.cantidadPagos}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      <Dinero valor={m.total} />
                    </TableCell>
                    <TableCell>
                      <BarraPorcentaje valor={m.porcentaje} etiqueta={`Participación de ${NOMBRE_MEDIO[m.medio]}`} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
              <TableFooter>
                <TableRow>
                  <TableCell className="font-semibold">Total</TableCell>
                  <TableCell className="text-right tabular-nums">{datos.cantidadPagos}</TableCell>
                  <TableCell className="text-right tabular-nums">
                    <Dinero valor={datos.total} className="font-semibold" />
                  </TableCell>
                  <TableCell />
                </TableRow>
              </TableFooter>
            </Table>
          </div>
          <p className="text-xs text-muted-foreground">Suma de los pagos registrados en los folios; los cargos no cuentan.</p>
        </>
      )}
    </Tarjeta>
  )
}

function TablaCanales({ periodo, habilitada }: PropsTabla) {
  const reporte = useReporteReservasPorCanal(periodo, habilitada)
  const datos = reporte.data
  const principal =
    datos && datos.totalReservas > 0 ? [...datos.canales].sort((a, b) => b.total - a.total)[0] : undefined

  return (
    <Tarjeta
      titulo="Reservas por canal"
      icono={Share2}
      error={reporte.error}
      cargando={habilitada && !datos}
      resumen={datos && `${datos.totalReservas} ${datos.totalReservas === 1 ? 'reserva' : 'reservas'} con entrada en el periodo`}
    >
      {datos && (
        <>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Canal</TableHead>
                  <TableHead className="text-right">Reservas</TableHead>
                  <TableHead className="text-right">Activas</TableHead>
                  <TableHead className="text-right">Finalizadas</TableHead>
                  <TableHead className="text-right">Canceladas</TableHead>
                  <TableHead className="text-right">No-show</TableHead>
                  <TableHead className="min-w-48">Participación</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {datos.canales.map((c) => (
                  <TableRow key={c.canal}>
                    <TableCell className="font-medium">{NOMBRE_CANAL[c.canal]}</TableCell>
                    <TableCell className="text-right tabular-nums">{c.total}</TableCell>
                    <TableCell className="text-right tabular-nums">{c.activas}</TableCell>
                    <TableCell className="text-right tabular-nums">{c.finalizadas}</TableCell>
                    <TableCell className="text-right tabular-nums">{c.canceladas}</TableCell>
                    <TableCell className="text-right tabular-nums">{c.noShow}</TableCell>
                    <TableCell>
                      <BarraPorcentaje valor={c.porcentaje} etiqueta={`Participación del canal ${NOMBRE_CANAL[c.canal]}`} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <p className="text-xs text-muted-foreground">
            Incluye las reservas canceladas y los no-show. Activas: pendientes, confirmadas o en curso.
            {principal && ` El canal principal es ${NOMBRE_CANAL[principal.canal]} (${formatearPorcentaje(principal.porcentaje)}).`}
          </p>
        </>
      )}
    </Tarjeta>
  )
}
