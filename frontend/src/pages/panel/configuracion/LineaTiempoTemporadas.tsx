import { differenceInCalendarDays, parseISO } from 'date-fns'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { TemporadaResponse } from '@/api/tipos'
import { Button } from '@/components/ui/button'
import { hoyISO } from '@/pages/publico/comun/reglas'
import { COLOR_TEMPORADA, segmentoEnAnio, textoRango } from './utilidadesTemporada'

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']

interface Props {
  temporadas: TemporadaResponse[]
  anio: number
  alCambiarAnio: (anio: number) => void
}

/** Las temporadas del año en una línea de tiempo: una fila por temporada, la base cubre el resto */
export function LineaTiempoTemporadas({ temporadas, anio, alCambiarAnio }: Props) {
  const hoy = hoyISO()
  const diasAnio = differenceInCalendarDays(parseISO(`${anio + 1}-01-01`), parseISO(`${anio}-01-01`))
  const posicionHoy = hoy.startsWith(`${anio}-`)
    ? (differenceInCalendarDays(parseISO(hoy), parseISO(`${anio}-01-01`)) / diasAnio) * 100
    : null
  // La base primero, luego media y alta
  const orden = { BASE: 0, MEDIA: 1, ALTA: 2 }
  const filas = [...temporadas].sort((a, b) => orden[a.tipo] - orden[b.tipo])

  return (
    <div className="flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
      <div className="flex items-center justify-between gap-3">
        <h2 className="font-titulo text-xl font-semibold">Año {anio}</h2>
        <div className="flex gap-2">
          <Button variant="outline" size="icon" aria-label="Año anterior" onClick={() => alCambiarAnio(anio - 1)}>
            <ChevronLeft aria-hidden="true" />
          </Button>
          <Button variant="outline" size="icon" aria-label="Año siguiente" onClick={() => alCambiarAnio(anio + 1)}>
            <ChevronRight aria-hidden="true" />
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        {/* Meses: iniciales en el celular, abreviatura desde sm */}
        <div aria-hidden="true" className="grid grid-cols-12 text-center text-[0.7rem] text-muted-foreground">
          {MESES.map((m) => (
            <span key={m}>
              <span className="sm:hidden">{m[0]}</span>
              <span className="hidden sm:inline">{m}</span>
            </span>
          ))}
        </div>

        {filas.length === 0 && <p className="text-sm text-muted-foreground">No hay temporadas registradas.</p>}

        {filas.map((t) => {
          const color = COLOR_TEMPORADA[t.tipo]
          const segmentos = t.rangos
            .map((r) => ({ rango: r, posicion: segmentoEnAnio(r, anio) }))
            .filter((s) => s.posicion !== null)
          const resumen =
            t.tipo === 'BASE'
              ? 'cubre las fechas que no son de otra temporada'
              : segmentos.length === 0
                ? `sin fechas en ${anio}`
                : segmentos.map((s) => textoRango(s.rango)).join('; ')
          return (
            <div key={t.id} className="flex flex-col gap-1">
              <p className="flex flex-wrap items-baseline gap-x-2 text-sm">
                <span className="font-semibold">{t.nombre}</span>
                <span className="text-xs text-muted-foreground">
                  {color.etiqueta} · <span className="sr-only">{resumen}</span>
                  <span aria-hidden="true">{t.tipo === 'BASE' ? 'resto del año' : `${segmentos.length} ${segmentos.length === 1 ? 'rango' : 'rangos'}`}</span>
                </span>
              </p>
              <div aria-hidden="true" className="relative h-6 overflow-hidden rounded-full bg-muted">
                {/* Divisiones de los meses */}
                <div className="absolute inset-0 grid grid-cols-12">
                  {MESES.map((m) => (
                    <span key={m} className="border-l border-border/70 first:border-l-0" />
                  ))}
                </div>
                {t.tipo === 'BASE' ? (
                  <div className="absolute inset-0 opacity-60" style={{ backgroundColor: color.barra }} />
                ) : (
                  segmentos.map(({ rango, posicion }) => (
                    <div
                      key={rango.fechaInicio}
                      title={textoRango(rango)}
                      className="absolute inset-y-0 rounded-full"
                      style={{
                        left: `${posicion!.izquierda}%`,
                        width: `max(${posicion!.ancho}%, 4px)`,
                        backgroundColor: color.barra,
                      }}
                    />
                  ))
                )}
                {posicionHoy !== null && (
                  <span className="absolute inset-y-0 w-0.5 bg-foreground" style={{ left: `${posicionHoy}%` }} />
                )}
              </div>
            </div>
          )
        })}
      </div>

      <ul className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
        {(['BASE', 'MEDIA', 'ALTA'] as const).map((tipo) => (
          <li key={tipo} className="inline-flex items-center gap-1.5">
            <span aria-hidden="true" className="size-3 rounded-full" style={{ backgroundColor: COLOR_TEMPORADA[tipo].barra }} />
            {COLOR_TEMPORADA[tipo].etiqueta}
          </li>
        ))}
        {posicionHoy !== null && (
          <li className="inline-flex items-center gap-1.5">
            <span aria-hidden="true" className="h-3 w-0.5 bg-foreground" /> Hoy
          </li>
        )}
      </ul>
    </div>
  )
}
