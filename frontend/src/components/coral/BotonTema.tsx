import { Moon, Sun } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useTema } from '@/lib/tema'
import { cn } from '@/lib/utils'

interface Props {
  className?: string
}

/** Alterna entre modo claro y oscuro */
export function BotonTema({ className }: Props) {
  const { tema, alternarTema } = useTema()
  const oscuro = tema === 'dark'
  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={alternarTema}
      aria-label={oscuro ? 'Activar modo claro' : 'Activar modo oscuro'}
      aria-pressed={oscuro}
      className={cn(className)}
    >
      {oscuro ? <Sun aria-hidden="true" /> : <Moon aria-hidden="true" />}
    </Button>
  )
}
