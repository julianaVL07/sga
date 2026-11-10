import { useState, type FormEvent } from 'react'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { useDefinirTarifas, type TarifaRequest } from '@/api/apartamentos'
import type { TarifaResponse, TemporadaResponse } from '@/api/tipos'
import { AvisoError, Campo, formatearDinero } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { aPesos } from './utilidades'

interface Props {
  identificacion: string
  temporadas: TemporadaResponse[]
  vigentes: TarifaResponse[]
  abierto: boolean
  alCambiar: (abierto: boolean) => void
}

/** Define tarifas por temporada que rigen desde hoy; la anterior queda en el histórico */
export function DialogoTarifas({ identificacion, temporadas, vigentes, abierto, alCambiar }: Props) {
  const definir = useDefinirTarifas(identificacion)
  const actuales = Object.fromEntries(vigentes.map((t) => [t.idTemporada, t.valorPorOcupante]))
  const [valores, setValores] = useState<Record<string, string>>({})
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [aviso, setAviso] = useState<string | null>(null)

  const cambiar = (valor: boolean) => {
    if (definir.isPending) return
    if (valor) {
      setValores(Object.fromEntries(temporadas.map((t) => [t.id, actuales[t.id] ? String(actuales[t.id]) : ''])))
    } else {
      definir.reset()
      setErrores({})
      setAviso(null)
    }
    alCambiar(valor)
  }

  const enviar = (evento: FormEvent) => {
    evento.preventDefault()
    const nuevos: Record<string, string> = {}
    const tarifas: TarifaRequest[] = []
    temporadas.forEach((t) => {
      const texto = valores[t.id] ?? ''
      if (texto.trim() === '') return
      const valor = aPesos(texto)
      if (!Number.isInteger(valor) || valor <= 0) {
        nuevos[t.id] = 'Escribe un valor mayor que cero, sin decimales'
        return
      }
      if (valor !== actuales[t.id]) tarifas.push({ idTemporada: t.id, valorPorOcupante: valor })
    })
    setErrores(nuevos)
    setAviso(null)
    if (Object.keys(nuevos).length > 0) return
    if (tarifas.length === 0) {
      setAviso('No cambiaste ninguna tarifa.')
      return
    }
    definir.mutate(tarifas, {
      onSuccess: () => {
        toast.success(tarifas.length === 1 ? 'Tarifa definida' : 'Tarifas definidas', { description: identificacion })
        cambiar(false)
      },
    })
  }

  return (
    <Dialog open={abierto} onOpenChange={cambiar}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="font-titulo text-xl">Definir tarifas</DialogTitle>
          <DialogDescription>
            Valor por ocupante facturable y por noche. Las nuevas rigen desde hoy y la anterior queda en el histórico;
            las reservas ya creadas no cambian.
          </DialogDescription>
        </DialogHeader>
        <form id="form-tarifas" noValidate onSubmit={enviar} className="flex flex-col gap-4">
          {temporadas.map((t) => (
            <Campo
              key={t.id}
              id={`tarifa-${t.id}`}
              etiqueta={`Temporada ${t.nombre.toLowerCase()}`}
              ayuda={actuales[t.id] ? `Vigente: ${formatearDinero(actuales[t.id])}` : 'Sin tarifa'}
              error={errores[t.id]}
            >
              <Input
                inputMode="numeric"
                placeholder="300000"
                value={valores[t.id] ?? ''}
                onChange={(e) => setValores((v) => ({ ...v, [t.id]: e.target.value }))}
              />
            </Campo>
          ))}
          {aviso && <AvisoError mensaje={aviso} />}
          {definir.error && <AvisoError error={definir.error} />}
        </form>
        <DialogFooter>
          <Button variant="outline" onClick={() => cambiar(false)} disabled={definir.isPending}>
            Cancelar
          </Button>
          <Button type="submit" form="form-tarifas" disabled={definir.isPending}>
            {definir.isPending && <Loader2 aria-hidden="true" className="animate-spin" />}
            Guardar tarifas
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
