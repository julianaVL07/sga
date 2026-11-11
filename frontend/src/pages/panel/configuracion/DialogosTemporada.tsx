import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { useDetallesApartamentos, useTodosLosApartamentos } from '@/api/apartamentos'
import { useAgregarRangoTemporada, useCrearTemporada, type RangoFechasRequest } from '@/api/configuracion'
import type { TemporadaResponse, TipoTemporada } from '@/api/tipos'
import { AvisoError, Campo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { aPesos } from '@/pages/panel/apartamentos/utilidades'
import { DialogoFormulario } from '@/pages/panel/folio/DialogoFormulario'
import { erroresDe } from '@/pages/panel/folio/erroresDe'
import { useAlAbrir } from '@/pages/panel/folio/useAlAbrir'
import { esFechaValida } from '@/pages/publico/comun/reglas'
import { claseSelect } from '@/pages/publico/comun/estilos'
import { COLOR_TEMPORADA, finExcluido, textoRango, TIPOS_TEMPORADA } from './utilidadesTemporada'

interface Base {
  abierto: boolean
  alCambiar: (abierto: boolean) => void
}

interface RangoEditable {
  clave: number
  inicio: string
  /** Último día incluido */
  fin: string
}

/** Valida un rango con la fecha final incluida y lo convierte al formato del backend (final excluida) */
function validarRango(rango: { inicio: string; fin: string }): { error?: string; rango?: RangoFechasRequest } {
  if (!esFechaValida(rango.inicio) || !esFechaValida(rango.fin)) return { error: 'Escribe las dos fechas' }
  if (rango.fin < rango.inicio) return { error: 'La fecha final no puede ser anterior a la inicial' }
  return { rango: { fechaInicio: rango.inicio, fechaFin: finExcluido(rango.fin) } }
}

const AYUDA_FIN = 'Último día de la temporada, incluido.'

// ---------- Crear temporada ----------

interface PropsCrear extends Base {
  temporadas: TemporadaResponse[]
}

export function DialogoCrearTemporada({ temporadas, abierto, alCambiar }: PropsCrear) {
  const crear = useCrearTemporada()
  const apartamentos = useTodosLosApartamentos()
  const detalles = useDetallesApartamentos(abierto ? (apartamentos.data ?? []).map((a) => a.identificacion) : [])
  const cargandoApartamentos = apartamentos.isPending || detalles.some((d) => d.isPending)
  const errorApartamentos = apartamentos.error ?? detalles.find((d) => d.error)?.error
  // La regla 7.4 exige tarifa de cada apartamento activo
  const activos = detalles.flatMap((d) => (d.data && d.data.activo && !d.data.eliminado ? [d.data] : []))

  const [id, setId] = useState('')
  const [nombre, setNombre] = useState('')
  const [tipo, setTipo] = useState<TipoTemporada>('MEDIA')
  const [rangos, setRangos] = useState<RangoEditable[]>([{ clave: 0, inicio: '', fin: '' }])
  const [tarifas, setTarifas] = useState<Record<string, string>>({})
  const [errores, setErrores] = useState<Record<string, string | undefined>>({})

  useAlAbrir(abierto, () => {
    setId('')
    setNombre('')
    setTipo('MEDIA')
    setRangos([{ clave: Date.now(), inicio: '', fin: '' }])
    setTarifas({})
    setErrores({})
    crear.reset()
  })

  const hayBase = temporadas.some((t) => t.tipo === 'BASE')
  const esBase = tipo === 'BASE'

  const enviar = () => {
    const nuevos: Record<string, string | undefined> = {}
    if (!id.trim()) nuevos.id = 'Escribe un identificador, por ejemplo T-FESTIVAL'
    if (!nombre.trim()) nuevos.nombre = 'Escribe el nombre'

    const rangosEnviar: RangoFechasRequest[] = []
    if (!esBase) {
      rangos.forEach((r) => {
        const { error, rango } = validarRango(r)
        if (error) nuevos[`rango-${r.clave}`] = error
        else if (rango) rangosEnviar.push(rango)
      })
      if (rangos.length === 0) nuevos.rangos = 'Una temporada que no es base necesita al menos un rango'
    }

    const tarifasEnviar = activos.map((a) => {
      const valor = aPesos(tarifas[a.identificacion] ?? '')
      if (!Number.isInteger(valor) || valor <= 0) nuevos[`tarifa-${a.identificacion}`] = 'Valor mayor que cero, sin decimales'
      return { identificacionApartamento: a.identificacion, valorPorOcupante: valor }
    })

    setErrores(nuevos)
    if (Object.values(nuevos).some(Boolean)) return

    crear.mutate(
      { id: id.trim(), nombre: nombre.trim(), tipo, rangos: esBase ? [] : rangosEnviar, tarifas: tarifasEnviar },
      {
        onSuccess: (temporada) => {
          toast.success('Temporada creada', { description: `${temporada.nombre} (${temporada.id})` })
          alCambiar(false)
        },
      },
    )
  }

  const deServidor = erroresDe(crear.error)

  return (
    <DialogoFormulario
      id="form-temporada"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Nueva temporada"
      descripcion="Todo apartamento activo debe tener tarifa en todas las temporadas: la temporada se crea junto con la tarifa de cada uno, o no se crea."
      textoEnviar="Crear temporada"
      cargando={crear.isPending}
      error={crear.error}
      tituloError="No se pudo crear la temporada"
      className="sm:max-w-xl"
      alEnviar={enviar}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Campo id="temporada-id" etiqueta="Identificador" error={errores.id ?? deServidor.id}>
          <Input placeholder="T-FESTIVAL" value={id} onChange={(e) => setId(e.target.value.toUpperCase())} />
        </Campo>
        <Campo id="temporada-nombre" etiqueta="Nombre" error={errores.nombre ?? deServidor.nombre}>
          <Input placeholder="Festival" value={nombre} onChange={(e) => setNombre(e.target.value)} />
        </Campo>
      </div>
      <Campo
        id="temporada-tipo"
        etiqueta="Tipo"
        ayuda={
          esBase
            ? hayBase
              ? 'Ya hay una temporada base: solo puede haber una.'
              : 'La base no lleva rangos: cubre las fechas que no son de otra temporada.'
            : undefined
        }
      >
        <select className={claseSelect} value={tipo} onChange={(e) => setTipo(e.target.value as TipoTemporada)}>
          {TIPOS_TEMPORADA.map((t) => (
            <option key={t} value={t}>
              {COLOR_TEMPORADA[t].etiqueta}
              {t === 'BASE' && hayBase ? ' (ya existe)' : ''}
            </option>
          ))}
        </select>
      </Campo>

      {!esBase && (
        <fieldset className="flex flex-col gap-3">
          <legend className="mb-2 text-sm font-medium">Rangos de fechas</legend>
          {rangos.map((r, i) => (
            <div key={r.clave} className="flex flex-col gap-1.5 rounded-campo border border-border p-3">
              <div className="grid grid-cols-[1fr_1fr_auto] items-end gap-2">
                <Campo id={`rango-${r.clave}-inicio`} etiqueta="Desde">
                  <Input
                    type="date"
                    value={r.inicio}
                    onChange={(e) => setRangos((rs) => rs.map((x) => (x.clave === r.clave ? { ...x, inicio: e.target.value } : x)))}
                  />
                </Campo>
                <Campo id={`rango-${r.clave}-fin`} etiqueta="Hasta (incluido)">
                  <Input
                    type="date"
                    min={r.inicio || undefined}
                    value={r.fin}
                    onChange={(e) => setRangos((rs) => rs.map((x) => (x.clave === r.clave ? { ...x, fin: e.target.value } : x)))}
                  />
                </Campo>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label={`Quitar el rango ${i + 1}`}
                  disabled={rangos.length === 1}
                  onClick={() => setRangos((rs) => rs.filter((x) => x.clave !== r.clave))}
                >
                  <Trash2 aria-hidden="true" />
                </Button>
              </div>
              {errores[`rango-${r.clave}`] && (
                <p className="text-xs font-medium text-error">{errores[`rango-${r.clave}`]}</p>
              )}
            </div>
          ))}
          {errores.rangos && <p className="text-xs font-medium text-error">{errores.rangos}</p>}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="w-fit"
            onClick={() => setRangos((rs) => [...rs, { clave: Date.now(), inicio: '', fin: '' }])}
          >
            <Plus aria-hidden="true" /> Agregar otro rango
          </Button>
          <p className="text-xs text-muted-foreground">{AYUDA_FIN}</p>
        </fieldset>
      )}

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium">Tarifa por ocupante y noche</legend>
        <p className="text-xs text-muted-foreground">Una por cada apartamento activo. Rige desde hoy.</p>
        {errorApartamentos ? (
          <AvisoError titulo="No se pudieron consultar los apartamentos" error={errorApartamentos} />
        ) : cargandoApartamentos ? (
          <Skeleton className="h-24" />
        ) : activos.length === 0 ? (
          <p className="text-sm text-muted-foreground">No hay apartamentos activos.</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {activos.map((a) => (
              <Campo
                key={a.identificacion}
                id={`tarifa-${a.identificacion}`}
                etiqueta={`${a.identificacion} · ${a.nombre}`}
                error={errores[`tarifa-${a.identificacion}`]}
              >
                <Input
                  inputMode="numeric"
                  placeholder="450000"
                  value={tarifas[a.identificacion] ?? ''}
                  onChange={(e) => setTarifas((t) => ({ ...t, [a.identificacion]: e.target.value }))}
                />
              </Campo>
            ))}
          </div>
        )}
      </fieldset>
    </DialogoFormulario>
  )
}

// ---------- Agregar rango ----------

interface PropsRango extends Base {
  temporada: TemporadaResponse | null
}

export function DialogoAgregarRango({ temporada, abierto, alCambiar }: PropsRango) {
  const agregar = useAgregarRangoTemporada()
  const [inicio, setInicio] = useState('')
  const [fin, setFin] = useState('')
  const [error, setError] = useState<string>()

  useAlAbrir(abierto, () => {
    setInicio('')
    setFin('')
    setError(undefined)
    agregar.reset()
  })

  if (!temporada) return null

  const enviar = () => {
    const resultado = validarRango({ inicio, fin })
    setError(resultado.error)
    if (!resultado.rango) return
    agregar.mutate(
      { id: temporada.id, rango: resultado.rango },
      {
        onSuccess: () => {
          toast.success('Rango agregado', {
            description: `${temporada.nombre}: ${textoRango(resultado.rango as RangoFechasRequest)}`,
          })
          alCambiar(false)
        },
      },
    )
  }

  return (
    <DialogoFormulario
      id="form-rango"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo={`Agregar rango a ${temporada.nombre}`}
      descripcion="El rango no puede solaparse con otra temporada ni con los rangos que ya tiene."
      textoEnviar="Agregar rango"
      cargando={agregar.isPending}
      error={agregar.error}
      tituloError="No se pudo agregar el rango"
      alEnviar={enviar}
    >
      <div className="grid grid-cols-2 gap-3">
        <Campo id="rango-inicio" etiqueta="Desde">
          <Input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </Campo>
        <Campo id="rango-fin" etiqueta="Hasta (incluido)">
          <Input type="date" min={inicio || undefined} value={fin} onChange={(e) => setFin(e.target.value)} />
        </Campo>
      </div>
      {error ? <p className="-mt-2 text-xs font-medium text-error">{error}</p> : <p className="-mt-2 text-xs text-muted-foreground">{AYUDA_FIN}</p>}
    </DialogoFormulario>
  )
}
