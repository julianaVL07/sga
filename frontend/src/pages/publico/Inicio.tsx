import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bath, BedDouble, Clock, Dog, HandCoins, ScrollText, ShieldCheck, Sparkles, Waves } from 'lucide-react'
import { useDetallesApartamentos, useTodosLosApartamentos } from '@/api/apartamentos'
import { usePoliticaVigente } from '@/api/publico'
import { AvisoError, Dinero, FotoApartamento, Onda, Vacio } from '@/components/coral'
import { Skeleton } from '@/components/ui/skeleton'
import { IMAGEN_PORTADA_INICIO } from '@/lib/imagenes'
import { cn } from '@/lib/utils'
import { BuscadorEstancia } from './comun/BuscadorEstancia'
import { useReglasAlojamiento, useTemporadasAlojamiento } from './comun/consultas'
import { estanciaAParams } from './comun/estancia'
import { ListaPolitica } from './comun/ListaPolitica'
import { conHora, conPunto, textoMascotas, textoPlazo, textoPorcentajeAnticipo } from './comun/reglas'
import { TarjetaApartamento } from './comun/TarjetaApartamento'
import { TextoCargo } from './comun/TextoCargo'

export default function Inicio() {
  const navigate = useNavigate()
  const { alojamiento, reglas } = useReglasAlojamiento()

  return (
    <div className="flex flex-col gap-16">
      {/* ---------- Portada ---------- */}
      <section aria-labelledby="titulo-portada" className="relative">
        <div className="relative -mx-4 -mt-8 overflow-hidden sm:mx-0 sm:mt-0 sm:rounded-tarjeta">
          {/* Si la imagen no carga, FotoApartamento deja el fondo de ondas */}
          <FotoApartamento src={IMAGEN_PORTADA_INICIO} alt="" className="h-[26rem] w-full md:h-[32rem]" />
          <div aria-hidden="true" className="absolute inset-0 bg-gradient-to-t from-ciruela/85 via-ciruela/40 to-ciruela/10" />
          <div className="absolute inset-x-0 top-0 flex flex-col gap-3 px-6 pt-14 text-white md:px-12 md:pt-20">
            <p className="text-xs font-semibold tracking-[0.28em] text-durazno uppercase">Bocagrande · Cartagena</p>
            <h1 id="titulo-portada" className="max-w-2xl font-titulo text-4xl leading-tight font-semibold md:text-6xl">
              Encuentra tu lugar ideal.
            </h1>
            <p className="max-w-xl text-base text-white/90 md:text-lg">
              Cinco apartamentos de alta gama frente al mar en Bocagrande, Cartagena
            </p>
          </div>
          <Onda className="absolute inset-x-0 bottom-0 h-20 md:h-28" />
        </div>
        <BuscadorEstancia
          className="relative z-10 mx-auto -mt-16 max-w-5xl md:-mt-20"
          inicial={{ entrada: '', salida: '', huespedes: [''] }}
          umbralEdad={reglas.umbralEdad}
          alBuscar={(estancia) => navigate(`/buscar?${estanciaAParams(estancia).toString()}`)}
        />
      </section>

      {/* ---------- Franja de datos ---------- */}
      <section aria-label="Lo que te espera">
        <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {[
            { icono: Waves, titulo: 'Frente al mar', texto: 'A pasos de la playa de Bocagrande' },
            {
              icono: Clock,
              titulo: conHora('Entrada', reglas.horaEntrada, ''),
              texto: conHora('Salida', reglas.horaSalida, ''),
            },
            {
              icono: Dog,
              titulo: 'Mascotas pequeñas',
              texto: textoMascotas(reglas),
            },
            { icono: Bath, titulo: 'Hidromasaje privado', texto: 'Para relajarte después del mar' },
          ].map(({ icono: Icono, titulo, texto }) => (
            <li key={titulo} className="flex items-start gap-3 rounded-tarjeta border border-border bg-card p-4">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-crema">
                <Icono aria-hidden="true" className="size-5 text-coral" />
              </span>
              <span className="flex flex-col">
                <span className="text-sm font-semibold">{titulo}</span>
                <span className="text-xs text-muted-foreground">{texto}</span>
              </span>
            </li>
          ))}
        </ul>
      </section>

      <SeccionApartamentos />

      {/* ---------- Servicios adicionales ---------- */}
      <section aria-labelledby="titulo-servicios">
        <h2 id="titulo-servicios" className="font-titulo text-3xl font-semibold">
          Servicios adicionales
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">Pídelos en recepción durante tu estancia.</p>
        {alojamiento.error ? (
          <AvisoError className="mt-4" error={alojamiento.error} />
        ) : !alojamiento.data ? (
          <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-24" />
            ))}
          </div>
        ) : (
          <ul className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {alojamiento.data.serviciosAdicionales
              .filter((s) => s.activo)
              .map((s) => (
                <li key={s.id} className="flex flex-col gap-2 rounded-tarjeta border border-border bg-card p-5">
                  <div className="flex items-start justify-between gap-3">
                    <span className="flex items-center gap-2 font-semibold">
                      <Sparkles aria-hidden="true" className="size-4 shrink-0 text-coral" />
                      {s.nombre}
                    </span>
                    {s.generaCargo && s.valor > 0 ? (
                      <Dinero valor={s.valor} className="font-semibold text-coral-texto" />
                    ) : (
                      <span className="text-sm font-semibold text-muted-foreground">Incluido</span>
                    )}
                  </div>
                  {s.descripcion && <p className="text-sm text-muted-foreground">{s.descripcion}</p>}
                </li>
              ))}
          </ul>
        )}
      </section>

      <AntesDeReservar />
    </div>
  )
}

/** Tarjetas de los apartamentos con filtros rápidos por capacidad y la tarifa base */
function SeccionApartamentos() {
  const apartamentos = useTodosLosApartamentos()
  const { temporadas } = useTemporadasAlojamiento()
  const lista = useMemo(() => apartamentos.data ?? [], [apartamentos.data])
  const detalles = useDetallesApartamentos(lista.map((a) => a.identificacion))
  const [minimo, setMinimo] = useState(0)

  const idBase = temporadas.find((t) => t.tipo === 'BASE')?.id
  const tarifaBase = (id: string) => {
    const detalle = detalles[lista.findIndex((a) => a.identificacion === id)]?.data
    return detalle?.tarifas.find((t) => t.idTemporada === idBase)?.valorPorOcupante
  }

  const capacidades = [...new Set(lista.map((a) => a.capacidadMaxima))].sort((a, b) => a - b)
  const visibles = lista.filter((a) => a.capacidadMaxima >= minimo)

  return (
    <section aria-labelledby="titulo-apartamentos">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 id="titulo-apartamentos" className="font-titulo text-3xl font-semibold">
            Nuestros apartamentos
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">Elige el que mejor se acomode a tu grupo.</p>
        </div>
        {capacidades.length > 1 && (
          <div role="group" aria-label="Filtrar por capacidad" className="flex flex-wrap gap-2">
            {[0, ...capacidades].map((c) => (
              <button
                key={c}
                type="button"
                aria-pressed={minimo === c}
                onClick={() => setMinimo(c)}
                className={cn(
                  'min-h-11 rounded-full border px-4 text-sm font-medium transition-colors',
                  minimo === c
                    ? 'border-primary bg-primary text-primary-foreground'
                    : 'border-border bg-card hover:bg-muted',
                )}
              >
                {c === 0 ? 'Todos' : `${c}+ personas`}
              </button>
            ))}
          </div>
        )}
      </div>

      {apartamentos.error ? (
        <AvisoError className="mt-6" error={apartamentos.error} />
      ) : !apartamentos.data ? (
        <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-80" />
          ))}
        </div>
      ) : visibles.length === 0 ? (
        <Vacio icono={BedDouble} titulo="No hay apartamentos con esa capacidad" />
      ) : (
        <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {visibles.map((a) => {
            const base = tarifaBase(a.identificacion)
            return (
              <TarjetaApartamento
                key={a.identificacion}
                enlace={`/apartamentos/${encodeURIComponent(a.identificacion)}`}
                foto={a.imagenes[0]}
                identificacion={a.identificacion}
                nombre={a.nombre}
                capacidad={a.capacidadMaxima}
                dormitorios={a.dormitorios}
                precio={
                  base !== undefined ? (
                    <span className="flex flex-col">
                      <span className="text-xs text-muted-foreground">desde</span>
                      <span>
                        <Dinero valor={base} className="font-titulo text-xl font-semibold text-coral-texto" />{' '}
                        <span className="text-xs text-muted-foreground">por persona / noche</span>
                      </span>
                    </span>
                  ) : null
                }
              />
            )
          })}
        </div>
      )}
    </section>
  )
}

/** Política de cancelación, anticipo, depósito y normas */
function AntesDeReservar() {
  const politica = usePoliticaVigente()
  const { alojamiento, reglas } = useReglasAlojamiento()

  return (
    <section aria-labelledby="titulo-antes" className="rounded-tarjeta bg-crema p-6 md:p-10">
      <h2 id="titulo-antes" className="font-titulo text-3xl font-semibold">
        Antes de reservar
      </h2>
      <div className="mt-6 grid gap-5 md:grid-cols-2">
        <article className="rounded-tarjeta bg-card p-5">
          <h3 className="flex items-center gap-2 font-semibold">
            <ScrollText aria-hidden="true" className="size-5 text-coral" /> Política de cancelación
          </h3>
          {politica.error ? (
            <AvisoError className="mt-3" error={politica.error} />
          ) : !politica.data ? (
            <Skeleton className="mt-3 h-20" />
          ) : (
            <>
              <ListaPolitica className="mt-3" politica={politica.data} />
              {politica.data.consecuenciaNoShow && (
                <p className="mt-3 text-xs text-muted-foreground">
                  Si no te presentas: {politica.data.consecuenciaNoShow}
                </p>
              )}
            </>
          )}
        </article>
        <article className="rounded-tarjeta bg-card p-5">
          <h3 className="flex items-center gap-2 font-semibold">
            <HandCoins aria-hidden="true" className="size-5 text-coral" /> Anticipo y depósito
          </h3>
          <ul className="mt-3 flex flex-col gap-2 text-sm">
            <li>
              Para confirmar, pagas <strong>{textoPorcentajeAnticipo(reglas.porcentajeAnticipo)}</strong> de la estancia
              por transferencia {textoPlazo(reglas.plazoConfirmacionHoras)}.
            </li>
            <li>
              Al reservar se suma un depósito de garantía reembolsable
              {reglas.depositoGarantia !== null && (
                <>
                  {' '}
                  de <Dinero valor={reglas.depositoGarantia} className="font-semibold" />
                </>
              )}
              , que se te devuelve al salir.
            </li>
            <li>
              {reglas.horaNocturna ? conHora('Si llegas desde', reglas.horaNocturna) : 'Si llegas en horario nocturno'},{' '}
              <TextoCargo tipo="nocturna" valor={reglas.recargoNocturno} minuscula />.
            </li>
          </ul>
        </article>
        <article className="rounded-tarjeta bg-card p-5 md:col-span-2">
          <h3 className="flex items-center gap-2 font-semibold">
            <ShieldCheck aria-hidden="true" className="size-5 text-coral" /> Normas de la casa
          </h3>
          <ul className="mt-3 grid gap-2 text-sm md:grid-cols-2">
            <li>
              {conPunto(
                `${conHora('Entrada desde', reglas.horaEntrada)} y ${conHora('salida hasta', reglas.horaSalida)}${
                  !reglas.horaEntrada || !reglas.horaSalida ? ' según el horario del alojamiento' : ''
                }`,
              )}
            </li>
            <li>
              {reglas.aceptaMascotas === false ? (
                'No se aceptan mascotas.'
              ) : (
                <>
                  {textoMascotas(reglas)}. <TextoCargo tipo="aseo" valor={reglas.cargoAseoMascota} />, una vez por estancia.
                </>
              )}
            </li>
            {alojamiento.data?.normaConvivencia && <li className="md:col-span-2">{alojamiento.data.normaConvivencia}</li>}
          </ul>
        </article>
      </div>
    </section>
  )
}
