import { useState } from 'react'
import { History, Info, Plus, ScrollText, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { useActualizarPoliticaCancelacion, useVersionesPolitica } from '@/api/configuracion'
import { usePoliticaVigente } from '@/api/publico'
import type { PoliticaCancelacionResponse, TramoCancelacionResponse } from '@/api/tipos'
import { AvisoError, Campo, Fecha, Paginador, Vacio } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DialogoFormulario } from '@/pages/panel/folio/DialogoFormulario'
import { claseTextarea } from '@/pages/publico/comun/estilos'

/** Tramos del de más antelación al de menos, con el intervalo de días que cubre cada uno */
function tramosConIntervalo(tramos: TramoCancelacionResponse[]) {
  const ordenados = [...tramos].sort((a, b) => b.antelacionMinimaDias - a.antelacionMinimaDias)
  return ordenados.map((t, i) => {
    const desde = t.antelacionMinimaDias
    const hasta = i === 0 ? null : ordenados[i - 1].antelacionMinimaDias - 1
    const dias = (n: number) => `${n} ${n === 1 ? 'día' : 'días'}`
    const intervalo =
      hasta === null ? `${dias(desde)} o más` : desde === hasta ? dias(desde) : `De ${desde} a ${dias(hasta)}`
    return { ...t, intervalo }
  })
}

export default function Politica() {
  const vigente = usePoliticaVigente()
  const [pagina, setPagina] = useState(0)
  const versiones = useVersionesPolitica(pagina)
  const [publicando, setPublicando] = useState(false)

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-titulo text-3xl font-semibold">Política de cancelación</h1>
          <p className="mt-1 text-sm text-muted-foreground">Cuánto se retiene al cancelar según los días de antelación.</p>
        </div>
        <Button onClick={() => setPublicando(true)} disabled={!vigente.data}>
          <Plus aria-hidden="true" /> Publicar nueva versión
        </Button>
      </header>

      <div
        role="note"
        className="flex gap-3 rounded-campo border border-border bg-muted/60 px-4 py-3 text-sm"
      >
        <Info aria-hidden="true" className="mt-0.5 size-5 shrink-0 text-primary" />
        <p>
          Cada reserva conserva la política con la que se creó. Publicar una nueva versión solo afecta a las reservas
          que se creen desde hoy; las anteriores siguen con su versión.
        </p>
      </div>

      {/* ---------- Vigente ---------- */}
      <div className="flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
        {vigente.error ? (
          <AvisoError titulo="No se pudo consultar la política" error={vigente.error} />
        ) : !vigente.data ? (
          <Skeleton className="h-48" />
        ) : (
          <>
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <h2 className="font-titulo text-xl font-semibold">Versión {vigente.data.version} · vigente</h2>
              <p className="text-sm text-muted-foreground">
                Desde el <Fecha valor={vigente.data.vigenteDesde} patron="d 'de' MMMM yyyy" />
              </p>
            </div>
            <TablaTramos politica={vigente.data} />
            <div className="text-sm">
              <p className="font-semibold">Si el huésped no se presenta (no-show)</p>
              <p className="text-muted-foreground">{vigente.data.consecuenciaNoShow}</p>
            </div>
          </>
        )}
      </div>

      {/* ---------- Historial ---------- */}
      <div className="flex flex-col gap-4 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
        <h2 className="inline-flex items-center gap-2 font-titulo text-xl font-semibold">
          <History aria-hidden="true" className="size-5 text-coral" /> Historial de versiones
        </h2>
        {versiones.error ? (
          <AvisoError error={versiones.error} />
        ) : !versiones.data ? (
          <Skeleton className="h-32" />
        ) : versiones.data.contenido.length === 0 ? (
          <Vacio icono={ScrollText} titulo="Sin versiones" />
        ) : (
          <>
            <ol className="flex flex-col divide-y divide-border">
              {versiones.data.contenido.map((v) => (
                <li key={v.version} className="flex flex-col gap-1 py-3 sm:flex-row sm:items-baseline sm:gap-4">
                  <div className="flex shrink-0 items-baseline gap-2 sm:w-56">
                    <span className="font-semibold">Versión {v.version}</span>
                    {v.version === vigente.data?.version && (
                      <span className="rounded-full bg-[#DDF2EA] px-2 py-0.5 text-xs font-semibold text-[#1C5E45]">Vigente</span>
                    )}
                  </div>
                  <p className="shrink-0 text-sm text-muted-foreground sm:w-40">
                    <Fecha valor={v.vigenteDesde} patron="d MMM yyyy" />
                  </p>
                  <p className="text-sm">
                    {tramosConIntervalo(v.tramos)
                      .map((t) => `${t.intervalo.toLowerCase()}: ${t.porcentajeRetencion} %`)
                      .join(' · ')}
                  </p>
                </li>
              ))}
            </ol>
            <Paginador
              pagina={pagina}
              totalPaginas={versiones.data.totalPaginas}
              totalElementos={versiones.data.totalElementos}
              alCambiar={setPagina}
            />
          </>
        )}
      </div>

      <DialogoPublicar vigente={vigente.data} abierto={publicando} alCambiar={setPublicando} />
    </section>
  )
}

function TablaTramos({ politica }: { politica: PoliticaCancelacionResponse }) {
  return (
    <div className="overflow-hidden rounded-tabla border border-border">
      <Table>
        <caption className="sr-only">Tramos de la política de cancelación vigente</caption>
        <TableHeader>
          <TableRow className="bg-muted/60 hover:bg-muted/60">
            <TableHead className="px-4 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Cancela con antelación de
            </TableHead>
            <TableHead className="px-4 text-right text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Se retiene
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tramosConIntervalo(politica.tramos).map((t) => (
            <TableRow key={t.antelacionMinimaDias}>
              <TableCell className="px-4 whitespace-normal">{t.intervalo}</TableCell>
              <TableCell className="px-4 text-right font-semibold tabular-nums">{t.porcentajeRetencion} %</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

// ---------- Publicar nueva versión ----------

interface TramoEditable {
  clave: number
  antelacion: string
  porcentaje: string
}

interface PropsPublicar {
  vigente: PoliticaCancelacionResponse | undefined
  abierto: boolean
  alCambiar: (abierto: boolean) => void
}

function DialogoPublicar({ vigente, abierto, alCambiar }: PropsPublicar) {
  const publicar = useActualizarPoliticaCancelacion()
  const [tramos, setTramos] = useState<TramoEditable[]>([])
  const [consecuencia, setConsecuencia] = useState('')
  const [errores, setErrores] = useState<Record<string, string | undefined>>({})

  // Al abrir, el formulario parte de la versión vigente
  const [anterior, setAnterior] = useState(abierto)
  if (abierto !== anterior) {
    setAnterior(abierto)
    if (abierto) {
      setTramos(
        tramosConIntervalo(vigente?.tramos ?? []).map((t, i) => ({
          clave: i,
          antelacion: String(t.antelacionMinimaDias),
          porcentaje: String(t.porcentajeRetencion),
        })),
      )
      setConsecuencia(vigente?.consecuenciaNoShow ?? '')
      setErrores({})
      publicar.reset()
    }
  }

  const cambiar = (clave: number, campo: 'antelacion' | 'porcentaje', valor: string) =>
    setTramos((ts) => ts.map((t) => (t.clave === clave ? { ...t, [campo]: valor } : t)))

  const enviar = () => {
    const nuevos: Record<string, string | undefined> = {}
    const convertidos = tramos.map((t) => {
      const antelacion = Number(t.antelacion)
      const porcentaje = Number(t.porcentaje)
      if (t.antelacion.trim() === '' || !Number.isInteger(antelacion) || antelacion < 0)
        nuevos[`antelacion-${t.clave}`] = 'Días enteros, 0 o más'
      if (t.porcentaje.trim() === '' || !Number.isInteger(porcentaje) || porcentaje < 0 || porcentaje > 100)
        nuevos[`porcentaje-${t.clave}`] = 'Entre 0 y 100'
      return { antelacionMinimaDias: antelacion, porcentajeRetencion: porcentaje }
    })
    if (convertidos.length < 2) nuevos.tramos = 'La política necesita al menos dos tramos.'
    else if (new Set(convertidos.map((t) => t.antelacionMinimaDias)).size !== convertidos.length)
      nuevos.tramos = 'Dos tramos no pueden tener la misma antelación.'
    else if (!convertidos.some((t) => t.antelacionMinimaDias === 0)) nuevos.tramos = 'Debe haber un tramo desde 0 días.'
    if (!consecuencia.trim()) nuevos.consecuencia = 'Describe qué pasa si el huésped no se presenta'

    setErrores(nuevos)
    if (Object.values(nuevos).some(Boolean)) return

    publicar.mutate(
      {
        consecuenciaNoShow: consecuencia.trim(),
        tramos: convertidos.sort((a, b) => b.antelacionMinimaDias - a.antelacionMinimaDias),
      },
      {
        onSuccess: (politica) => {
          toast.success(`Versión ${politica.version} publicada`, { description: 'Rige para las reservas nuevas.' })
          alCambiar(false)
        },
      },
    )
  }

  return (
    <DialogoFormulario
      id="form-politica"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Publicar nueva versión"
      descripcion={`Se crea la versión ${(vigente?.version ?? 0) + 1}, vigente desde hoy. La versión actual se conserva y las reservas existentes no cambian.`}
      textoEnviar="Publicar versión"
      cargando={publicar.isPending}
      error={publicar.error}
      tituloError="No se pudo publicar la política"
      className="sm:max-w-lg"
      alEnviar={enviar}
    >
      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium">Tramos</legend>
        <p className="text-xs text-muted-foreground">
          Cada tramo aplica desde su antelación mínima. Al menos dos, con antelaciones distintas y uno desde 0 días.
        </p>
        {tramos.map((t, i) => (
          <div key={t.clave} className="grid grid-cols-[1fr_1fr_auto] items-start gap-2">
            <Campo id={`tramo-${t.clave}-antelacion`} etiqueta="Desde (días)" error={errores[`antelacion-${t.clave}`]}>
              <Input inputMode="numeric" value={t.antelacion} onChange={(e) => cambiar(t.clave, 'antelacion', e.target.value)} />
            </Campo>
            <Campo id={`tramo-${t.clave}-porcentaje`} etiqueta="Retiene (%)" error={errores[`porcentaje-${t.clave}`]}>
              <Input inputMode="numeric" value={t.porcentaje} onChange={(e) => cambiar(t.clave, 'porcentaje', e.target.value)} />
            </Campo>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="mt-6"
              aria-label={`Quitar el tramo ${i + 1}`}
              onClick={() => setTramos((ts) => ts.filter((x) => x.clave !== t.clave))}
            >
              <Trash2 aria-hidden="true" />
            </Button>
          </div>
        ))}
        {errores.tramos && <p className="text-xs font-medium text-error">{errores.tramos}</p>}
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="w-fit"
          onClick={() => setTramos((ts) => [...ts, { clave: Date.now(), antelacion: '', porcentaje: '' }])}
        >
          <Plus aria-hidden="true" /> Agregar tramo
        </Button>
      </fieldset>
      <Campo id="politica-noshow" etiqueta="Consecuencia del no-show" error={errores.consecuencia}>
        <textarea className={claseTextarea} value={consecuencia} onChange={(e) => setConsecuencia(e.target.value)} />
      </Campo>
    </DialogoFormulario>
  )
}
