import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

/** El backend devuelve 10 elementos por página */
export const TAMANO_PAGINA = 10

interface Props {
  /** Página actual, empieza en 0 como el backend */
  pagina: number
  totalPaginas: number
  totalElementos?: number
  alCambiar: (pagina: number) => void
  className?: string
}

export function Paginador({ pagina, totalPaginas, totalElementos, alCambiar, className }: Props) {
  if (totalPaginas <= 1) return null

  const esPrimera = pagina <= 0
  const esUltima = pagina >= totalPaginas - 1

  return (
    <nav aria-label="Paginación" className={cn('flex items-center justify-between gap-3', className)}>
      <p className="text-sm text-muted-foreground" aria-live="polite">
        Página {pagina + 1} de {totalPaginas}
        {totalElementos !== undefined && <span className="hidden sm:inline"> · {totalElementos} en total</span>}
      </p>
      <div className="flex gap-2">
        <Button
          variant="outline"
          size="icon"
          aria-label="Página anterior"
          disabled={esPrimera}
          onClick={() => alCambiar(pagina - 1)}
        >
          <ChevronLeft aria-hidden="true" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          aria-label="Página siguiente"
          disabled={esUltima}
          onClick={() => alCambiar(pagina + 1)}
        >
          <ChevronRight aria-hidden="true" />
        </Button>
      </div>
    </nav>
  )
}
