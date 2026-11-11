import { useState } from 'react'
import { Loader2, Plus, Trash2 } from 'lucide-react'
import { useTodosLosApartamentos } from '@/api/apartamentos'
import { useApartamento } from '@/api/publico'
import {
  useModificarReserva,
  type ModificacionReservaResponse,
  type ModificarReservaRequest,
  type ReservaDetalleResponse,
} from '@/api/reservas'
import type { OcupanteRequest } from '@/api/tipos'
import { AvisoError, Campo, Dinero } from '@/components/coral'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import { useCotizacion, useReglasAlojamiento, useTemporadasAlojamiento } from '@/pages/publico/comun/consultas'
import { EtiquetaPago } from '@/pages/publico/comun/EtiquetaPago'
import { claseSelect } from '@/pages/publico/comun/estilos'
import { SelectorMascotas } from '@/pages/publico/comun/SelectorMascotas'
import { TextoCargo } from '@/pages/publico/comun/TextoCargo'
import { avisoEstanciaMinima, esFechaValida, hoyISO, nochesEntre, sumarDias } from '@/pages/publico/comun/reglas'

interface Props {
  reserva: ReservaDetalleResponse
  abierto: boolean
  alCambiar: (abierto: boolean) => void
  alModificar: (resultado: ModificacionReservaResponse) => void
}

interface OcupanteFormulario {
  nombre: string
  tipoDocumento: string
  numeroDocumento: string
  fechaNacimiento: string
}

function aFormulario(reserva: ReservaDetalleResponse) {
  return {
    apartamento: reserva.identificacionApartamento,
    entrada: reserva.fechaEntrada,
    salida: reserva.fechaSalida,
    ocupantes: reserva.ocupantes.map<OcupanteFormulario>((o) => ({
      nombre: o.nombre,
      tipoDocumento: o.tipoDocumento ?? '',
      numeroDocumento: o.numeroDocumento ?? '',
      fechaNacimiento: o.fechaNacimiento,
    })),
    mascotas: reserva.cantidadMascotas,
    peso: reserva.cantidadMascotas > 0 ? String(reserva.pesoMascotaMasPesadaKg) : '',
  }
}

function aOcupantes(lista: OcupanteFormulario[]): OcupanteRequest[] {
  return lista.map((o) => ({
    nombre: o.nombre.trim(),
    tipoDocumento: o.tipoDocumento || undefined,
    numeroDocumento: o.numeroDocumento.trim() || undefined,
    fechaNacimiento: o.fechaNacimiento,
  }))
}

/** Fechas, ocupantes, apartamento y mascotas; muestra la diferencia que se asentará en el folio */
export function DialogoModificar({ reserva, abierto, alCambiar, alModificar }: Props) {
  return (
    <Dialog open={abierto} onOpenChange={alCambiar}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-2xl">
        {abierto && <Formulario reserva={reserva} alCerrar={() => alCambiar(false)} alModificar={alModificar} />}
      </DialogContent>
    </Dialog>
  )
}

function Formulario({
  reserva,
  alCerrar,
  alModificar,
}: {
  reserva: ReservaDetalleResponse
  alCerrar: () => void
  alModificar: (r: ModificacionReservaResponse) => void
}) {
  const inicial = aFormulario(reserva)
  const [form, setForm] = useState(inicial)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const modificar = useModificarReserva(reserva.codigo)
  const apartamentos = useTodosLosApartamentos()
  const apartamento = useApartamento(form.apartamento || undefined)
  const { reglas } = useReglasAlojamiento()
  const { temporadas } = useTemporadasAlojamiento()

  const capacidad = apartamento.data?.capacidadMaxima ?? null
  const dentroDeCapacidad = capacidad === null || form.ocupantes.length <= capacidad
  const noches = nochesEntre(form.entrada, form.salida)
  const avisoRP01 = avisoEstanciaMinima(form.entrada, form.salida, temporadas, reglas)
  const ocupantesValidos = form.ocupantes.every((o) => o.nombre.trim() && esFechaValida(o.fechaNacimiento))

  const cotizacion = useCotizacion(
    noches > 0 && !avisoRP01 && ocupantesValidos && dentroDeCapacidad
      ? {
          identificacionApartamento: form.apartamento,
          fechaEntrada: form.entrada,
          fechaSalida: form.salida,
          ocupantes: aOcupantes(form.ocupantes),
        }
      : null,
  )
  const diferenciaEstimada = cotizacion.data ? cotizacion.data.valorTotal - reserva.valorTotal : null

  const cambiarOcupante = (i: number, cambios: Partial<OcupanteFormulario>) =>
    setForm({ ...form, ocupantes: form.ocupantes.map((o, j) => (j === i ? { ...o, ...cambios } : o)) })

  const guardar = () => {
    const e: Record<string, string> = {}
    if (noches < 1) e.salida = 'La salida debe ser después de la llegada.'
    if (!dentroDeCapacidad) e.ocupantes = `El apartamento recibe máximo ${capacidad} personas.`
    if (!ocupantesValidos) e.ocupantes = 'Cada ocupante necesita nombre y fecha de nacimiento.'
    if (form.mascotas > 0) {
      const kg = Number(form.peso)
      if (!form.peso || Number.isNaN(kg) || kg <= 0) e.peso = 'Escribe el peso de la mascota más pesada.'
      else if (reglas.pesoMaximoMascotaKg !== null && kg > reglas.pesoMaximoMascotaKg) e.peso = `Máximo ${reglas.pesoMaximoMascotaKg} kg.`
    }
    setErrores(e)
    if (Object.keys(e).length > 0) return

    // Solo viajan los campos que cambiaron; el resto se conserva
    const datos: ModificarReservaRequest = {}
    if (form.apartamento !== inicial.apartamento) datos.identificacionApartamento = form.apartamento
    if (form.entrada !== inicial.entrada) datos.fechaEntrada = form.entrada
    if (form.salida !== inicial.salida) datos.fechaSalida = form.salida
    if (JSON.stringify(form.ocupantes) !== JSON.stringify(inicial.ocupantes)) datos.ocupantes = aOcupantes(form.ocupantes)
    if (form.mascotas !== inicial.mascotas || form.peso !== inicial.peso) {
      datos.cantidadMascotas = form.mascotas
      datos.pesoMascotaMasPesadaKg = form.mascotas > 0 ? Number(form.peso) : 0
    }
    if (Object.keys(datos).length === 0) {
      setErrores({ general: 'No hay cambios para guardar.' })
      return
    }
    modificar.mutate(datos, {
      onSuccess: (resultado) => {
        alModificar(resultado)
        alCerrar()
      },
    })
  }

  return (
    <>
      <DialogHeader>
        <DialogTitle className="font-titulo text-xl">Modificar {reserva.codigo}</DialogTitle>
        <DialogDescription>
          Se recalcula con las tarifas vigentes y la diferencia se asienta en el folio.
        </DialogDescription>
      </DialogHeader>

      <div className="flex flex-col gap-5">
        <Campo id="mod-apartamento" etiqueta="Apartamento">
          <select
            className={claseSelect}
            value={form.apartamento}
            onChange={(e) => setForm({ ...form, apartamento: e.target.value })}
          >
            {!apartamentos.data && <option value={form.apartamento}>{form.apartamento}</option>}
            {(apartamentos.data ?? []).map((a) => (
              <option key={a.identificacion} value={a.identificacion}>
                {a.identificacion} · {a.nombre} (hasta {a.capacidadMaxima})
              </option>
            ))}
          </select>
        </Campo>
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo id="mod-entrada" etiqueta="Llegada">
            <Input
              type="date"
              min={hoyISO()}
              value={form.entrada}
              onChange={(e) => {
                const v = e.target.value
                setForm({ ...form, entrada: v, salida: v && form.salida <= v ? sumarDias(v, 1) : form.salida })
              }}
            />
          </Campo>
          <Campo id="mod-salida" etiqueta="Salida" error={errores.salida}>
            <Input
              type="date"
              min={form.entrada ? sumarDias(form.entrada, 1) : undefined}
              value={form.salida}
              onChange={(e) => setForm({ ...form, salida: e.target.value })}
            />
          </Campo>
        </div>
        {avisoRP01 && <AvisoError mensaje={avisoRP01} />}

        <fieldset className="flex flex-col gap-3">
          <legend className="mb-1 text-sm font-medium">
            Ocupantes{' '}
            <span className="text-muted-foreground">
              · {form.ocupantes.length}
              {capacidad !== null && ` de ${capacidad}`}
            </span>
          </legend>
          {form.ocupantes.map((o, i) => (
            <div key={i} className="grid gap-2 sm:grid-cols-[1fr_10rem_auto] sm:items-center">
              <Input
                aria-label={`Nombre del ocupante ${i + 1}`}
                placeholder="Nombre"
                value={o.nombre}
                onChange={(e) => cambiarOcupante(i, { nombre: e.target.value })}
              />
              <Input
                type="date"
                aria-label={`Fecha de nacimiento del ocupante ${i + 1}`}
                max={hoyISO()}
                value={o.fechaNacimiento}
                onChange={(e) => cambiarOcupante(i, { fechaNacimiento: e.target.value })}
              />
              <div className="flex items-center justify-between gap-2">
                {o.fechaNacimiento && (
                  <EtiquetaPago fecha={o.fechaNacimiento} umbral={reglas.umbralEdad} referencia={form.entrada} />
                )}
                {i > 0 && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label={`Quitar al ocupante ${i + 1}`}
                    onClick={() => setForm({ ...form, ocupantes: form.ocupantes.filter((_, j) => j !== i) })}
                  >
                    <Trash2 aria-hidden="true" />
                  </Button>
                )}
              </div>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            className="w-fit"
            disabled={capacidad !== null && form.ocupantes.length >= capacidad}
            onClick={() =>
              setForm({
                ...form,
                ocupantes: [...form.ocupantes, { nombre: '', tipoDocumento: '', numeroDocumento: '', fechaNacimiento: '' }],
              })
            }
          >
            <Plus aria-hidden="true" /> Agregar ocupante
          </Button>
          {errores.ocupantes && <p className="text-sm font-medium text-error">{errores.ocupantes}</p>}
        </fieldset>

        <div className="grid gap-4 sm:grid-cols-2">
          <SelectorMascotas
            id="mod-mascotas"
            etiqueta="Mascotas"
            cantidad={form.mascotas}
            alCambiar={(n) => setForm({ ...form, mascotas: n })}
            maximo={reglas.maximoMascotas}
          />
          {form.mascotas > 0 && (
            <Campo id="mod-peso" etiqueta="Peso de la más pesada (kg)" error={errores.peso}>
              <Input
                type="number"
                min={1}
                max={reglas.pesoMaximoMascotaKg ?? undefined}
                value={form.peso}
                onChange={(e) => setForm({ ...form, peso: e.target.value })}
              />
            </Campo>
          )}
        </div>
        {reserva.cantidadMascotas === 0 && form.mascotas > 0 && (
          <p className="text-xs text-muted-foreground">
            <TextoCargo tipo="aseo" valor={reglas.cargoAseoMascota} />, una vez por estancia.
          </p>
        )}
        {reserva.cantidadMascotas > 0 && form.mascotas === 0 && (
          <p className="text-xs text-muted-foreground">El cargo de aseo se corrige con su movimiento inverso en el folio.</p>
        )}

        <div className="rounded-campo bg-muted/60 p-4 text-sm" aria-live="polite">
          {cotizacion.error ? (
            <AvisoError error={cotizacion.error} />
          ) : diferenciaEstimada === null ? (
            <p className="text-muted-foreground">Completa los datos para ver la diferencia.</p>
          ) : (
            <div className={cn('flex flex-col gap-1', cotizacion.isFetching && 'opacity-60')}>
              <p className="flex justify-between gap-3">
                <span>Valor actual</span>
                <Dinero valor={reserva.valorTotal} />
              </p>
              <p className="flex justify-between gap-3">
                <span>Nuevo valor</span>
                <Dinero valor={cotizacion.data?.valorTotal ?? 0} />
              </p>
              <p className="flex items-baseline justify-between gap-3 border-t border-border pt-2 font-semibold">
                <span>Diferencia estimada para el folio</span>
                <span className={cn(diferenciaEstimada < 0 && 'text-[#1C5E45] dark:text-[#8FD9B8]')}>
                  {diferenciaEstimada > 0 ? '+' : ''}
                  <Dinero valor={diferenciaEstimada} />
                </span>
              </p>
            </div>
          )}
        </div>

        {errores.general && <AvisoError mensaje={errores.general} />}
        {modificar.error && <AvisoError error={modificar.error} />}
      </div>

      <DialogFooter>
        <Button type="button" variant="outline" onClick={alCerrar} disabled={modificar.isPending}>
          Cancelar
        </Button>
        <Button type="button" onClick={guardar} disabled={modificar.isPending}>
          {modificar.isPending && <Loader2 aria-hidden="true" className="animate-spin" />}
          Guardar cambios
        </Button>
      </DialogFooter>
    </>
  )
}
