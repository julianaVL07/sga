import type { FolioResponse, MedioPago, TipoCargo } from '@/api/reservas'
import { AvisoError, Dinero, Fecha } from '@/components/coral'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

const NOMBRE_TIPO: Record<TipoCargo, string> = {
  ALOJAMIENTO: 'Alojamiento',
  SERVICIO_ADICIONAL: 'Servicio adicional',
  AJUSTE_MODIFICACION: 'Ajuste',
  PENALIDAD_CANCELACION_NOSHOW: 'Penalidad',
}

const NOMBRE_MEDIO: Record<MedioPago, string> = {
  TARJETA_DEBITO_CREDITO: 'Tarjeta',
  TRANSFERENCIA_BANCARIA: 'Transferencia',
}

interface Props {
  folio: FolioResponse | undefined
  error: unknown
}

/** Cargos, pagos y saldo del folio, sin acciones */
export function FolioSoloLectura({ folio, error }: Props) {
  if (error) return <AvisoError error={error} />
  if (!folio) return <Skeleton className="h-40" />

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h3 className="text-sm font-semibold">Cargos</h3>
        {folio.cargos.length === 0 ? (
          <p className="mt-2 text-sm text-muted-foreground">Sin cargos.</p>
        ) : (
          <ul className="mt-2 flex flex-col divide-y divide-border text-sm">
            {folio.cargos.map((c, i) => (
              <li key={i} className="flex items-start justify-between gap-3 py-2">
                <span>
                  {c.concepto}
                  <span className="block text-xs text-muted-foreground">
                    {NOMBRE_TIPO[c.tipo]} · <Fecha valor={c.fecha} />
                  </span>
                </span>
                <Dinero valor={c.valor} className={cn(c.valor < 0 && 'text-[#1C5E45] dark:text-[#8FD9B8]')} />
              </li>
            ))}
          </ul>
        )}
      </div>
      <div>
        <h3 className="text-sm font-semibold">Pagos</h3>
        {folio.pagos.length === 0 ? (
          <p className="mt-2 text-sm text-muted-foreground">Todavía no hay pagos registrados.</p>
        ) : (
          <ul className="mt-2 flex flex-col divide-y divide-border text-sm">
            {folio.pagos.map((p, i) => (
              <li key={i} className="flex items-start justify-between gap-3 py-2">
                <span>
                  {NOMBRE_MEDIO[p.medio]}
                  <span className="block text-xs text-muted-foreground">
                    <Fecha valor={p.fecha} />
                  </span>
                </span>
                <Dinero valor={p.valor} />
              </li>
            ))}
          </ul>
        )}
      </div>
      <dl className="flex flex-col gap-2 rounded-campo bg-muted/60 p-4 text-sm">
        <div className="flex justify-between gap-3">
          <dt>Total de cargos</dt>
          <dd>
            <Dinero valor={folio.totalCargos} />
          </dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt>Total pagado</dt>
          <dd>
            <Dinero valor={folio.totalPagado} />
          </dd>
        </div>
        <div className="flex items-baseline justify-between gap-3 border-t border-border pt-2">
          <dt className="font-semibold">{folio.saldo < 0 ? 'Saldo a favor' : 'Saldo'}</dt>
          <dd>
            <Dinero valor={Math.abs(folio.saldo)} grande className="text-2xl" />
          </dd>
        </div>
        {folio.cerrado && <p className="text-xs text-muted-foreground">Folio cerrado.</p>}
      </dl>
    </div>
  )
}
