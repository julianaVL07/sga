import { ChevronDown, SlidersHorizontal } from 'lucide-react'
import { cn } from '@/lib/utils'

interface Props {
  id: string
  abierto: boolean
  alCambiar: (abierto: boolean) => void
  /** Filtros con valor, para mostrarlos en el botón cuando están plegados */
  activos: number
  /** Desde qué ancho los filtros se muestran siempre y el botón desaparece */
  desde: 'md' | 'lg'
  /** id del contenido que se pliega */
  controla: string
}

const OCULTAR_DESDE = { md: 'md:hidden', lg: 'lg:hidden' }
const MOSTRAR_DESDE = { md: 'hidden md:flex', lg: 'hidden lg:flex' }

/** "Filtros": en el celular es un botón que pliega el formulario; en pantallas grandes, solo el título */
export function EncabezadoFiltros({ id, abierto, alCambiar, activos, desde, controla }: Props) {
  const titulo = (
    <>
      <SlidersHorizontal aria-hidden="true" className="size-4 text-coral" /> Filtros
      {activos > 0 && (
        <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-semibold text-primary-foreground">{activos}</span>
      )}
    </>
  )
  return (
    <>
      <button
        type="button"
        id={id}
        aria-expanded={abierto}
        aria-controls={controla}
        onClick={() => alCambiar(!abierto)}
        className={cn('flex min-h-11 w-full items-center gap-2 text-left font-semibold', OCULTAR_DESDE[desde])}
      >
        {titulo}
        <ChevronDown aria-hidden="true" className={cn('ml-auto size-4 transition-transform', abierto && 'rotate-180')} />
      </button>
      <h2 className={cn('items-center gap-2 font-semibold', MOSTRAR_DESDE[desde])}>{titulo}</h2>
    </>
  )
}
