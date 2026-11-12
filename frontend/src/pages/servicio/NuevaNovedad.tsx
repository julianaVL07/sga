import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Loader2, Send, Sparkles } from 'lucide-react'
import { toast } from 'sonner'
import { useRegistrarNovedad, useTodosLosApartamentos, type GravedadNovedad } from '@/api/apartamentos'
import { esErrorApi } from '@/api/cliente'
import { useAuth } from '@/auth/AuthContext'
import { AvisoError, Campo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { claseSelect, claseTextarea } from '@/pages/panel/calendario/estilos'
import { GRAVEDADES } from './gravedades'
import { useSugerenciaGravedad } from './useSugerenciaGravedad'

export default function NuevaNovedad() {
  const [parametros] = useSearchParams()
  const navigate = useNavigate()
  const { usuario } = useAuth()
  const apartamentos = useTodosLosApartamentos()
  const registrar = useRegistrarNovedad()

  const [apartamento, setApartamento] = useState(parametros.get('apartamento') ?? '')
  const [descripcion, setDescripcion] = useState('')
  const [gravedad, setGravedad] = useState<GravedadNovedad | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const { sugerencia, pedirSugerencia } = useSugerenciaGravedad()

  const cambiarDescripcion = (texto: string) => {
    setDescripcion(texto)
    pedirSugerencia(texto)
  }

  const enviar = (evento: FormEvent) => {
    evento.preventDefault()
    const nuevos: Record<string, string> = {}
    if (!apartamento) nuevos.apartamento = 'Elige el apartamento'
    if (!descripcion.trim()) nuevos.descripcion = 'Cuenta qué pasó'
    if (!gravedad) nuevos.gravedad = 'Elige la gravedad'
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0 || !gravedad) return

    registrar.mutate(
      {
        id: apartamento,
        datos: { autor: usuario?.nombre ?? 'Personal de servicio', descripcion: descripcion.trim(), gravedad },
      },
      {
        onSuccess: (novedad) => {
          toast.success('Novedad registrada', { description: novedad.identificacionApartamento })
          navigate(`/servicio/apartamentos/${encodeURIComponent(novedad.identificacionApartamento)}`)
        },
        onError: (error) => {
          if (esErrorApi(error) && error.status === 400) setErrores(error.errores)
        },
      },
    )
  }

  const errorGeneral =
    registrar.error && !(esErrorApi(registrar.error) && registrar.error.status === 400) ? registrar.error : null

  return (
    <form noValidate onSubmit={enviar} className="flex flex-col gap-5">
      <h1 className="sr-only">Reportar una novedad</h1>

      <Campo id="novedad-apartamento" etiqueta="Apartamento" error={errores.apartamento}>
        <select
          value={apartamento}
          onChange={(e) => setApartamento(e.target.value)}
          className={cn(claseSelect, 'h-14 text-base')}
          disabled={apartamentos.isPending}
        >
          <option value="">{apartamentos.isPending ? 'Cargando…' : 'Elige el apartamento'}</option>
          {(apartamentos.data ?? []).map((a) => (
            <option key={a.identificacion} value={a.identificacion}>
              {a.identificacion} · {a.nombre}
            </option>
          ))}
        </select>
      </Campo>
      {apartamentos.error && <AvisoError error={apartamentos.error} />}

      <Campo id="novedad-descripcion" etiqueta="¿Qué pasó?" error={errores.descripcion}>
        <textarea
          value={descripcion}
          onChange={(e) => cambiarDescripcion(e.target.value)}
          placeholder="Copa de cristal rota en la cocina"
          className={cn(claseTextarea, 'min-h-36 text-base')}
        />
      </Campo>

      {/* Sugerencia de gravedad de la IA: solo ayuda, la gravedad la elige siempre la persona */}
      <div id="sugerencia-gravedad-ia" data-sugerencia-ia aria-live="polite" className="empty:hidden">
        {sugerencia.estado === 'disponible' && (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-campo border border-border bg-muted/60 px-4 py-3">
            <p className="flex min-w-0 flex-1 items-start gap-2 text-sm">
              <Sparkles aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-coral" />
              <span>
                Sugerencia de la IA: <strong>{sugerencia.gravedad}</strong> · {sugerencia.explicacion}
              </span>
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={gravedad === sugerencia.gravedad}
              onClick={() => setGravedad(sugerencia.gravedad)}
            >
              {gravedad === sugerencia.gravedad ? 'Sugerencia usada' : 'Usar sugerencia'}
            </Button>
          </div>
        )}
        {sugerencia.estado === 'no-disponible' && (
          <p className="text-xs text-muted-foreground">Sugerencia no disponible</p>
        )}
      </div>

      <fieldset aria-describedby={errores.gravedad ? 'gravedad-error' : undefined}>
        <legend className="mb-1.5 text-sm font-medium">Gravedad</legend>
        <div role="radiogroup" aria-label="Gravedad" className="grid grid-cols-3 gap-3">
          {GRAVEDADES.map((g) => {
            const elegida = gravedad === g.valor
            return (
              <button
                key={g.valor}
                type="button"
                role="radio"
                aria-checked={elegida}
                onClick={() => setGravedad(g.valor)}
                className={cn(
                  'flex h-14 items-center justify-center rounded-campo border-2 text-base font-semibold transition-colors duration-150 outline-none focus-visible:ring-3 focus-visible:ring-ring/50',
                  !elegida && 'border-border bg-card text-foreground hover:bg-muted',
                )}
                style={elegida ? { backgroundColor: g.fondo, borderColor: g.borde, color: g.texto } : undefined}
              >
                {g.etiqueta}
              </button>
            )
          })}
        </div>
        {errores.gravedad && (
          <p id="gravedad-error" className="mt-1.5 text-xs font-medium text-error">
            {errores.gravedad}
          </p>
        )}
      </fieldset>

      {errorGeneral && <AvisoError error={errorGeneral} />}

      <Button type="submit" className="h-14 w-full text-base" disabled={registrar.isPending}>
        {registrar.isPending ? (
          <Loader2 aria-hidden="true" className="size-5 animate-spin" />
        ) : (
          <Send aria-hidden="true" className="size-5" />
        )}
        Registrar novedad
      </Button>
    </form>
  )
}
