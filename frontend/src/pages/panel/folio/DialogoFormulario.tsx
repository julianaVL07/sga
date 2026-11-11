import type { FormEvent, ReactNode } from 'react'
import { Loader2 } from 'lucide-react'
import { AvisoError } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'

interface Props {
  /** id del formulario: enlaza el botón de enviar del pie */
  id: string
  abierto: boolean
  alCambiar: (abierto: boolean) => void
  titulo: string
  descripcion?: ReactNode
  textoEnviar: string
  cargando?: boolean
  /** Error de la mutación (409 de negocio o 400 sin campos), en un AvisoError */
  error?: unknown
  tituloError?: string
  peligroso?: boolean
  className?: string
  alEnviar: () => void
  children: ReactNode
}

/** Diálogo con un formulario: campos, el aviso del 409 y los botones Cancelar y Enviar */
export function DialogoFormulario({
  id,
  abierto,
  alCambiar,
  titulo,
  descripcion,
  textoEnviar,
  cargando = false,
  error,
  tituloError,
  peligroso = false,
  className,
  alEnviar,
  children,
}: Props) {
  const enviar = (evento: FormEvent) => {
    evento.preventDefault()
    if (!cargando) alEnviar()
  }

  return (
    <Dialog open={abierto} onOpenChange={(valor) => !cargando && alCambiar(valor)}>
      <DialogContent className={cn('max-h-[90dvh] overflow-y-auto sm:max-w-md', className)}>
        <DialogHeader>
          <DialogTitle className="font-titulo text-xl">{titulo}</DialogTitle>
          {descripcion && <DialogDescription>{descripcion}</DialogDescription>}
        </DialogHeader>
        <form id={id} noValidate onSubmit={enviar} className="flex flex-col gap-4">
          {children}
          {!!error && <AvisoError titulo={tituloError} error={error} />}
        </form>
        <DialogFooter>
          <Button variant="outline" onClick={() => alCambiar(false)} disabled={cargando}>
            Cancelar
          </Button>
          <Button
            type="submit"
            form={id}
            disabled={cargando}
            variant={peligroso ? 'destructive' : 'default'}
            className={peligroso ? 'bg-error text-white hover:bg-error/90' : undefined}
          >
            {cargando && <Loader2 aria-hidden="true" className="animate-spin" />}
            {textoEnviar}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

