import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarPlus, CalendarRange, Plus } from 'lucide-react'
import { useTodasLasTemporadas } from '@/api/apartamentos'
import type { TemporadaResponse } from '@/api/tipos'
import { AvisoError, Vacio } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { PildoraTemporada } from '@/pages/publico/comun/PildoraTemporada'
import { DialogoAgregarRango, DialogoCrearTemporada } from './DialogosTemporada'
import { LineaTiempoTemporadas } from './LineaTiempoTemporadas'
import { diasDeRango, textoRango } from './utilidadesTemporada'

export default function Temporadas() {
  const temporadas = useTodasLasTemporadas()
  const [anio, setAnio] = useState(() => new Date().getFullYear())
  const [creando, setCreando] = useState(false)
  const [conRango, setConRango] = useState<TemporadaResponse | null>(null)
  const [rangoAbierto, setRangoAbierto] = useState(false)

  const lista = temporadas.data ?? []

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-titulo text-3xl font-semibold">Temporadas y tarifas</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Cada noche se cobra con la tarifa de su temporada. Las tarifas de cada apartamento se cambian en{' '}
            <Link to="/panel/apartamentos" className="font-medium text-primary hover:underline">
              Apartamentos
            </Link>
            .
          </p>
        </div>
        <Button onClick={() => setCreando(true)}>
          <Plus aria-hidden="true" /> Nueva temporada
        </Button>
      </header>

      {temporadas.error ? (
        <AvisoError titulo="No se pudieron consultar las temporadas" error={temporadas.error} />
      ) : temporadas.isPending ? (
        <>
          <Skeleton className="h-56" />
          <Skeleton className="h-40" />
        </>
      ) : lista.length === 0 ? (
        <div className="rounded-tarjeta border border-border bg-card shadow-tarjeta">
          <Vacio icono={CalendarRange} titulo="Sin temporadas" descripcion="Crea la temporada base y luego las de temporada media y alta." />
        </div>
      ) : (
        <>
          <LineaTiempoTemporadas temporadas={lista} anio={anio} alCambiarAnio={setAnio} />

          <ul className="grid gap-4 md:grid-cols-2">
            {lista.map((t) => (
              <li key={t.id} className="flex flex-col gap-3 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <h2 className="font-titulo text-lg font-semibold">{t.nombre}</h2>
                    <p className="font-mono text-xs text-muted-foreground">{t.id}</p>
                  </div>
                  <PildoraTemporada tipo={t.tipo} />
                </div>
                {t.tipo === 'BASE' ? (
                  <p className="text-sm text-muted-foreground">
                    No tiene rangos: cubre todas las fechas que no son de otra temporada.
                  </p>
                ) : (
                  <>
                    {t.rangos.length === 0 ? (
                      <p className="text-sm text-muted-foreground">Sin rangos.</p>
                    ) : (
                      <ul className="flex flex-col divide-y divide-border text-sm">
                        {[...t.rangos]
                          .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio))
                          .map((r) => (
                            <li key={r.fechaInicio} className="flex justify-between gap-3 py-1.5">
                              <span>{textoRango(r)}</span>
                              <span className="shrink-0 text-muted-foreground">
                                {diasDeRango(r)} {diasDeRango(r) === 1 ? 'día' : 'días'}
                              </span>
                            </li>
                          ))}
                      </ul>
                    )}
                    <Button
                      variant="outline"
                      size="sm"
                      className="w-fit"
                      onClick={() => {
                        setConRango(t)
                        setRangoAbierto(true)
                      }}
                    >
                      <CalendarPlus aria-hidden="true" /> Agregar rango
                    </Button>
                  </>
                )}
              </li>
            ))}
          </ul>
        </>
      )}

      <DialogoCrearTemporada temporadas={lista} abierto={creando} alCambiar={setCreando} />
      <DialogoAgregarRango temporada={conRango} abierto={rangoAbierto} alCambiar={setRangoAbierto} />
    </section>
  )
}
