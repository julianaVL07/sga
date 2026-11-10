import { useEffect, useState } from 'react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, ImagePlus, Loader2, Trash2, TriangleAlert } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import {
  useActualizarApartamento,
  useRegistrarApartamento,
  useTodasLasTemporadas,
  type ActualizacionApartamentoResponse,
  type TarifaRequest,
} from '@/api/apartamentos'
import { esErrorApi } from '@/api/cliente'
import { useApartamento } from '@/api/publico'
import { AvisoError, Campo, FotoApartamento } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { claseTextarea } from '../calendario/estilos'
import { aPesos } from './utilidades'

const MAXIMO_IMAGENES = 10

const entero = (mensaje: string) =>
  z.string().trim().refine((v) => /^\d+$/.test(v) && Number(v) >= 1, mensaje)

const esquema = z.object({
  identificacion: z.string().trim(),
  nombre: z.string().trim().min(1, 'Escribe el nombre'),
  descripcion: z.string(),
  dormitorios: entero('Al menos 1 dormitorio'),
  capacidadMaxima: entero('Al menos 1 persona'),
  dotacion: z.string().refine((v) => lineas(v).length > 0, 'Escribe al menos un elemento'),
  imagenes: z.array(
    z.object({
      url: z.string().trim().regex(/^https?:\/\/\S+$/, 'Debe empezar por http:// o https://'),
    }),
  ),
  principal: z.string(),
  tarifas: z.record(z.string(), z.string()),
})

type Datos = z.infer<typeof esquema>

function lineas(texto: string): string[] {
  return texto
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean)
}

const VACIO: Datos = {
  identificacion: '',
  nombre: '',
  descripcion: '',
  dormitorios: '1',
  capacidadMaxima: '2',
  dotacion: '',
  imagenes: [{ url: '' }],
  principal: '0',
  tarifas: {},
}

export default function FormularioApartamento() {
  const { id } = useParams()
  const editando = !!id
  const navigate = useNavigate()
  const existente = useApartamento(id)
  const temporadas = useTodasLasTemporadas()
  const registrar = useRegistrarApartamento()
  const actualizar = useActualizarApartamento(id ?? '')
  const mutacion = editando ? actualizar : registrar
  const [resultado, setResultado] = useState<ActualizacionApartamentoResponse | null>(null)

  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    setValue,
    formState: { errors },
  } = useForm<Datos>({ resolver: zodResolver(esquema), defaultValues: VACIO })
  const imagenes = useFieldArray({ control, name: 'imagenes' })
  const principal = useWatch({ control, name: 'principal' })
  const urls = useWatch({ control, name: 'imagenes' })

  // Al editar, el formulario arranca con los datos actuales
  const datosActuales = existente.data
  useEffect(() => {
    if (!datosActuales) return
    reset({
      ...VACIO,
      identificacion: datosActuales.identificacion,
      nombre: datosActuales.nombre,
      descripcion: datosActuales.descripcion ?? '',
      dormitorios: String(datosActuales.dormitorios),
      capacidadMaxima: String(datosActuales.capacidadMaxima),
      dotacion: datosActuales.dotacion.join('\n'),
      // Las imágenes no se editan aquí
      imagenes: [],
    })
  }, [datosActuales, reset])

  const enviar = (datos: Datos) => {
    const comunes = {
      nombre: datos.nombre.trim(),
      descripcion: datos.descripcion.trim() || null,
      dormitorios: Number(datos.dormitorios),
      capacidadMaxima: Number(datos.capacidadMaxima),
      dotacion: lineas(datos.dotacion),
    }

    if (editando) {
      actualizar.mutate(comunes, {
        onSuccess: (respuesta) => {
          if (respuesta.reservasSobreCapacidad.length > 0) {
            setResultado(respuesta)
            window.scrollTo({ top: 0, behavior: 'smooth' })
          } else {
            toast.success('Apartamento actualizado', { description: respuesta.apartamento.identificacion })
            navigate(`/panel/apartamentos/${encodeURIComponent(respuesta.apartamento.identificacion)}`)
          }
        },
        onError: (error) => marcarErrores(error),
      })
      return
    }

    // Registro: validaciones propias del alta
    let valido = true
    if (!datos.identificacion.trim()) {
      setError('identificacion', { message: 'Escribe la identificación, por ejemplo APT-401' })
      valido = false
    }
    const tarifas: TarifaRequest[] = []
    Object.entries(datos.tarifas).forEach(([idTemporada, texto]) => {
      if (!texto?.trim()) return
      const valor = aPesos(texto)
      if (!Number.isInteger(valor) || valor <= 0) {
        setError(`tarifas.${idTemporada}`, { message: 'Valor mayor que cero, sin decimales' })
        valido = false
      } else tarifas.push({ idTemporada, valorPorOcupante: valor })
    })
    if (!valido) return

    const principal = Number(datos.principal)
    registrar.mutate(
      {
        ...comunes,
        identificacion: datos.identificacion.trim(),
        imagenes: datos.imagenes.map((img, i) => ({ url: img.url.trim(), principal: i === principal })),
        tarifas: tarifas.length > 0 ? tarifas : null,
      },
      {
        onSuccess: (creado) => {
          toast.success('Apartamento registrado', { description: `${creado.identificacion} nace inactivo hasta que lo actives.` })
          navigate(`/panel/apartamentos/${encodeURIComponent(creado.identificacion)}`)
        },
        onError: (error) => marcarErrores(error),
      },
    )
  }

  /** Los 400 traen campo → mensaje: se muestran junto a cada campo */
  const marcarErrores = (error: unknown) => {
    if (!esErrorApi(error) || error.status !== 400) return
    Object.entries(error.errores).forEach(([campo, mensaje]) => {
      const base = campo.split(/[.[]/)[0] as keyof Datos
      if (base in VACIO) setError(base, { message: mensaje })
    })
  }

  const errorGeneral =
    mutacion.error && !(esErrorApi(mutacion.error) && mutacion.error.status === 400) ? mutacion.error : null
  const rutaVolver = editando ? `/panel/apartamentos/${encodeURIComponent(id)}` : '/panel/apartamentos'

  if (editando && existente.error) {
    return (
      <section className="mx-auto w-full max-w-3xl">
        <AvisoError error={existente.error} />
      </section>
    )
  }
  if (editando && !existente.data) {
    return (
      <section className="mx-auto flex w-full max-w-3xl flex-col gap-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-96 w-full" />
      </section>
    )
  }

  // Resultado de bajar la capacidad: reservas que quedan sobre la nueva capacidad
  if (resultado) {
    const apto = resultado.apartamento
    return (
      <section className="mx-auto flex w-full max-w-3xl flex-col gap-5">
        <h1 className="font-titulo text-3xl font-semibold">Apartamento actualizado</h1>
        <div
          role="alert"
          className="rounded-tarjeta border p-5"
          style={{ backgroundColor: '#FFF1C2', borderColor: '#F0D27A', color: '#6B4800' }}
        >
          <p className="flex items-center gap-2 font-semibold">
            <TriangleAlert aria-hidden="true" className="size-5" />
            Hay reservas con más personas que la nueva capacidad ({apto.capacidadMaxima})
          </p>
          {resultado.advertencia && <p className="mt-2 text-sm">{resultado.advertencia}</p>}
          <p className="mt-2 text-sm">Las reservas no cambian; revísalas con los huéspedes.</p>
          <ul className="mt-3 flex flex-wrap gap-2">
            {resultado.reservasSobreCapacidad.map((codigo) => (
              <li key={codigo}>
                <Link
                  to={`/panel/reservas/${encodeURIComponent(codigo)}`}
                  className="inline-flex min-h-11 items-center rounded-full bg-white/70 px-4 text-sm font-semibold underline-offset-2 hover:underline"
                >
                  {codigo}
                </Link>
              </li>
            ))}
          </ul>
        </div>
        <Button asChild className="w-fit">
          <Link to={`/panel/apartamentos/${encodeURIComponent(apto.identificacion)}`}>Ir al apartamento</Link>
        </Button>
      </section>
    )
  }

  return (
    <section className="mx-auto w-full max-w-3xl">
      <Link
        to={rutaVolver}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft aria-hidden="true" className="size-4" /> Volver
      </Link>
      <h1 className="mt-2 font-titulo text-3xl font-semibold">
        {editando ? `Editar ${existente.data?.nombre ?? ''}` : 'Nuevo apartamento'}
      </h1>
      {!editando && (
        <p className="text-sm text-muted-foreground">
          Nace inactivo: no sale en la búsqueda hasta que lo actives con tarifa en todas las temporadas.
        </p>
      )}

      <form noValidate onSubmit={handleSubmit(enviar)} className="mt-6 flex flex-col gap-5">
        <fieldset className="flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta md:p-5">
          <legend className="sr-only">Datos</legend>
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo id="identificacion" etiqueta="Identificación" error={errors.identificacion?.message}>
              <Input placeholder="APT-401" disabled={editando} autoCapitalize="characters" {...register('identificacion')} />
            </Campo>
            <Campo id="nombre" etiqueta="Nombre" error={errors.nombre?.message}>
              <Input placeholder="Coral Esmeralda" {...register('nombre')} />
            </Campo>
          </div>
          <Campo id="descripcion" etiqueta="Descripción (opcional)" error={errors.descripcion?.message}>
            <textarea className={claseTextarea} {...register('descripcion')} />
          </Campo>
          <div className="grid grid-cols-2 gap-4">
            <Campo id="dormitorios" etiqueta="Dormitorios" error={errors.dormitorios?.message}>
              <Input type="number" min={1} inputMode="numeric" {...register('dormitorios')} />
            </Campo>
            <Campo
              id="capacidadMaxima"
              etiqueta="Capacidad máxima"
              ayuda={editando ? 'Si la bajas, te mostramos las reservas que la superan.' : undefined}
              error={errors.capacidadMaxima?.message}
            >
              <Input type="number" min={1} inputMode="numeric" {...register('capacidadMaxima')} />
            </Campo>
          </div>
          <Campo id="dotacion" etiqueta="Dotación" ayuda="Un elemento por línea" error={errors.dotacion?.message}>
            <textarea
              className={claseTextarea}
              placeholder={'Cama extragrande\nCocina integral\nBalcón con vista al mar'}
              {...register('dotacion')}
            />
          </Campo>
        </fieldset>

        {!editando && (
          <fieldset className="rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta md:p-5">
            <legend className="float-left mb-1 font-titulo text-xl font-semibold">Imágenes</legend>
            <p className="clear-both text-xs text-muted-foreground">
              Entre 1 y {MAXIMO_IMAGENES}. Marca cuál es la principal.
            </p>
            <ul className="mt-4 flex flex-col gap-3">
              {imagenes.fields.map((campo, i) => (
                <li key={campo.id} className="flex flex-wrap items-start gap-x-3 gap-y-1 sm:flex-nowrap">
                  <FotoApartamento src={urls?.[i]?.url} alt="" className="mt-6 size-11 shrink-0 rounded-campo bg-crema" />
                  <Campo
                    id={`imagen-${i}`}
                    etiqueta={`URL de la imagen ${i + 1}`}
                    error={errors.imagenes?.[i]?.url?.message}
                    className="min-w-0 flex-1 basis-[calc(100%-3.5rem)] sm:basis-0"
                  >
                    <Input type="url" inputMode="url" placeholder="https://…" {...register(`imagenes.${i}.url`)} />
                  </Campo>
                  <label className="ml-14 flex min-h-11 shrink-0 cursor-pointer items-center gap-1.5 text-sm sm:mt-6 sm:ml-0">
                    <input
                      type="radio"
                      value={String(i)}
                      className="size-4 accent-[var(--coral-primario)]"
                      {...register('principal')}
                    />
                    Principal
                  </label>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="shrink-0 sm:mt-6"
                    aria-label={`Quitar la imagen ${i + 1}`}
                    disabled={imagenes.fields.length === 1}
                    onClick={() => {
                      // La principal sigue apuntando a la misma imagen; si se quita, pasa a la primera
                      const p = Number(principal)
                      imagenes.remove(i)
                      if (p === i) setValue('principal', '0')
                      else if (p > i) setValue('principal', String(p - 1))
                    }}
                  >
                    <Trash2 aria-hidden="true" />
                  </Button>
                </li>
              ))}
            </ul>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="mt-3"
              disabled={imagenes.fields.length >= MAXIMO_IMAGENES}
              onClick={() => imagenes.append({ url: '' })}
            >
              <ImagePlus aria-hidden="true" /> Agregar imagen
            </Button>
          </fieldset>
        )}

        {!editando && (
          <fieldset className="rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta md:p-5">
            <legend className="float-left mb-1 font-titulo text-xl font-semibold">Tarifas (opcional)</legend>
            <p className="clear-both text-xs text-muted-foreground">
              Por ocupante facturable y por noche. Sin tarifa en todas las temporadas no se puede activar.
            </p>
            <div className="mt-4 grid gap-4 sm:grid-cols-3">
              {(temporadas.data ?? []).map((t) => (
                <Campo
                  key={t.id}
                  id={`tarifa-${t.id}`}
                  etiqueta={`Temporada ${t.nombre.toLowerCase()}`}
                  error={errors.tarifas?.[t.id]?.message}
                >
                  <Input inputMode="numeric" placeholder="300000" {...register(`tarifas.${t.id}`)} />
                </Campo>
              ))}
            </div>
          </fieldset>
        )}

        {errorGeneral && <AvisoError error={errorGeneral} />}

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button asChild variant="outline">
            <Link to={rutaVolver}>Cancelar</Link>
          </Button>
          <Button type="submit" disabled={mutacion.isPending}>
            {mutacion.isPending && <Loader2 aria-hidden="true" className="animate-spin" />}
            {editando ? 'Guardar cambios' : 'Registrar apartamento'}
          </Button>
        </div>
      </form>
    </section>
  )
}
