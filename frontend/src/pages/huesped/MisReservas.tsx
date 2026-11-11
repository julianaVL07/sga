import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, ChevronRight, Luggage, Users } from 'lucide-react'
import { useTodosLosApartamentos } from '@/api/apartamentos'
import { useMisReservas } from '@/api/reservas'
import type { EstadoReserva as Estado } from '@/api/tipos'
import { AvisoError, Dinero, EstadoReserva, Fecha, Paginador, Vacio } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { nochesEntre } from '@/pages/publico/comun/reglas'

const FILTROS_ESTADO: { valor: Estado | undefined; texto: string }[] = [
  { valor: undefined, texto: 'Todas' },
  { valor: 'PENDIENTE', texto: 'Pendientes' },
  { valor: 'CONFIRMADA', texto: 'Confirmadas' },
  { valor: 'EN_CURSO', texto: 'En curso' },
  { valor: 'FINALIZADA', texto: 'Finalizadas' },
  { valor: 'CANCELADA', texto: 'Canceladas' },
  { valor: 'NO_SHOW', texto: 'No se presentó' },
]

export default function MisReservas() {
  const [estado, setEstado] = useState<Estado | undefined>()
  const [pagina, setPagina] = useState(0)
  const consulta = useMisReservas(estado, pagina)
  const apartamentos = useTodosLosApartamentos()
  const nombre = (id: string) => apartamentos.data?.find((a) => a.identificacion === id)?.nombre

  return (
    <section className="mx-auto flex w-full max-w-4xl flex-col gap-6">
      <div>
        <h1 className="font-titulo text-3xl font-semibold md:text-4xl">Mis reservas</h1>
        <p className="mt-1 text-sm text-muted-foreground">Tus estancias en Coral, de la más reciente a la más antigua.</p>
      </div>

      <div role="group" aria-label="Filtrar por estado" className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1">
        {FILTROS_ESTADO.map((f) => (
          <button
            key={f.texto}
            type="button"
            aria-pressed={estado === f.valor}
            onClick={() => {
              setEstado(f.valor)
              setPagina(0)
            }}
            className={cn(
              'min-h-11 shrink-0 rounded-full border px-4 text-sm font-medium whitespace-nowrap transition-colors',
              estado === f.valor
                ? 'border-primary bg-primary text-primary-foreground'
                : 'border-border bg-card hover:bg-muted',
            )}
          >
            {f.texto}
          </button>
        ))}
      </div>

      {consulta.error ? (
        <AvisoError error={consulta.error} />
      ) : !consulta.data ? (
        <div className="flex flex-col gap-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-28" />
          ))}
        </div>
      ) : consulta.data.contenido.length === 0 ? (
        <div className="rounded-tarjeta border border-border bg-card">
          <Vacio
            icono={Luggage}
            titulo={estado ? 'No tienes reservas en este estado' : 'Todavía no tienes reservas'}
            descripcion="Busca un apartamento y reserva tu próxima estancia frente al mar."
            accion={
              <Button asChild>
                <Link to="/buscar">Buscar apartamentos</Link>
              </Button>
            }
          />
        </div>
      ) : (
        <>
          <ul className="flex flex-col gap-3">
            {consulta.data.contenido.map((r) => {
              const noches = nochesEntre(r.fechaEntrada, r.fechaSalida)
              return (
                <li key={r.codigo}>
                  <Link
                    to={`/mis-reservas/${encodeURIComponent(r.codigo)}`}
                    className="flex items-center gap-4 rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta transition-colors hover:bg-muted/40 md:p-5"
                  >
                    <div className="flex min-w-0 flex-1 flex-col gap-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold">{r.codigo}</span>
                        <EstadoReserva estado={r.estado} />
                      </div>
                      <p className="truncate text-sm">
                        {r.apartamento}
                        {nombre(r.apartamento) && <span className="text-muted-foreground"> · {nombre(r.apartamento)}</span>}
                      </p>
                      <p className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
                        <span className="inline-flex items-start gap-1">
                          <CalendarDays aria-hidden="true" className="mt-0.5 size-3.5 shrink-0" />
                          <span>
                            <Fecha valor={r.fechaEntrada} /> → <Fecha valor={r.fechaSalida} /> ·{' '}
                            <span className="whitespace-nowrap">
                              {noches} {noches === 1 ? 'noche' : 'noches'}
                            </span>
                          </span>
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <Users aria-hidden="true" className="size-3.5" /> {r.totalOcupantes}
                        </span>
                      </p>
                    </div>
                    <div className="flex flex-col items-end gap-1">
                      <Dinero valor={r.valorTotal} className="font-semibold" />
                      <ChevronRight aria-hidden="true" className="size-5 text-muted-foreground" />
                    </div>
                  </Link>
                </li>
              )
            })}
          </ul>
          <Paginador
            pagina={pagina}
            totalPaginas={consulta.data.totalPaginas}
            totalElementos={consulta.data.totalElementos}
            alCambiar={setPagina}
          />
        </>
      )}
    </section>
  )
}
