/** Controles nativos (select y textarea) con el mismo aspecto que el Input de shadcn */
const base =
  'w-full min-w-0 rounded-campo border border-input bg-card px-3.5 text-base text-foreground transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-sm dark:bg-input/30'

export const claseSelect = `${base} h-11`

export const claseTextarea = `${base} min-h-24 py-2.5`

/**
 * Contenido plegable de los filtros: en el celular se oculta salvo que esté abierto; desde el ancho dado siempre
 * se ve. Usa "contents" para que los campos sigan participando en la grilla del formulario.
 */
export function claseContenidoPlegable(abierto: boolean, desde: 'md' | 'lg'): string {
  if (abierto) return 'contents'
  return desde === 'md' ? 'hidden md:contents' : 'hidden lg:contents'
}

/** Casilla de verificación nativa con el color primario */
export const claseCasilla = 'size-5 shrink-0 cursor-pointer rounded accent-[var(--primary)] dark:[color-scheme:dark]'
