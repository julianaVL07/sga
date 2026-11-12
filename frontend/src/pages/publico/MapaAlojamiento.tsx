import { Component, useEffect, useRef, useState, type ReactNode } from 'react'
import { CircleMarker, MapContainer, TileLayer, Tooltip } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { ExternalLink, MapPin, MapPinOff } from 'lucide-react'
import { useAlojamiento } from '@/api/publico'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

/*
 * Mapa del alojamiento con Leaflet y los mapas de OpenStreetMap, con la latitud y la longitud de GET /api/alojamiento.
 * Si el mapa no carga (sin internet, el servidor de mapas no responde o Leaflet falla), se muestra la dirección con
 * las coordenadas, sin errores en la pantalla.
 */

/** Si en este tiempo no llegó ninguna parte del mapa, se muestra la dirección */
const ESPERA_MAXIMA_MS = 8000
const ZOOM = 16
const MOSAICOS_OSM = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const ATRIBUCION_OSM = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
// Los colores de la marca (--coral-primario y --coral-coral); Leaflet los necesita como valores, no como clases
const COLOR_BORDE = '#8e3b6f'
const COLOR_RELLENO = '#e75480'

interface Props {
  /** detalle: sección "Ubicación" del apartamento · franja: banda horizontal de la búsqueda */
  variante?: 'detalle' | 'franja'
  className?: string
}

export function MapaAlojamiento({ variante = 'detalle', className }: Props) {
  const consulta = useAlojamiento()
  const esFranja = variante === 'franja'

  // Sin datos del alojamiento no hay nada que ubicar: no se muestra la sección
  if (consulta.isError) return null
  if (!consulta.data) {
    return <Skeleton className={cn(esFranja ? 'h-44' : 'h-80', className)} />
  }

  const { nombre, direccion, ciudad, latitud, longitud } = consulta.data
  const coordenadasValidas =
    Number.isFinite(latitud) && Number.isFinite(longitud) && Math.abs(latitud) <= 90 && Math.abs(longitud) <= 180
  const ubicacion = { nombre, direccion, ciudad, latitud, longitud, coordenadasValidas }

  const mapa = coordenadasValidas ? (
    <MapaConRespaldo {...ubicacion} className={esFranja ? 'h-44 md:h-full md:min-h-44' : 'h-72 md:h-80'} />
  ) : (
    <SinMapa {...ubicacion} className={esFranja ? 'h-44 md:h-full' : 'h-40'} />
  )

  if (esFranja) {
    return (
      <section
        aria-labelledby="titulo-ubicacion"
        className={cn(
          'grid overflow-hidden rounded-tarjeta border border-border bg-card shadow-tarjeta md:grid-cols-[minmax(16rem,1fr)_2fr]',
          className,
        )}
      >
        <div className="flex flex-col gap-2 p-4 lg:p-5">
          <h2 id="titulo-ubicacion" className="inline-flex items-center gap-2 font-titulo text-xl font-semibold">
            <MapPin aria-hidden="true" className="size-5 text-coral" /> Dónde queda
          </h2>
          <DatosUbicacion {...ubicacion} />
        </div>
        {mapa}
      </section>
    )
  }

  return (
    <section aria-labelledby="titulo-ubicacion" className={cn('flex flex-col gap-3', className)}>
      <h2 id="titulo-ubicacion" className="font-titulo text-2xl font-semibold">
        Ubicación
      </h2>
      <DatosUbicacion {...ubicacion} />
      <div className="overflow-hidden rounded-tarjeta border border-border">{mapa}</div>
    </section>
  )
}

interface Ubicacion {
  nombre: string
  direccion: string
  ciudad: string
  latitud: number
  longitud: number
  coordenadasValidas: boolean
}

function textoCoordenadas(latitud: number, longitud: number) {
  return `${latitud.toFixed(5)}, ${longitud.toFixed(5)}`
}

function enlaceOsm(latitud: number, longitud: number) {
  return `https://www.openstreetmap.org/?mlat=${latitud}&mlon=${longitud}#map=${ZOOM}/${latitud}/${longitud}`
}

/** Dirección, ciudad, coordenadas y enlace a OpenStreetMap */
function DatosUbicacion({ nombre, direccion, ciudad, latitud, longitud, coordenadasValidas }: Ubicacion) {
  return (
    <div className="flex flex-col gap-1 text-sm text-muted-foreground">
      <p>
        <span className="font-semibold text-foreground">{nombre}</span>
        {direccion && <> · {direccion}</>}
        {ciudad && <>, {ciudad}</>}
      </p>
      {coordenadasValidas && (
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <span className="font-mono text-xs">{textoCoordenadas(latitud, longitud)}</span>
          <a
            href={enlaceOsm(latitud, longitud)}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1 font-medium text-primary underline-offset-4 hover:underline"
          >
            Abrir en OpenStreetMap <ExternalLink aria-hidden="true" className="size-3.5" />
            <span className="sr-only">(se abre en otra pestaña)</span>
          </a>
        </p>
      )}
    </div>
  )
}

/** Lo que se ve en lugar del mapa: la dirección con las coordenadas */
function SinMapa({ direccion, ciudad, latitud, longitud, coordenadasValidas, className }: Ubicacion & { className?: string }) {
  return (
    <div
      role="note"
      className={cn('flex flex-col items-center justify-center gap-1 bg-crema/60 p-4 text-center text-sm', className)}
    >
      <MapPinOff aria-hidden="true" className="size-6 text-coral" />
      <p className="font-medium text-foreground">El mapa no está disponible en este momento</p>
      <p className="text-muted-foreground">
        {[direccion, ciudad].filter(Boolean).join(', ')}
        {coordenadasValidas && (
          <>
            <br />
            <span className="font-mono text-xs">{textoCoordenadas(latitud, longitud)}</span>
          </>
        )}
      </p>
    </div>
  )
}

type EstadoMapa = 'cargando' | 'listo' | 'sin-mapa'

/**
 * Mapa de Leaflet que cae a la dirección si: el navegador ya sabe que no hay conexión, ninguna parte del mapa carga
 * (la primera que falla sin que otra haya cargado basta), pasan ESPERA_MAXIMA_MS sin que cargue nada o Leaflet lanza
 * un error al pintar.
 */
function MapaConRespaldo(props: Ubicacion & { className?: string }) {
  const { latitud, longitud, nombre, className } = props
  const [estado, setEstado] = useState<EstadoMapa>(() =>
    typeof navigator !== 'undefined' && navigator.onLine === false ? 'sin-mapa' : 'cargando',
  )
  const cargoAlguna = useRef(false)

  useEffect(() => {
    if (estado !== 'cargando') return
    const espera = window.setTimeout(() => {
      if (!cargoAlguna.current) setEstado('sin-mapa')
    }, ESPERA_MAXIMA_MS)
    return () => window.clearTimeout(espera)
  }, [estado])

  if (estado === 'sin-mapa') return <SinMapa {...props} />

  return (
    <ErrorDelMapa alFallar={() => setEstado('sin-mapa')}>
      <div className={cn('relative isolate', className)}>
        <MapContainer
          center={[latitud, longitud]}
          zoom={ZOOM}
          scrollWheelZoom={false}
          className="size-full"
          aria-label={`Mapa con la ubicación de ${nombre}`}
        >
          <TileLayer
            url={MOSAICOS_OSM}
            attribution={ATRIBUCION_OSM}
            eventHandlers={{
              tileload: () => {
                if (!cargoAlguna.current) {
                  cargoAlguna.current = true
                  setEstado('listo')
                }
              },
              tileerror: () => {
                if (!cargoAlguna.current) setEstado('sin-mapa')
              },
            }}
          />
          <CircleMarker
            center={[latitud, longitud]}
            radius={11}
            pathOptions={{ color: COLOR_BORDE, weight: 3, fillColor: COLOR_RELLENO, fillOpacity: 0.85 }}
          >
            <Tooltip direction="top" offset={[0, -10]}>
              {nombre}
            </Tooltip>
          </CircleMarker>
        </MapContainer>
        {estado === 'cargando' && (
          <div className="pointer-events-none absolute inset-0 z-[500] flex items-center justify-center bg-card/70 text-sm text-muted-foreground">
            Cargando mapa…
          </div>
        )}
      </div>
    </ErrorDelMapa>
  )
}

/** Si Leaflet falla al pintar, avisa para mostrar la dirección en lugar de romper la página */
class ErrorDelMapa extends Component<{ alFallar: () => void; children: ReactNode }, { fallo: boolean }> {
  state = { fallo: false }

  static getDerivedStateFromError() {
    return { fallo: true }
  }

  componentDidCatch() {
    this.props.alFallar()
  }

  render() {
    return this.state.fallo ? null : this.props.children
  }
}
