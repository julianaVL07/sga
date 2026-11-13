import { useState, type FormEvent, type ReactNode } from 'react'
import { Loader2, Lock, Save } from 'lucide-react'
import { toast } from 'sonner'
import { useActualizarAlojamiento, type ActualizarAlojamientoRequest } from '@/api/configuracion'
import { useAlojamiento } from '@/api/publico'
import type { AlojamientoResponse, ParametrosAlojamientoResponse } from '@/api/tipos'
import { AvisoError, Campo, Dinero } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { erroresDe } from '@/pages/panel/folio/erroresDe'
import { formatearHora } from '@/pages/publico/comun/reglas'
import { claseTextarea } from '@/pages/publico/comun/estilos'

interface Formulario {
  nombre: string
  descripcion: string
  ciudad: string
  direccion: string
  latitud: string
  longitud: string
  horaEntrada: string
  horaSalida: string
  normaConvivencia: string
}

const aFormulario = (a: AlojamientoResponse): Formulario => ({
  nombre: a.nombre ?? '',
  descripcion: a.descripcion ?? '',
  ciudad: a.ciudad ?? '',
  direccion: a.direccion ?? '',
  latitud: String(a.latitud ?? ''),
  longitud: String(a.longitud ?? ''),
  horaEntrada: a.horaEntrada?.slice(0, 5) ?? '',
  horaSalida: a.horaSalida?.slice(0, 5) ?? '',
  normaConvivencia: a.normaConvivencia ?? '',
})

const vacioANull = (texto: string) => (texto.trim() === '' ? null : texto.trim())

export default function Alojamiento() {
  const alojamiento = useAlojamiento()

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-6">
      <header>
        <h1 className="font-titulo text-3xl font-semibold">Alojamiento</h1>
        <p className="mt-1 text-sm text-muted-foreground">Datos, horario y normas que ven los huéspedes.</p>
      </header>
      {alojamiento.error ? (
        <AvisoError titulo="No se pudo consultar el alojamiento" error={alojamiento.error} />
      ) : !alojamiento.data ? (
        <>
          <Skeleton className="h-96" />
          <Skeleton className="h-48" />
        </>
      ) : (
        <>
          <FormularioAlojamiento alojamiento={alojamiento.data} />
          <ServiciosAdicionales alojamiento={alojamiento.data} />
          <Parametros parametros={alojamiento.data.parametros} />
        </>
      )}
    </section>
  )
}

function FormularioAlojamiento({ alojamiento }: { alojamiento: AlojamientoResponse }) {
  const actualizar = useActualizarAlojamiento()
  const [datos, setDatos] = useState<Formulario>(() => aFormulario(alojamiento))
  const [errores, setErrores] = useState<Partial<Record<keyof Formulario, string>>>({})

  const campo = (clave: keyof Formulario) => ({
    value: datos[clave],
    onChange: (e: { target: { value: string } }) => setDatos((d) => ({ ...d, [clave]: e.target.value })),
  })
  const deServidor = erroresDe(actualizar.error)
  const error = (clave: keyof Formulario) => errores[clave] ?? deServidor[clave]

  const enviar = (evento: FormEvent) => {
    evento.preventDefault()
    const nuevos: Partial<Record<keyof Formulario, string>> = {}
    if (!datos.nombre.trim()) nuevos.nombre = 'Escribe el nombre'
    if (!datos.ciudad.trim()) nuevos.ciudad = 'Escribe la ciudad'
    const latitud = Number(datos.latitud)
    const longitud = Number(datos.longitud)
    if (datos.latitud.trim() === '' || Number.isNaN(latitud) || latitud < -90 || latitud > 90)
      nuevos.latitud = 'Entre -90 y 90'
    if (datos.longitud.trim() === '' || Number.isNaN(longitud) || longitud < -180 || longitud > 180)
      nuevos.longitud = 'Entre -180 y 180'
    // L-12: las dos horas o ninguna
    if (!!datos.horaEntrada !== !!datos.horaSalida) {
      nuevos[datos.horaEntrada ? 'horaSalida' : 'horaEntrada'] = 'Escribe las dos horas del horario o ninguna'
    }
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return

    const solicitud: ActualizarAlojamientoRequest = {
      nombre: datos.nombre.trim(),
      descripcion: vacioANull(datos.descripcion),
      ciudad: datos.ciudad.trim(),
      direccion: vacioANull(datos.direccion),
      latitud,
      longitud,
      normaConvivencia: vacioANull(datos.normaConvivencia),
      horaEntrada: datos.horaEntrada || null,
      horaSalida: datos.horaSalida || null,
    }
    actualizar.mutate(solicitud, {
      onSuccess: (nuevo) => {
        setDatos(aFormulario(nuevo))
        toast.success('Alojamiento actualizado')
      },
    })
  }

  return (
    <form
      noValidate
      onSubmit={enviar}
      className="flex flex-col gap-6 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6"
    >
      <Seccion titulo="Datos">
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo id="aloj-nombre" etiqueta="Nombre" error={error('nombre')}>
            <Input {...campo('nombre')} />
          </Campo>
          <Campo id="aloj-ciudad" etiqueta="Ciudad" error={error('ciudad')}>
            <Input {...campo('ciudad')} />
          </Campo>
          <Campo id="aloj-direccion" etiqueta="Dirección" error={error('direccion')} className="sm:col-span-2">
            <Input {...campo('direccion')} />
          </Campo>
          <Campo id="aloj-latitud" etiqueta="Latitud" error={error('latitud')}>
            <Input inputMode="decimal" {...campo('latitud')} />
          </Campo>
          <Campo id="aloj-longitud" etiqueta="Longitud" error={error('longitud')}>
            <Input inputMode="decimal" {...campo('longitud')} />
          </Campo>
          <Campo id="aloj-descripcion" etiqueta="Descripción" error={error('descripcion')} className="sm:col-span-2">
            <textarea className={claseTextarea} {...campo('descripcion')} />
          </Campo>
        </div>
      </Seccion>

      <Seccion titulo="Horario">
        <div className="grid grid-cols-2 gap-4">
          <Campo id="aloj-entrada" etiqueta="Hora de entrada" error={error('horaEntrada')}>
            <Input type="time" {...campo('horaEntrada')} />
          </Campo>
          <Campo id="aloj-salida" etiqueta="Hora de salida" error={error('horaSalida')}>
            <Input type="time" {...campo('horaSalida')} />
          </Campo>
        </div>
      </Seccion>

      <Seccion titulo="Normas de convivencia">
        <Campo id="aloj-normas" etiqueta="Normas" error={error('normaConvivencia')}>
          <textarea className={`${claseTextarea} min-h-32`} {...campo('normaConvivencia')} />
        </Campo>
      </Seccion>

      {actualizar.error && <AvisoError titulo="No se pudo guardar" error={actualizar.error} />}
      <div className="flex justify-end">
        <Button type="submit" disabled={actualizar.isPending} className="w-full sm:w-auto">
          {actualizar.isPending ? <Loader2 aria-hidden="true" className="animate-spin" /> : <Save aria-hidden="true" />}
          Guardar cambios
        </Button>
      </div>
    </form>
  )
}

function Seccion({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <fieldset className="flex flex-col gap-3">
      <legend className="mb-3 font-titulo text-xl font-semibold">{titulo}</legend>
      {children}
    </fieldset>
  )
}

function NotaSoloLectura({ children }: { children: ReactNode }) {
  return (
    <p className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
      <Lock aria-hidden="true" className="size-3.5 shrink-0" />
      {children}
    </p>
  )
}

function ServiciosAdicionales({ alojamiento }: { alojamiento: AlojamientoResponse }) {
  const servicios = alojamiento.serviciosAdicionales
  return (
    <div className="flex flex-col gap-3 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
      <div className="flex flex-col gap-1">
        <h2 className="font-titulo text-xl font-semibold">Servicios adicionales</h2>
        <NotaSoloLectura>Se cambian en la configuración del servidor.</NotaSoloLectura>
      </div>
      {servicios.length === 0 ? (
        <p className="text-sm text-muted-foreground">El alojamiento no tiene servicios adicionales.</p>
      ) : (
        <ul className="flex flex-col divide-y divide-border">
          {servicios.map((s) => (
            <li key={s.id} className="flex items-start justify-between gap-3 py-3 text-sm">
              <div className="min-w-0">
                <p className="font-medium">
                  {s.nombre}
                  {!s.activo && <span className="ml-2 text-xs font-normal text-muted-foreground">(inactivo)</span>}
                </p>
                {s.descripcion && <p className="text-muted-foreground">{s.descripcion}</p>}
              </div>
              <span className="shrink-0 text-right">
                {s.generaCargo ? <Dinero valor={s.valor} className="font-semibold" /> : <span className="text-muted-foreground">Sin cargo</span>}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function Parametros({ parametros: p }: { parametros: ParametrosAlojamientoResponse }) {
  const horas = (n: number | null) => (n === null ? 'Sin definir' : `${n} ${n === 1 ? 'hora' : 'horas'}`)
  const filas: [string, ReactNode][] = [
    ['Umbral de edad facturable', p.umbralEdadFacturable === null ? 'Sin definir' : `Desde ${p.umbralEdadFacturable} años`],
    ['Tiempo de preparación', horas(p.tiempoPreparacionHoras)],
    ['Plazo de confirmación', horas(p.plazoConfirmacionHoras)],
    ['Hora límite de no-show', p.horaLimiteNoShow ? formatearHora(p.horaLimiteNoShow) : 'Sin definir'],
    ['Anticipo', p.exigeAnticipo ? `${p.porcentajeAnticipo} % del valor de la estancia` : 'No se exige'],
  ]
  if (p.depositoGarantia !== null && p.depositoGarantia !== undefined) {
    filas.push(['Depósito de garantía', <Dinero key="deposito" valor={p.depositoGarantia} />])
  }
  return (
    <div className="flex flex-col gap-3 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
      <div className="flex flex-col gap-1">
        <h2 className="font-titulo text-xl font-semibold">Parámetros</h2>
        <NotaSoloLectura>Se cambian en la configuración del servidor.</NotaSoloLectura>
      </div>
      <dl className="grid gap-x-6 sm:grid-cols-2">
        {filas.map(([titulo, valor]) => (
          <div key={titulo} className="flex items-baseline justify-between gap-3 border-b border-border py-2.5 text-sm">
            <dt className="text-muted-foreground">{titulo}</dt>
            <dd className="text-right font-medium">{valor}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
