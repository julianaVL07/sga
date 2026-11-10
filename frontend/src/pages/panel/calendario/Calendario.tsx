import { useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useTodasLasTemporadas, useTodosLosApartamentos } from '@/api/apartamentos'
import { useCalendarios } from '@/api/ocupacion'
import { AvisoError, formatearFecha } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { CalendarioOcupacion } from './CalendarioOcupacion'
import { claseSelect } from './estilos'
import { hoyISO, inicioDeSemana, sumarDias } from './fechas'
import { useTitulares } from './useTitulares'

/** Tres semanas a la vista; las flechas avanzan de a una semana */
const NOCHES_VISIBLES = 21

export default function Calendario() {
  const [desde, setDesde] = useState(() => inicioDeSemana(hoyISO()))
  const [filtro, setFiltro] = useState('')
  const hasta = sumarDias(desde, NOCHES_VISIBLES)

  const apartamentos = useTodosLosApartamentos()
  const temporadas = useTodasLasTemporadas()
  const lista = apartamentos.data ?? []
  const visibles = filtro ? lista.filter((a) => a.identificacion === filtro) : lista
  const ids = visibles.map((a) => a.identificacion)
  const consultas = useCalendarios(ids, desde, hasta)

  const titulares = useTitulares()

  const calendarios = Object.fromEntries(ids.map((id, i) => [id, consultas[i]?.data]))
  const errorCalendario = consultas.find((c) => c.error)?.error
  const esEstaSemana = desde === inicioDeSemana(hoyISO())

  return (
    <section className="mx-auto w-full max-w-7xl">
      <h1 className="sr-only">Calendario de ocupación</h1>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon" aria-label="Semana anterior" onClick={() => setDesde(sumarDias(desde, -7))}>
            <ChevronLeft aria-hidden="true" />
          </Button>
          <p className="min-w-0 flex-1 text-center font-semibold sm:flex-none" aria-live="polite">
            {formatearFecha(desde, "d 'de' MMM")} – {formatearFecha(sumarDias(hasta, -1), "d 'de' MMM yyyy")}
          </p>
          <Button variant="outline" size="icon" aria-label="Semana siguiente" onClick={() => setDesde(sumarDias(desde, 7))}>
            <ChevronRight aria-hidden="true" />
          </Button>
          <Button variant="ghost" disabled={esEstaSemana} onClick={() => setDesde(inicioDeSemana(hoyISO()))}>
            Esta semana
          </Button>
        </div>

        <div className="flex items-center gap-2">
          <label htmlFor="filtro-apartamento" className="text-sm font-medium whitespace-nowrap">
            Apartamento
          </label>
          <select
            id="filtro-apartamento"
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
            className={claseSelect + ' sm:w-60'}
          >
            <option value="">Todos</option>
            {lista.map((a) => (
              <option key={a.identificacion} value={a.identificacion}>
                {a.identificacion} · {a.nombre}
              </option>
            ))}
          </select>
        </div>
      </div>

      {apartamentos.error && <AvisoError className="mt-4" error={apartamentos.error} />}
      {errorCalendario && <AvisoError className="mt-4" error={errorCalendario} />}

      <CalendarioOcupacion
        className="mt-5"
        desde={desde}
        cantidadNoches={NOCHES_VISIBLES}
        filas={visibles}
        calendarios={calendarios}
        temporadas={temporadas.data ?? []}
        titulares={titulares}
        cargando={apartamentos.isPending || consultas.some((c) => c.isPending)}
      />
    </section>
  )
}
