import { useState } from 'react'
import { toast } from 'sonner'
import { useRegistrarCargoServicio, useRegistrarCorreccion, useRegistrarPago } from '@/api/folio'
import type { MedioPago } from '@/api/reservas'
import type { ServicioAdicionalResponse } from '@/api/tipos'
import { AvisoError, Campo, Dinero, formatearDinero } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { aPesos } from '@/pages/panel/apartamentos/utilidades'
import { claseSelect, claseTextarea } from '@/pages/publico/comun/estilos'
import { DialogoFormulario } from './DialogoFormulario'
import { erroresDe } from './erroresDe'
import { NOMBRE_MEDIO, type Movimiento } from './movimientos'
import { useAlAbrir } from './useAlAbrir'

export interface BaseDialogoFolio {
  codigo: string
  abierto: boolean
  alCambiar: (abierto: boolean) => void
}

// ---------- Registrar pago ----------

const MEDIOS: MedioPago[] = ['TARJETA_DEBITO_CREDITO', 'TRANSFERENCIA_BANCARIA']

export function DialogoPago({ codigo, saldo, abierto, alCambiar }: BaseDialogoFolio & { saldo: number }) {
  const pago = useRegistrarPago(codigo)
  const [valor, setValor] = useState('')
  const [medio, setMedio] = useState<MedioPago>('TRANSFERENCIA_BANCARIA')
  const [error, setError] = useState<string>()

  useAlAbrir(abierto, () => {
    setValor('')
    setMedio('TRANSFERENCIA_BANCARIA')
    setError(undefined)
    pago.reset()
  })

  const enviar = () => {
    const pesos = aPesos(valor)
    if (!Number.isInteger(pesos) || pesos <= 0) {
      setError('Escribe un valor mayor que cero, sin decimales')
      return
    }
    setError(undefined)
    pago.mutate(
      { valor: pesos, medio },
      {
        onSuccess: (folio) => {
          toast.success(`Pago de ${formatearDinero(pesos)} registrado`, {
            description: `Nuevo saldo: ${formatearDinero(folio.saldo)}`,
          })
          alCambiar(false)
        },
      },
    )
  }

  return (
    <DialogoFormulario
      id="form-pago"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Registrar pago"
      descripcion="Se admiten pagos parciales. Un pago registrado no se edita ni se borra."
      textoEnviar="Registrar pago"
      cargando={pago.isPending}
      error={pago.error}
      tituloError="No se pudo registrar el pago"
      alEnviar={enviar}
    >
      <Campo
        id="pago-valor"
        etiqueta="Valor en pesos"
        error={error ?? erroresDe(pago.error).valor}
        ayuda={
          saldo > 0 ? (
            <>
              Saldo pendiente: <Dinero valor={saldo} />.{' '}
              <Button type="button" variant="link" className="h-auto p-0 text-xs" onClick={() => setValor(String(saldo))}>
                Usar el saldo
              </Button>
            </>
          ) : undefined
        }
      >
        <Input inputMode="numeric" placeholder="310000" value={valor} onChange={(e) => setValor(e.target.value)} />
      </Campo>
      <Campo id="pago-medio" etiqueta="Medio de pago">
        <select className={claseSelect} value={medio} onChange={(e) => setMedio(e.target.value as MedioPago)}>
          {MEDIOS.map((m) => (
            <option key={m} value={m}>
              {NOMBRE_MEDIO[m]}
            </option>
          ))}
        </select>
      </Campo>
    </DialogoFormulario>
  )
}

// ---------- Cargar servicio adicional ----------

interface PropsServicio extends BaseDialogoFolio {
  servicios: ServicioAdicionalResponse[] | undefined
  errorServicios: unknown
}

export function DialogoServicio({ codigo, servicios, errorServicios, abierto, alCambiar }: PropsServicio) {
  const cargo = useRegistrarCargoServicio(codigo)
  const disponibles = (servicios ?? []).filter((s) => s.activo && s.generaCargo)
  const [idServicio, setIdServicio] = useState('')
  const [error, setError] = useState<string>()

  useAlAbrir(abierto, () => {
    setIdServicio('')
    setError(undefined)
    cargo.reset()
  })

  const elegido = disponibles.find((s) => s.id === idServicio)

  const enviar = () => {
    if (!elegido) {
      setError('Elige un servicio')
      return
    }
    setError(undefined)
    cargo.mutate(elegido.id, {
      onSuccess: () => {
        toast.success('Servicio cargado al folio', { description: `${elegido.nombre}: ${formatearDinero(elegido.valor)}` })
        alCambiar(false)
      },
    })
  }

  return (
    <DialogoFormulario
      id="form-servicio"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Cargar servicio adicional"
      descripcion="Se asienta con el valor del servicio y la fecha de hoy. Si después hay que quitarlo, se corrige con un movimiento inverso."
      textoEnviar="Cargar al folio"
      cargando={cargo.isPending}
      error={cargo.error}
      tituloError="No se pudo cargar el servicio"
      alEnviar={enviar}
    >
      {errorServicios ? (
        <AvisoError titulo="No se pudieron consultar los servicios" error={errorServicios} />
      ) : !servicios ? (
        <p className="text-sm text-muted-foreground">Cargando servicios…</p>
      ) : disponibles.length === 0 ? (
        <AvisoError mensaje="El alojamiento no tiene servicios activos que generen cargo." />
      ) : (
        <>
          <Campo id="servicio-id" etiqueta="Servicio" error={error}>
            <select className={claseSelect} value={idServicio} onChange={(e) => setIdServicio(e.target.value)}>
              <option value="">Elige un servicio</option>
              {disponibles.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nombre} · {formatearDinero(s.valor)}
                </option>
              ))}
            </select>
          </Campo>
          {elegido?.descripcion && <p className="-mt-2 text-sm text-muted-foreground">{elegido.descripcion}</p>}
        </>
      )}
    </DialogoFormulario>
  )
}

// ---------- Corrección de un cargo ----------

interface PropsCorreccion extends BaseDialogoFolio {
  /** Cargos que se pueden corregir */
  cargos: Movimiento[]
  /** Cargo elegido desde la tabla; null para elegirlo en el diálogo */
  indiceInicial: number | null
}

export function DialogoCorreccion({ codigo, cargos, indiceInicial, abierto, alCambiar }: PropsCorreccion) {
  const correccion = useRegistrarCorreccion(codigo)
  const [indice, setIndice] = useState('')
  const [motivo, setMotivo] = useState('')
  const [errores, setErrores] = useState<{ indice?: string; motivo?: string }>({})

  useAlAbrir(abierto, () => {
    setIndice(indiceInicial !== null ? String(indiceInicial) : '')
    setMotivo('')
    setErrores({})
    correccion.reset()
  })

  const elegido = cargos.find((c) => String(c.indiceCargo) === indice)

  const enviar = () => {
    const nuevos = {
      indice: elegido ? undefined : 'Elige el cargo que vas a corregir',
      motivo: motivo.trim() ? undefined : 'Escribe el motivo de la corrección',
    }
    setErrores(nuevos)
    if (nuevos.indice || nuevos.motivo || !elegido) return
    correccion.mutate(
      { indiceCargo: elegido.indiceCargo as number, motivo: motivo.trim() },
      {
        onSuccess: () => {
          toast.success(`Corrección de ${elegido.numero} registrada`)
          alCambiar(false)
        },
      },
    )
  }

  return (
    <DialogoFormulario
      id="form-correccion"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Registrar corrección"
      descripcion={
        <>
          Un cargo del folio <strong>no se edita ni se borra</strong>. La corrección asienta un movimiento inverso: un
          cargo del mismo tipo con el valor opuesto, el motivo y la fecha de hoy. Los dos quedan en el folio y se
          anulan entre sí.
        </>
      }
      textoEnviar="Asentar movimiento inverso"
      cargando={correccion.isPending}
      error={correccion.error}
      tituloError="No se pudo registrar la corrección"
      alEnviar={enviar}
    >
      <Campo id="correccion-cargo" etiqueta="Cargo a corregir" error={errores.indice}>
        <select className={claseSelect} value={indice} onChange={(e) => setIndice(e.target.value)}>
          <option value="">Elige un cargo</option>
          {cargos.map((c) => (
            <option key={c.clave} value={String(c.indiceCargo)}>
              {c.numero} · {c.concepto} · {formatearDinero(c.valor)}
            </option>
          ))}
        </select>
      </Campo>
      {elegido && (
        <p className="flex items-baseline justify-between gap-3 rounded-campo bg-muted/60 px-4 py-3 text-sm">
          <span>Movimiento inverso</span>
          <Dinero valor={-elegido.valor} className="font-semibold" />
        </p>
      )}
      <Campo id="correccion-motivo" etiqueta="Motivo" error={errores.motivo ?? erroresDe(correccion.error).motivo}>
        <textarea
          className={claseTextarea}
          placeholder="La cena no se consumió"
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
        />
      </Campo>
    </DialogoFormulario>
  )
}
