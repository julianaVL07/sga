/*
 * Clases para los controles nativos (select y textarea) con el mismo aspecto que el Input de shadcn.
 * Se usan en recepción, calendario, apartamentos y servicio.
 */

const base =
  'w-full min-w-0 rounded-campo border border-input bg-card px-3.5 text-base text-foreground transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-sm'

export const claseSelect = `${base} h-11`

export const claseTextarea = `${base} min-h-28 py-2.5`
