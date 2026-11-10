import { cloneElement, isValidElement, type ReactElement, type ReactNode } from 'react'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

interface Props {
  /** id del control; enlaza el label y el mensaje de error */
  id: string
  etiqueta: string
  error?: string
  ayuda?: ReactNode
  className?: string
  /** El control (Input, select...). Recibe id, aria-invalid y aria-describedby. */
  children: ReactElement<Record<string, unknown>>
}

/** Campo de formulario con label, ayuda y mensaje de error accesibles */
export function Campo({ id, etiqueta, error, ayuda, className, children }: Props) {
  const idAyuda = ayuda ? `${id}-ayuda` : undefined
  const idError = error ? `${id}-error` : undefined
  const describedBy = [idAyuda, idError].filter(Boolean).join(' ') || undefined

  const control = isValidElement(children)
    ? cloneElement(children, { id, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })
    : children

  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <Label htmlFor={id} className="text-sm font-medium">
        {etiqueta}
      </Label>
      {control}
      {ayuda && (
        <p id={idAyuda} className="text-xs text-muted-foreground">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={idError} className="text-xs font-medium text-error">
          {error}
        </p>
      )}
    </div>
  )
}
