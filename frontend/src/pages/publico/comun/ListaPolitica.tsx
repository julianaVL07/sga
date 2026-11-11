import type { PoliticaCancelacionResponse } from '@/api/tipos'
import { cn } from '@/lib/utils'
import { tramosOrdenados } from './reglas'

interface Props {
  politica: PoliticaCancelacionResponse | undefined
  className?: string
}

function textoTramo(dias: number, siguiente: number | undefined): string {
  if (dias === 0) return siguiente !== undefined ? `Con menos de ${siguiente} días` : 'En cualquier momento'
  return `Con ${dias} días o más de antelación`
}

/** Tramos de la política de cancelación: cuánto se retiene según la antelación */
export function ListaPolitica({ politica, className }: Props) {
  const tramos = tramosOrdenados(politica)
  if (tramos.length === 0) return null
  return (
    <ul className={cn('flex flex-col gap-2 text-sm', className)}>
      {tramos.map((t, i) => (
        <li key={t.antelacionMinimaDias} className="flex items-center justify-between gap-3">
          <span>{textoTramo(t.antelacionMinimaDias, tramos[i - 1]?.antelacionMinimaDias)}</span>
          <span className="font-semibold tabular-nums whitespace-nowrap">
            {t.porcentajeRetencion === 0 ? 'Sin retención' : `Se retiene el ${t.porcentajeRetencion} %`}
          </span>
        </li>
      ))}
    </ul>
  )
}
