import { useState } from 'react'
import { toast } from 'sonner'
import { useAutorizarCierreFolio } from '@/api/folio'
import { Campo, Dinero } from '@/components/coral'
import { Input } from '@/components/ui/input'
import { claseTextarea } from '@/pages/publico/comun/estilos'
import { DialogoFormulario } from './DialogoFormulario'
import type { BaseDialogoFolio } from './DialogosFolio'
import { erroresDe } from './erroresDe'
import { useAlAbrir } from './useAlAbrir'

// ---------- Autorizar cierre con saldo (solo ADMINISTRADOR) ----------

interface PropsAutorizar extends BaseDialogoFolio {
  saldo: number
  /** Nombre del administrador que inició sesión */
  autorizadoPor: string
}

export function DialogoAutorizarCierre({ codigo, saldo, autorizadoPor, abierto, alCambiar }: PropsAutorizar) {
  const autorizar = useAutorizarCierreFolio(codigo)
  const [motivo, setMotivo] = useState('')
  const [error, setError] = useState<string>()

  useAlAbrir(abierto, () => {
    setMotivo('')
    setError(undefined)
    autorizar.reset()
  })

  const enviar = () => {
    if (!motivo.trim()) {
      setError('El motivo es obligatorio para cerrar con saldo')
      return
    }
    setError(undefined)
    autorizar.mutate(
      { autorizadoPor: `${autorizadoPor} (administración)`, motivo: motivo.trim() },
      {
        onSuccess: () => {
          toast.success('Folio cerrado con autorización')
          alCambiar(false)
        },
      },
    )
  }

  return (
    <DialogoFormulario
      id="form-autorizar"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Autorizar cierre con saldo"
      descripcion="El folio se cierra aunque tenga saldo. Queda registrado quién lo autorizó y por qué."
      textoEnviar="Autorizar y cerrar"
      cargando={autorizar.isPending}
      error={autorizar.error}
      tituloError="No se pudo autorizar el cierre"
      alEnviar={enviar}
    >
      <p className="flex items-baseline justify-between gap-3 rounded-campo bg-muted/60 px-4 py-3 text-sm">
        <span>{saldo < 0 ? 'Saldo a favor' : 'Saldo pendiente'}</span>
        <Dinero valor={Math.abs(saldo)} className="font-semibold" />
      </p>
      <Campo id="autorizar-por" etiqueta="Autoriza">
        <Input value={autorizadoPor} readOnly disabled />
      </Campo>
      <Campo id="autorizar-motivo" etiqueta="Motivo" error={error ?? erroresDe(autorizar.error).motivo}>
        <textarea
          className={claseTextarea}
          placeholder="Saldo de $5.000 por redondeo aceptado por el huésped"
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
        />
      </Campo>
    </DialogoFormulario>
  )
}
