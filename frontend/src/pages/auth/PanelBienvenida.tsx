import { Logo, Onda } from '@/components/coral'

/** Panel ciruela con ondas del login y el registro; en el celular es una franja arriba */
export function PanelBienvenida() {
  return (
    <div className="relative flex flex-col justify-center overflow-hidden bg-ciruela px-6 pt-6 pb-14 text-ciruela-texto md:px-10 md:py-16">
      <div className="relative z-10 flex flex-col gap-3 md:gap-8">
        <Logo claro conSubtitulo tamano="md" className="md:hidden" />
        <Logo claro conSubtitulo tamano="lg" className="hidden md:inline-flex" />
        <p className="font-titulo text-2xl leading-tight font-semibold text-white md:text-4xl">
          Encuentra tu lugar ideal.
        </p>
        <p className="hidden max-w-xs text-sm text-ciruela-texto/80 md:block">
          Apartamentos frente al mar en Bocagrande, Cartagena.
        </p>
      </div>
      <Onda className="absolute inset-x-0 bottom-0 h-16 md:h-40" />
    </div>
  )
}
