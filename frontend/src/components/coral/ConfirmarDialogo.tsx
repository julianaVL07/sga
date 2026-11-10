import type { ReactNode } from 'react'
import { Loader2 } from 'lucide-react'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'

interface Props {
  abierto: boolean
  alCambiar: (abierto: boolean) => void
  titulo: string
  descripcion?: ReactNode
  textoConfirmar?: string
  textoCancelar?: string
  /** Acción que no se puede deshacer: el botón se muestra en rojo */
  peligroso?: boolean
  /** Mientras se ejecuta la acción */
  cargando?: boolean
  alConfirmar: () => void
  /** Contenido extra, por ejemplo un AvisoError con el 409 */
  children?: ReactNode
}

/** Pide confirmación antes de una acción importante */
export function ConfirmarDialogo({
  abierto,
  alCambiar,
  titulo,
  descripcion,
  textoConfirmar = 'Confirmar',
  textoCancelar = 'Cancelar',
  peligroso = false,
  cargando = false,
  alConfirmar,
  children,
}: Props) {
  return (
    <AlertDialog open={abierto} onOpenChange={alCambiar}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle className="font-titulo text-xl">{titulo}</AlertDialogTitle>
          {descripcion && <AlertDialogDescription>{descripcion}</AlertDialogDescription>}
        </AlertDialogHeader>
        {children}
        <AlertDialogFooter>
          <AlertDialogCancel disabled={cargando}>{textoCancelar}</AlertDialogCancel>
          <Button
            variant={peligroso ? 'destructive' : 'default'}
            className={peligroso ? 'bg-error text-white hover:bg-error/90' : undefined}
            disabled={cargando}
            onClick={alConfirmar}
          >
            {cargando && <Loader2 aria-hidden="true" className="animate-spin" />}
            {textoConfirmar}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
