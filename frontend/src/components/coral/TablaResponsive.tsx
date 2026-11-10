import type { ReactNode } from 'react'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { cn } from '@/lib/utils'

export interface ColumnaTabla<T> {
  clave: string
  titulo: string
  celda: (fila: T) => ReactNode
  className?: string
  /** No mostrar esta columna dentro de la tarjeta del celular (por ejemplo, si ya va en el título) */
  ocultarEnTarjeta?: boolean
}

interface Props<T> {
  columnas: ColumnaTabla<T>[]
  filas: T[]
  claveFila: (fila: T) => string
  /** Encabezado de cada tarjeta en el celular */
  tituloTarjeta?: (fila: T) => ReactNode
  /** Acciones de la fila (botones), en la última columna o al pie de la tarjeta */
  acciones?: (fila: T) => ReactNode
  /** Texto para lectores de pantalla que describe la tabla */
  descripcion?: string
  className?: string
}

/** Tabla en el computador; tarjetas apiladas en el celular */
export function TablaResponsive<T>({
  columnas,
  filas,
  claveFila,
  tituloTarjeta,
  acciones,
  descripcion,
  className,
}: Props<T>) {
  return (
    <div className={className}>
      <div className="hidden md:block">
        <Table>
          {descripcion && <caption className="sr-only">{descripcion}</caption>}
          <TableHeader>
            <TableRow className="bg-muted/60 hover:bg-muted/60">
              {columnas.map((col) => (
                <TableHead key={col.clave} className={cn('h-11 px-4 text-xs font-semibold uppercase tracking-wide text-muted-foreground', col.className)}>
                  {col.titulo}
                </TableHead>
              ))}
              {acciones && (
                <TableHead className="h-11 px-4 text-right">
                  <span className="sr-only">Acciones</span>
                </TableHead>
              )}
            </TableRow>
          </TableHeader>
          <TableBody>
            {filas.map((fila) => (
              <TableRow key={claveFila(fila)} className="hover:bg-muted/40">
                {columnas.map((col) => (
                  <TableCell key={col.clave} className={cn('px-4 py-3', col.className)}>
                    {col.celda(fila)}
                  </TableCell>
                ))}
                {acciones && <TableCell className="px-4 py-3 text-right">{acciones(fila)}</TableCell>}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <ul className="flex flex-col gap-3 md:hidden" aria-label={descripcion}>
        {filas.map((fila) => (
          <li key={claveFila(fila)} className="rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta">
            {tituloTarjeta && <div className="mb-3 font-semibold">{tituloTarjeta(fila)}</div>}
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
              {columnas
                .filter((col) => !col.ocultarEnTarjeta)
                .map((col) => (
                  <div key={col.clave} className="contents">
                    <dt className="text-muted-foreground">{col.titulo}</dt>
                    <dd className="text-right">{col.celda(fila)}</dd>
                  </div>
                ))}
            </dl>
            {acciones && <div className="mt-4 flex flex-wrap justify-end gap-2">{acciones(fila)}</div>}
          </li>
        ))}
      </ul>
    </div>
  )
}
