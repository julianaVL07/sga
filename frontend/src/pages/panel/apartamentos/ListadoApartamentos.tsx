import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BedDouble, Plus } from 'lucide-react'
import { useDetallesApartamentos } from '@/api/apartamentos'
import { useApartamentos } from '@/api/publico'
import type { ApartamentoResponse } from '@/api/tipos'
import {
  AvisoError,
  EstadoOperativo,
  FotoApartamento,
  Paginador,
  TablaResponsive,
  Vacio,
  type ColumnaTabla,
} from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { MenuAccionesApartamento } from './AccionesApartamento'
import { useEsAdministrador } from './utilidades'
import { EtiquetaActivo } from './EtiquetaActivo'

export default function ListadoApartamentos() {
  const esAdmin = useEsAdministrador()
  const [pagina, setPagina] = useState(0)
  const consulta = useApartamentos(pagina)
  const filas = consulta.data?.contenido ?? []
  const detalles = useDetallesApartamentos(filas.map((a) => a.identificacion))
  const activo = (id: string) => detalles[filas.findIndex((a) => a.identificacion === id)]?.data?.activo

  const columnas: ColumnaTabla<ApartamentoResponse>[] = [
    {
      clave: 'apartamento',
      titulo: 'Apartamento',
      ocultarEnTarjeta: true,
      celda: (a) => (
        <Link to={`/panel/apartamentos/${encodeURIComponent(a.identificacion)}`} className="flex items-center gap-3 hover:underline">
          <FotoApartamento src={a.imagenes[0]} alt="" className="size-12 shrink-0 rounded-campo bg-crema" />
          <span className="flex flex-col">
            <span className="font-semibold">{a.identificacion}</span>
            <span className="text-xs text-muted-foreground">{a.nombre}</span>
          </span>
        </Link>
      ),
    },
    {
      clave: 'dormitorios',
      titulo: 'Dormitorios',
      className: 'md:text-center',
      celda: (a) => <span className="tabular-nums">{a.dormitorios}</span>,
    },
    {
      clave: 'capacidad',
      titulo: 'Capacidad',
      className: 'md:text-center',
      celda: (a) => <span className="tabular-nums">{a.capacidadMaxima}</span>,
    },
    {
      clave: 'estado',
      titulo: 'Estado',
      celda: (a) => <EstadoOperativo estado={a.estadoOperativo} />,
    },
    {
      clave: 'venta',
      titulo: 'Venta',
      celda: (a) => <EtiquetaActivo activo={activo(a.identificacion)} />,
    },
  ]

  return (
    <section className="mx-auto w-full max-w-6xl">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-titulo text-3xl font-semibold">Apartamentos</h1>
          {consulta.data && (
            <p className="text-sm text-muted-foreground">
              {consulta.data.totalElementos} {consulta.data.totalElementos === 1 ? 'apartamento' : 'apartamentos'} en el alojamiento
            </p>
          )}
        </div>
        {esAdmin && (
          <Button asChild>
            <Link to="/panel/apartamentos/nuevo">
              <Plus aria-hidden="true" /> Nuevo apartamento
            </Link>
          </Button>
        )}
      </div>

      <div className="mt-6 rounded-tarjeta border border-border bg-card p-3 shadow-tarjeta md:p-4">
        {consulta.error ? (
          <AvisoError error={consulta.error} />
        ) : !consulta.data ? (
          <div className="flex flex-col gap-2">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-16 w-full" />
            ))}
          </div>
        ) : filas.length === 0 ? (
          <Vacio
            icono={BedDouble}
            titulo="Todavía no hay apartamentos"
            descripcion={esAdmin ? 'Registra el primero; nace inactivo hasta que lo actives.' : undefined}
          />
        ) : (
          <>
            <TablaResponsive
              columnas={columnas}
              filas={filas}
              claveFila={(a) => a.identificacion}
              tituloTarjeta={(a) => (
                <Link
                  to={`/panel/apartamentos/${encodeURIComponent(a.identificacion)}`}
                  className="flex items-center gap-3 hover:underline"
                >
                  <FotoApartamento src={a.imagenes[0]} alt="" className="size-12 shrink-0 rounded-campo bg-crema" />
                  <span>
                    {a.identificacion} · {a.nombre}
                  </span>
                </Link>
              )}
              acciones={(a) => <MenuAccionesApartamento apartamento={a} />}
              descripcion="Apartamentos del alojamiento"
            />
            <Paginador
              className="mt-4"
              pagina={pagina}
              totalPaginas={consulta.data.totalPaginas}
              totalElementos={consulta.data.totalElementos}
              alCambiar={setPagina}
            />
          </>
        )}
      </div>
    </section>
  )
}
