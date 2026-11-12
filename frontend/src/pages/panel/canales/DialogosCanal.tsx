import { useState } from 'react'
import { Check, Copy, KeyRound, TriangleAlert } from 'lucide-react'
import { toast } from 'sonner'
import {
  useCrearCanal,
  useResolverConflictoCanal,
  type ConflictoCanalResponse,
  type CredencialCanalResponse,
} from '@/api/canales'
import { Campo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { DialogoFormulario } from '@/pages/panel/folio/DialogoFormulario'
import { erroresDe } from '@/pages/panel/folio/erroresDe'
import { useAlAbrir } from '@/pages/panel/folio/useAlAbrir'
import { claseTextarea } from '@/pages/publico/comun/estilos'

interface Base {
  abierto: boolean
  alCambiar: (abierto: boolean) => void
}

// ---------- Crear canal ----------

const FORMATO_NOMBRE = /^[a-z0-9][a-z0-9-]{1,39}$/

export function DialogoCrearCanal({
  abierto,
  alCambiar,
  alCrear,
}: Base & { alCrear: (credencial: CredencialCanalResponse) => void }) {
  const crear = useCrearCanal()
  const [nombre, setNombre] = useState('')
  const [aviso, setAviso] = useState<string>()

  useAlAbrir(abierto, () => {
    setNombre('')
    setAviso(undefined)
    crear.reset()
  })

  const enviar = () => {
    const valor = nombre.trim().toLowerCase()
    if (!FORMATO_NOMBRE.test(valor)) {
      setAviso('De 2 a 40 caracteres entre minúsculas, números y guiones; empieza por letra o número.')
      return
    }
    setAviso(undefined)
    crear.mutate(valor, {
      onSuccess: (respuesta) => {
        alCambiar(false)
        alCrear(respuesta)
      },
    })
  }

  return (
    <DialogoFormulario
      id="form-canal"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Nuevo canal de venta"
      descripcion="Plataforma externa autorizada a vender el inventario. El SGA genera su credencial al crearla."
      textoEnviar="Crear canal"
      cargando={crear.isPending}
      error={crear.error}
      tituloError="No se pudo crear el canal"
      alEnviar={enviar}
    >
      <Campo
        id="canal-nombre"
        etiqueta="Nombre"
        error={aviso ?? erroresDe(crear.error).nombre}
        ayuda="Va en la ruta de la API: /api/canales/{nombre}/... Por ejemplo, expedia."
      >
        <Input autoComplete="off" maxLength={40} value={nombre} onChange={(e) => setNombre(e.target.value)} />
      </Campo>
    </DialogoFormulario>
  )
}

// ---------- Credencial (una sola vez) ----------

/** Muestra la credencial recién emitida. Al cerrar se descarta: el SGA no la puede volver a mostrar. */
export function DialogoCredencial({
  credencial,
  alCerrar,
}: {
  credencial: CredencialCanalResponse | null
  alCerrar: () => void
}) {
  const [copiada, setCopiada] = useState(false)
  const [anterior, setAnterior] = useState(credencial)
  if (credencial !== anterior) {
    setAnterior(credencial)
    setCopiada(false)
  }

  const copiar = async () => {
    if (!credencial) return
    try {
      await navigator.clipboard.writeText(credencial.credencial)
      setCopiada(true)
      toast.success('Credencial copiada')
    } catch {
      toast.error('No se pudo copiar', { description: 'Selecciona el texto y cópialo a mano.' })
    }
  }

  return (
    <Dialog open={credencial !== null} onOpenChange={(abierto) => !abierto && alCerrar()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 font-titulo text-xl">
            <KeyRound aria-hidden="true" className="size-5 text-coral" />
            Credencial de {credencial?.nombre}
          </DialogTitle>
          <DialogDescription>
            El canal la envía en el encabezado <code className="font-mono text-xs">X-Canal-Credencial</code> de cada
            petición.
          </DialogDescription>
        </DialogHeader>

        <div
          role="alert"
          className="flex gap-3 rounded-campo border px-4 py-3 text-sm"
          style={{ backgroundColor: '#FFF1C2', borderColor: '#F0D27A', color: '#6B4800' }}
        >
          <TriangleAlert aria-hidden="true" className="mt-0.5 size-5 shrink-0" />
          <p className="font-semibold">Guárdala, no se volverá a mostrar</p>
        </div>

        <div className="flex gap-2">
          <Input
            readOnly
            aria-label="Credencial del canal"
            value={credencial?.credencial ?? ''}
            onFocus={(e) => e.currentTarget.select()}
            className="font-mono text-sm"
          />
          <Button type="button" variant="outline" onClick={copiar} className="shrink-0">
            {copiada ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
            {copiada ? 'Copiada' : 'Copiar'}
          </Button>
        </div>

        <DialogFooter>
          <Button onClick={alCerrar}>Ya la guardé</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ---------- Resolver conflicto ----------

export function DialogoResolverConflicto({
  conflicto,
  abierto,
  alCambiar,
}: Base & { conflicto: ConflictoCanalResponse | null }) {
  const resolver = useResolverConflictoCanal()
  const [motivo, setMotivo] = useState('')
  const [aviso, setAviso] = useState<string>()

  useAlAbrir(abierto, () => {
    setMotivo('')
    setAviso(undefined)
    resolver.reset()
  })

  if (!conflicto) return null

  const enviar = () => {
    if (!motivo.trim()) {
      setAviso('Escribe el motivo de la resolución')
      return
    }
    setAviso(undefined)
    resolver.mutate(
      { id: conflicto.id, motivo: motivo.trim() },
      {
        onSuccess: () => {
          toast.success('Conflicto resuelto', { description: `${conflicto.canal} · ${conflicto.identificadorExterno}` })
          alCambiar(false)
        },
      },
    )
  }

  return (
    <DialogoFormulario
      id="form-resolver-conflicto"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Resolver conflicto"
      descripcion={
        <>
          Reserva <strong>{conflicto.identificadorExterno}</strong> de {conflicto.canal}. Resolverlo no cambia la
          reserva vigente: solo deja constancia de cómo se atendió.
        </>
      }
      textoEnviar="Marcar como resuelto"
      cargando={resolver.isPending}
      error={resolver.error}
      tituloError="No se pudo resolver el conflicto"
      className="sm:max-w-lg"
      alEnviar={enviar}
    >
      <p className="rounded-campo bg-muted/60 px-3 py-2 text-sm">{conflicto.detalle}</p>
      <Campo id="conflicto-motivo" etiqueta="Motivo" error={aviso ?? erroresDe(resolver.error).motivo}>
        <textarea
          className={claseTextarea}
          maxLength={1000}
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
          placeholder="Por ejemplo: se avisó al canal y el huésped eligió otras fechas"
        />
      </Campo>
    </DialogoFormulario>
  )
}
