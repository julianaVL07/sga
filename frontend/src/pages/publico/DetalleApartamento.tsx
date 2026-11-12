import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { ArrowLeft, BedDouble, CalendarCheck, Clock, Dog, ShieldCheck, Users } from 'lucide-react'
import { useApartamento } from '@/api/publico'
import { useAuth } from '@/auth/AuthContext'
import { ROLES_PANEL, tieneAlgunRol } from '@/auth/roles'
import { AvisoError, Campo, Dinero, Fecha, FotoApartamento, Vacio } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { useCotizacion, useReglasAlojamiento, useTemporadasAlojamiento } from './comun/consultas'
import { DesgloseCotizacion } from './comun/DesgloseCotizacion'
import { estanciaAParams, leerEstancia, ocupantesParaCotizar } from './comun/estancia'
import { iconoDotacion } from './comun/iconos'
import { MapaAlojamiento } from './MapaAlojamiento'
import { PildoraTemporada } from './comun/PildoraTemporada'
import {
  avisoEstanciaMinima,
  conHora,
  conPunto,
  hoyISO,
  nochesEntre,
  sumarDias,
  textoMascotas,
  textoMenores,
} from './comun/reglas'
import { SelectorHuespedes } from './comun/SelectorHuespedes'
import { TextoCargo } from './comun/TextoCargo'

export default function DetalleApartamento() {
  const { id } = useParams()
  const consulta = useApartamento(id)
  const { alojamiento, reglas } = useReglasAlojamiento()
  const { temporadas } = useTemporadasAlojamiento()

  if (consulta.error) {
    return (
      <div className="flex flex-col gap-4">
        <AvisoError error={consulta.error} />
        <Button variant="outline" asChild className="w-fit">
          <Link to="/buscar">
            <ArrowLeft aria-hidden="true" /> Ver apartamentos
          </Link>
        </Button>
      </div>
    )
  }

  const apto = consulta.data
  if (!apto) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  // Foto principal primero; se completan cinco espacios
  const fotos = [
    ...apto.imagenes.filter((i) => i.principal).map((i) => i.url),
    ...apto.imagenes.filter((i) => !i.principal).map((i) => i.url),
  ]
  const galeria = Array.from({ length: 5 }, (_, i) => fotos[i] ?? null)

  const tarifas = apto.tarifas
    .map((t) => ({ ...t, temporada: temporadas.find((x) => x.id === t.idTemporada) }))
    .sort((a, b) => orden(a.temporada?.tipo) - orden(b.temporada?.tipo))

  return (
    <article className="flex flex-col gap-8">
      <header className="flex flex-col gap-2">
        <Link to="/buscar" className="inline-flex w-fit items-center gap-1 text-sm text-muted-foreground hover:underline">
          <ArrowLeft aria-hidden="true" className="size-4" /> Apartamentos
        </Link>
        <p className="text-xs font-semibold tracking-[0.2em] text-coral-texto uppercase">{apto.identificacion}</p>
        <h1 className="font-titulo text-3xl font-semibold md:text-5xl">{apto.nombre}</h1>
        <p className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-1.5">
            <Users aria-hidden="true" className="size-4 text-coral" /> Hasta {apto.capacidadMaxima} personas
          </span>
          <span className="inline-flex items-center gap-1.5">
            <BedDouble aria-hidden="true" className="size-4 text-coral" /> {apto.dormitorios}{' '}
            {apto.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}
          </span>
        </p>
      </header>

      {/* ---------- Galería: una grande y cuatro pequeñas ---------- */}
      <div className="grid gap-2 overflow-hidden rounded-tarjeta md:h-[28rem] md:grid-cols-4 md:grid-rows-2">
        {galeria.map((src, i) => (
          <FotoApartamento
            key={i}
            src={src}
            alt={i === 0 ? `Foto principal de ${apto.nombre}` : `Foto ${i + 1} de ${apto.nombre}`}
            className={cn(
              'size-full',
              i === 0 ? 'aspect-[4/3] md:col-span-2 md:row-span-2 md:aspect-auto' : 'hidden md:flex',
            )}
          />
        ))}
      </div>

      <div className="grid gap-8 lg:grid-cols-[1fr_24rem] lg:items-start">
        <div className="flex flex-col gap-10">
          {apto.descripcion && (
            <section aria-labelledby="titulo-descripcion">
              <h2 id="titulo-descripcion" className="font-titulo text-2xl font-semibold">
                Sobre el apartamento
              </h2>
              <p className="mt-3 leading-relaxed whitespace-pre-line text-muted-foreground">{apto.descripcion}</p>
            </section>
          )}

          {apto.dotacion.length > 0 && (
            <section aria-labelledby="titulo-dotacion">
              <h2 id="titulo-dotacion" className="font-titulo text-2xl font-semibold">
                Dotación
              </h2>
              <ul className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
                {apto.dotacion.map((d) => {
                  const Icono = iconoDotacion(d)
                  return (
                    <li key={d} className="flex items-center gap-3 text-sm">
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-crema">
                        <Icono aria-hidden="true" className="size-4 text-coral" />
                      </span>
                      {d}
                    </li>
                  )
                })}
              </ul>
            </section>
          )}

          <section aria-labelledby="titulo-tarifas">
            <h2 id="titulo-tarifas" className="font-titulo text-2xl font-semibold">
              Tarifas por temporada
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Valor por persona y por noche. {textoMenores(reglas.umbralEdad)}.
            </p>
            <div className="mt-4 overflow-hidden rounded-tabla border border-border">
              <table className="w-full text-sm">
                <caption className="sr-only">Tarifas del apartamento por temporada</caption>
                <thead className="bg-muted/60 text-left text-xs tracking-wide text-muted-foreground uppercase">
                  <tr>
                    <th scope="col" className="px-4 py-3 font-semibold">
                      Temporada
                    </th>
                    <th scope="col" className="hidden px-4 py-3 font-semibold sm:table-cell">
                      Fechas
                    </th>
                    <th scope="col" className="px-4 py-3 text-right font-semibold">
                      Por persona / noche
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border bg-card">
                  {tarifas.map((t) => (
                    <tr key={t.idTemporada}>
                      <td className="px-4 py-3 align-top">
                        <PildoraTemporada tipo={t.temporada?.tipo ?? null} nombre={t.temporada?.nombre ?? t.idTemporada} />
                        <div className="mt-1.5 text-xs text-muted-foreground sm:hidden">
                          <RangosTemporada temporada={t.temporada} />
                        </div>
                      </td>
                      <td className="hidden px-4 py-3 align-top text-muted-foreground sm:table-cell">
                        <RangosTemporada temporada={t.temporada} />
                      </td>
                      <td className="px-4 py-3 text-right align-top">
                        <Dinero valor={t.valorPorOcupante} className="font-semibold" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section aria-labelledby="titulo-normas" className="grid gap-4 sm:grid-cols-3">
            <h2 id="titulo-normas" className="sr-only">
              Horarios y normas
            </h2>
            <div className="rounded-tarjeta border border-border bg-card p-4">
              <Clock aria-hidden="true" className="size-5 text-coral" />
              <p className="mt-2 text-sm font-semibold">Horarios</p>
              <p className="text-sm text-muted-foreground">
                {conPunto(conHora('Entrada desde', reglas.horaEntrada))}{' '}
                {conPunto(conHora('Salida hasta', reglas.horaSalida))}
                {(!reglas.horaEntrada || !reglas.horaSalida) && ' Consulta el horario con recepción.'}
              </p>
            </div>
            <div className="rounded-tarjeta border border-border bg-card p-4">
              <Dog aria-hidden="true" className="size-5 text-coral" />
              <p className="mt-2 text-sm font-semibold">Mascotas</p>
              <p className="text-sm text-muted-foreground">
                {reglas.aceptaMascotas === false ? (
                  'No se aceptan mascotas.'
                ) : (
                  <>
                    {textoMascotas(reglas)}. <TextoCargo tipo="aseo" valor={reglas.cargoAseoMascota} />, una vez por
                    estancia.
                  </>
                )}
              </p>
            </div>
            <div className="rounded-tarjeta border border-border bg-card p-4">
              <ShieldCheck aria-hidden="true" className="size-5 text-coral" />
              <p className="mt-2 text-sm font-semibold">Normas</p>
              <p className="text-sm text-muted-foreground">
                {alojamiento.data?.normaConvivencia || 'Respeta el descanso de los demás huéspedes.'}
              </p>
            </div>
          </section>

          <MapaAlojamiento />
        </div>

        <TarjetaCotizar identificacion={apto.identificacion} capacidad={apto.capacidadMaxima} />
      </div>
    </article>
  )
}

const orden = (tipo: string | undefined) => (tipo === 'BASE' ? 0 : tipo === 'MEDIA' ? 1 : tipo === 'ALTA' ? 2 : 3)

function RangosTemporada({ temporada }: { temporada: { tipo: string; rangos: { fechaInicio: string; fechaFin: string }[] } | undefined }) {
  if (!temporada) return null
  if (temporada.tipo === 'BASE' || temporada.rangos.length === 0) return <span>El resto del año</span>
  return (
    <ul className="flex flex-col gap-0.5">
      {temporada.rangos.map((r) => (
        <li key={r.fechaInicio} className="whitespace-nowrap">
          <Fecha valor={r.fechaInicio} patron="d MMM yyyy" /> – <Fecha valor={r.fechaFin} patron="d MMM yyyy" />
        </li>
      ))}
    </ul>
  )
}

/** Tarjeta fija con las fechas, los huéspedes y la cotización noche por noche */
function TarjetaCotizar({ identificacion, capacidad }: { identificacion: string; capacidad: number }) {
  const [params] = useSearchParams()
  const inicial = leerEstancia(params)
  const [entrada, setEntrada] = useState(inicial.entrada)
  const [salida, setSalida] = useState(inicial.salida)
  const [huespedes, setHuespedes] = useState(inicial.huespedes)
  const { roles, autenticado } = useAuth()
  const { reglas } = useReglasAlojamiento()
  const { temporadas } = useTemporadasAlojamiento()

  const noches = nochesEntre(entrada, salida)
  const avisoRP01 = avisoEstanciaMinima(entrada, salida, temporadas, reglas)
  const sobreCapacidad = huespedes.length > capacidad
  const listo = noches > 0 && !avisoRP01 && !sobreCapacidad

  const cotizacion = useCotizacion(
    listo
      ? {
          identificacionApartamento: identificacion,
          fechaEntrada: entrada,
          fechaSalida: salida,
          ocupantes: ocupantesParaCotizar(
            huespedes.map((f) => ({ fechaNacimiento: f })),
            reglas.umbralEdad,
            entrada,
          ),
        }
      : null,
  )

  const consulta = estanciaAParams({ entrada, salida, huespedes }).toString()
  const esPanel = tieneAlgunRol(roles, ROLES_PANEL)
  const esServicio = autenticado && !esPanel && !roles.includes('HUESPED')
  const destino = esPanel
    ? `/panel/reservas/nueva?apartamento=${encodeURIComponent(identificacion)}&${consulta}`
    : `/reservar/${encodeURIComponent(identificacion)}?${consulta}`

  return (
    <aside
      aria-labelledby="titulo-cotizar"
      className="order-first flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta lg:sticky lg:top-24 lg:order-none"
    >
      <h2 id="titulo-cotizar" className="font-titulo text-2xl font-semibold">
        Cotiza tu estancia
      </h2>
      <div className="grid grid-cols-2 gap-3">
        <Campo id="cotizar-entrada" etiqueta="Llegada">
          <Input
            type="date"
            min={hoyISO()}
            value={entrada}
            onChange={(e) => {
              const valor = e.target.value
              setEntrada(valor)
              if (valor && (!salida || salida <= valor)) setSalida(sumarDias(valor, 1))
            }}
          />
        </Campo>
        <Campo id="cotizar-salida" etiqueta="Salida">
          <Input
            type="date"
            min={entrada ? sumarDias(entrada, 1) : hoyISO()}
            value={salida}
            onChange={(e) => setSalida(e.target.value)}
          />
        </Campo>
      </div>
      <div className="flex flex-col gap-1.5">
        <span className="text-sm font-medium">Huéspedes</span>
        <SelectorHuespedes
          desplegable
          huespedes={huespedes}
          alCambiar={setHuespedes}
          umbralEdad={reglas.umbralEdad}
          referencia={entrada}
          maximo={capacidad}
        />
      </div>

      {sobreCapacidad && <AvisoError mensaje={`Este apartamento recibe máximo ${capacidad} personas.`} />}
      {avisoRP01 && <AvisoError titulo="Estancia mínima" mensaje={avisoRP01} />}

      {noches < 1 ? (
        <Vacio
          icono={CalendarCheck}
          titulo="Elige tus fechas"
          descripcion="Te mostramos el valor noche por noche."
          className="py-6"
        />
      ) : !listo ? null : cotizacion.error ? (
        <AvisoError error={cotizacion.error} />
      ) : !cotizacion.data ? (
        <Skeleton className="h-40" />
      ) : (
        <DesgloseCotizacion
          cotizacion={cotizacion.data}
          temporadas={temporadas}
          className={cn(cotizacion.isFetching && 'opacity-60')}
        />
      )}

      {esServicio ? (
        <p className="text-sm text-muted-foreground">Las reservas se hacen desde el portal o en recepción.</p>
      ) : (
        <Button asChild size="lg" className={cn(!listo && 'pointer-events-none opacity-50')}>
          <Link to={destino} aria-disabled={!listo} tabIndex={listo ? undefined : -1}>
            Reservar
          </Link>
        </Button>
      )}
      <p className="text-center text-xs text-muted-foreground">Aún no se te cobra nada.</p>
    </aside>
  )
}
