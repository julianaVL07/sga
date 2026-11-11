import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { CalendarSearch, SearchX } from 'lucide-react'
import { useApartamentosDisponibles } from '@/api/publico'
import type { FiltrosDisponibles } from '@/api/tipos'
import { AvisoError, Campo, Dinero, Fecha, Paginador, Vacio } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { BuscadorEstancia } from './comun/BuscadorEstancia'
import { useReglasAlojamiento, useTemporadasAlojamiento } from './comun/consultas'
import { EncabezadoFiltros } from './comun/EncabezadoFiltros'
import { claseContenidoPlegable } from './comun/estilos'
import { estanciaAParams, fechasCompletas, leerEstancia, resumenHuespedes } from './comun/estancia'
import { avisoEstanciaMinima, nochesEntre } from './comun/reglas'
import { TarjetaApartamento } from './comun/TarjetaApartamento'

const FILTROS = ['capacidadMinima', 'caracteristica', 'precioMinimo', 'precioMaximo'] as const
type Filtro = (typeof FILTROS)[number]

const aNumero = (texto: string | null) => {
  const limpio = (texto ?? '').replace(/[$.\s]/g, '')
  return limpio === '' || Number.isNaN(Number(limpio)) ? undefined : Number(limpio)
}

export default function Buscar() {
  const [params, setParams] = useSearchParams()
  const { reglas } = useReglasAlojamiento()
  const { temporadas } = useTemporadasAlojamiento()
  const estancia = leerEstancia(params)
  const pagina = Number(params.get('pagina') ?? 0) || 0
  const noches = nochesEntre(estancia.entrada, estancia.salida)

  const avisoRP01 = avisoEstanciaMinima(estancia.entrada, estancia.salida, temporadas, reglas)

  // La URL es la fuente de verdad; React Query compara los filtros por valor
  const filtros: FiltrosDisponibles | null =
    noches < 1 || avisoRP01
      ? null
      : {
          entrada: estancia.entrada,
          salida: estancia.salida,
          ocupantes: estancia.huespedes.length,
          fechasNacimiento: fechasCompletas(estancia.huespedes),
          capacidadMinima: aNumero(params.get('capacidadMinima')),
          caracteristica: params.get('caracteristica')?.trim() || undefined,
          precioMinimo: aNumero(params.get('precioMinimo')),
          precioMaximo: aNumero(params.get('precioMaximo')),
        }

  const consulta = useApartamentosDisponibles(filtros, pagina)

  const filtrosActuales = Object.fromEntries(FILTROS.map((f) => [f, params.get(f) ?? ''])) as Record<Filtro, string>

  const cambiarPagina = (nueva: number) => {
    const siguientes = new URLSearchParams(params)
    siguientes.set('pagina', String(nueva))
    setParams(siguientes)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const enlaceDetalle = (id: string) =>
    `/apartamentos/${encodeURIComponent(id)}?${estanciaAParams(estancia).toString()}`

  return (
    <section className="flex flex-col gap-6">
      <div>
        <h1 className="font-titulo text-3xl font-semibold md:text-4xl">Apartamentos disponibles</h1>
        {noches > 0 && (
          <p className="mt-1 text-sm text-muted-foreground">
            <Fecha valor={estancia.entrada} /> → <Fecha valor={estancia.salida} /> · {noches}{' '}
            {noches === 1 ? 'noche' : 'noches'} ·{' '}
            <span className="whitespace-nowrap">
              {resumenHuespedes(estancia.huespedes, reglas.umbralEdad, estancia.entrada)}
            </span>
          </p>
        )}
      </div>

      <BuscadorEstancia
        key={`${estancia.entrada}|${estancia.salida}|${estancia.huespedes.join(',')}`}
        inicial={estancia}
        umbralEdad={reglas.umbralEdad}
        textoBoton="Actualizar"
        alBuscar={(nueva) => setParams(estanciaAParams(nueva, filtrosActuales))}
      />

      <div className="grid gap-6 lg:grid-cols-[17rem_1fr] lg:items-start">
        <FiltrosBusqueda
          key={params.toString()}
          valores={filtrosActuales}
          alAplicar={(valores) => setParams(estanciaAParams(estancia, valores))}
        />

        <div className="flex flex-col gap-4">
          {avisoRP01 && <AvisoError titulo="Estancia mínima de fin de semana" mensaje={avisoRP01} />}

          {noches < 1 ? (
            <div className="rounded-tarjeta border border-border bg-card">
              <Vacio
                icono={CalendarSearch}
                titulo="Elige tus fechas"
                descripcion="Indica la llegada, la salida y los huéspedes para ver los apartamentos libres y el valor de tu estancia."
              />
            </div>
          ) : avisoRP01 ? null : consulta.error ? (
            <AvisoError error={consulta.error} />
          ) : !consulta.data ? (
            <div className="grid gap-5 sm:grid-cols-2">
              {[0, 1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-80" />
              ))}
            </div>
          ) : consulta.data.contenido.length === 0 ? (
            <div className="rounded-tarjeta border border-border bg-card">
              <Vacio
                icono={SearchX}
                titulo="No hay apartamentos libres"
                descripcion="Prueba con otras fechas o quita algunos filtros."
              />
            </div>
          ) : (
            <>
              <p className="text-sm text-muted-foreground" aria-live="polite">
                {consulta.data.totalElementos}{' '}
                {consulta.data.totalElementos === 1 ? 'apartamento disponible' : 'apartamentos disponibles'}
              </p>
              <div className="grid gap-5 sm:grid-cols-2">
                {consulta.data.contenido.map((a) => (
                  <TarjetaApartamento
                    key={a.identificacion}
                    enlace={enlaceDetalle(a.identificacion)}
                    foto={a.imagenPrincipal}
                    identificacion={a.identificacion}
                    nombre={a.nombre}
                    capacidad={a.capacidadMaxima}
                    precio={
                      <span className="flex flex-col">
                        <span className="text-xs text-muted-foreground">Valor estimado de la estancia</span>
                        <Dinero valor={a.valorEstimado} className="font-titulo text-xl font-semibold text-coral-texto" />
                      </span>
                    }
                  />
                ))}
              </div>
              <Paginador
                pagina={pagina}
                totalPaginas={consulta.data.totalPaginas}
                totalElementos={consulta.data.totalElementos}
                alCambiar={cambiarPagina}
              />
            </>
          )}
        </div>
      </div>
    </section>
  )
}

interface PropsFiltros {
  valores: Record<Filtro, string>
  alAplicar: (valores: Record<Filtro, string>) => void
}

/** Capacidad mínima, característica de la dotación y rango del valor estimado */
function FiltrosBusqueda({ valores, alAplicar }: PropsFiltros) {
  const [borrador, setBorrador] = useState(valores)
  const [error, setError] = useState<string>()
  // En el celular los filtros empiezan plegados para que los resultados queden a la vista
  const [abierto, setAbierto] = useState(false)

  const cambiar = (campo: Filtro, valor: string) => setBorrador((b) => ({ ...b, [campo]: valor }))

  const aplicar = (e: FormEvent) => {
    e.preventDefault()
    const min = aNumero(borrador.precioMinimo)
    const max = aNumero(borrador.precioMaximo)
    if (min !== undefined && max !== undefined && min > max) {
      setError('El precio mínimo no puede ser mayor que el máximo.')
      return
    }
    setError(undefined)
    alAplicar(borrador)
  }

  const limpiar = () => {
    const vacios = { capacidadMinima: '', caracteristica: '', precioMinimo: '', precioMaximo: '' }
    setBorrador(vacios)
    setError(undefined)
    alAplicar(vacios)
  }

  return (
    <form
      noValidate
      onSubmit={aplicar}
      aria-label="Filtros"
      className="flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta lg:p-5"
    >
      <EncabezadoFiltros
        id="boton-filtros"
        controla="contenido-filtros"
        abierto={abierto}
        alCambiar={setAbierto}
        activos={Object.values(valores).filter((v) => v.trim() !== '').length}
        desde="lg"
      />
      <div id="contenido-filtros" className={claseContenidoPlegable(abierto, 'lg')}>
      <Campo id="filtro-capacidad" etiqueta="Capacidad mínima">
        <Input
          type="number"
          inputMode="numeric"
          min={1}
          placeholder="Ej.: 4"
          value={borrador.capacidadMinima}
          onChange={(e) => cambiar('capacidadMinima', e.target.value)}
        />
      </Campo>
      <Campo id="filtro-caracteristica" etiqueta="Característica" ayuda="Busca en la dotación, por ejemplo «hidromasaje».">
        <Input
          placeholder="Hidromasaje, balcón…"
          value={borrador.caracteristica}
          onChange={(e) => cambiar('caracteristica', e.target.value)}
        />
      </Campo>
      <fieldset className="flex flex-col gap-2">
        <legend className="mb-1.5 text-sm font-medium">Valor de la estancia</legend>
        <div className="grid grid-cols-2 gap-2">
          <Campo id="filtro-precio-min" etiqueta="Desde">
            <Input
              inputMode="numeric"
              placeholder="$0"
              value={borrador.precioMinimo}
              onChange={(e) => cambiar('precioMinimo', e.target.value)}
            />
          </Campo>
          <Campo id="filtro-precio-max" etiqueta="Hasta">
            <Input
              inputMode="numeric"
              placeholder="Sin límite"
              value={borrador.precioMaximo}
              onChange={(e) => cambiar('precioMaximo', e.target.value)}
            />
          </Campo>
        </div>
        {error && <p className="text-xs font-medium text-error">{error}</p>}
      </fieldset>
      <div className="flex gap-2">
        <Button type="submit" className="flex-1">
          Aplicar
        </Button>
        <Button type="button" variant="ghost" onClick={limpiar}>
          Limpiar
        </Button>
      </div>
      </div>
    </form>
  )
}
