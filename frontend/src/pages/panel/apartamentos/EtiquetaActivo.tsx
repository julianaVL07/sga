import { Skeleton } from '@/components/ui/skeleton'

/** A la venta (activo) o inactivo; mientras carga, un esqueleto */
export function EtiquetaActivo({ activo }: { activo: boolean | undefined }) {
  if (activo === undefined) return <Skeleton className="ml-auto h-5 w-20 md:ml-0" />
  return activo ? (
    <span className="inline-flex rounded-full bg-[#DDF2EA] px-2.5 py-1 text-xs font-semibold whitespace-nowrap text-[#1C5E45]">
      A la venta
    </span>
  ) : (
    <span className="inline-flex rounded-full border border-border px-2.5 py-1 text-xs font-semibold whitespace-nowrap text-muted-foreground">
      Inactivo
    </span>
  )
}
