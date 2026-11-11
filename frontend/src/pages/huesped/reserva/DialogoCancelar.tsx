import { useState } from 'react'
import { usePoliticaVigente } from '@/api/publico'
import { useCancelarReserva, type ReservaDetalleResponse, type RetencionResponse } from '@/api/reservas'
import { AvisoError, ConfirmarDialogo, Dinero } from '@/components/coral'
import { Label } from '@/components/ui/label'
import { claseTextarea } from '@/pages/publico/comun/estilos'
import { retencionEstimada } from '@/pages/publico/comun/reglas'

interface Props {
  reserva: ReservaDetalleResponse
  abierto: boolean
  alCambiar: (abierto: boolean) => void
  alCancelar: (resultado: RetencionResponse) => void
}

/** Pide el motivo y muestra la retención estimada antes de cancelar */
export function DialogoCancelar({ reserva, abierto, alCambiar, alCancelar }: Props) {
  const politica = usePoliticaVigente()
  const cancelar = useCancelarReserva(reserva.codigo)
  const [motivo, setMotivo] = useState('')
  const [errorMotivo, setErrorMotivo] = useState<string>()

  const estimada = retencionEstimada(politica.data, reserva.valorTotal, reserva.fechaEntrada)
  const otraVersion = politica.data && politica.data.version !== reserva.versionPoliticaCancelacion

  const confirmar = () => {
    if (!motivo.trim()) {
      setErrorMotivo('Escribe el motivo de la cancelación.')
      return
    }
    setErrorMotivo(undefined)
    cancelar.mutate(motivo.trim(), {
      onSuccess: (resultado) => {
        alCambiar(false)
        setMotivo('')
        alCancelar(resultado)
      },
    })
  }

  return (
    <ConfirmarDialogo
      abierto={abierto}
      alCambiar={(a) => {
        if (!a) cancelar.reset()
        alCambiar(a)
      }}
      titulo={`Cancelar ${reserva.codigo}`}
      descripcion="La reserva se cancela, las noches quedan libres y no se puede deshacer."
      textoConfirmar="Cancelar reserva"
      textoCancelar="Volver"
      peligroso
      cargando={cancelar.isPending}
      alConfirmar={confirmar}
    >
      <div className="flex flex-col gap-4">
        <div className="rounded-campo bg-muted/60 p-4 text-sm">
          {estimada ? (
            <>
              <p>
                Faltan <strong>{estimada.dias}</strong> {estimada.dias === 1 ? 'día' : 'días'} para la llegada. Según la
                política, se retiene el <strong>{estimada.porcentaje} %</strong> de la estancia.
              </p>
              <p className="mt-2 flex items-baseline justify-between gap-3 font-semibold">
                <span>Retención estimada</span>
                <Dinero valor={estimada.valor} grande className="text-2xl" />
              </p>
              {otraVersion && (
                <p className="mt-2 text-xs text-muted-foreground">
                  La reserva tiene congelada la versión {reserva.versionPoliticaCancelacion} de la política; el valor final
                  lo calcula el sistema con esa versión.
                </p>
              )}
            </>
          ) : politica.error ? (
            <AvisoError error={politica.error} />
          ) : (
            <p>Calculando la retención…</p>
          )}
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="cancelar-motivo">Motivo</Label>
          <textarea
            id="cancelar-motivo"
            className={claseTextarea}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            aria-invalid={errorMotivo ? true : undefined}
            aria-describedby={errorMotivo ? 'cancelar-motivo-error' : undefined}
            placeholder="Por ejemplo: cambio de planes"
          />
          {errorMotivo && (
            <p id="cancelar-motivo-error" className="text-xs font-medium text-error">
              {errorMotivo}
            </p>
          )}
        </div>
        {cancelar.error && <AvisoError error={cancelar.error} />}
      </div>
    </ConfirmarDialogo>
  )
}
