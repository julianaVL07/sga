import { Loader2 } from 'lucide-react'

/** Indicador mientras se descarga el código de una página */
export function CargandoPagina() {
  return (
    <div role="status" className="flex min-h-[50vh] items-center justify-center gap-2 text-muted-foreground">
      <Loader2 className="size-5 animate-spin" aria-hidden />
      <span className="text-sm">Cargando…</span>
    </div>
  )
}
