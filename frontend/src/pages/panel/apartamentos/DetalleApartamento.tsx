import { useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  Ban,
  BedDouble,
  CalendarX2,
  CircleDollarSign,
  Pencil,
  Power,
  ScrollText,
  Users,
  Wrench,
} from 'lucide-react'
import { useHistoricoTarifas, useTodasLasTemporadas } from '@/api/apartamentos'
import { esErrorApi } from '@/api/cliente'
import { useApartamento } from '@/api/publico'
import type { ApartamentoDetalleResponse, TemporadaResponse } from '@/api/tipos'
import {
  AvisoError,
  Dinero,
  EstadoOperativo,
  Fecha,
  FotoApartamento,
  Paginador,
  TablaResponsive,
  Vacio,
} from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import {
  DialogoActivar,
  DialogoBloqueo,
  DialogoRetirar,
  DialogoServicio,
} from './AccionesApartamento'
import { DialogoTarifas } from './DialogoTarifas'
import { EtiquetaActivo } from './EtiquetaActivo'
import { useEsAdministrador } from './utilidades'

type Dialogo = 'tarifas' | 'activar' | 'bloqueo' | 'servicio' | 'retirar' | null

export default function DetalleApartamento() {
  const { id } = useParams()
  const navigate = useNavigate()
  const esAdmin = useEsAdministrador()
  const consulta = useApartamento(id)
  const temporadas = useTodasLasTemporadas()
  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const abrir = (d: Exclude<Dialogo, null>) => ({
    abierto: dialogo === d,
    alCambiar: (abierto: boolean) => setDialogo(abierto ? d : null),
  })

  if (consulta.error) {
    return (
      <section className="mx-auto w-full max-w-5xl">
        <Volver />
        {esErrorApi(consulta.error) && consulta.error.status === 404 ? (
          <Vacio
            icono={BedDouble}
            titulo="No encontramos este apartamento"
            descripcion="No existe o fue retirado de la venta."
          />
        ) : (
          <AvisoError className="mt-4" error={consulta.error} />
        )}
      </section>
    )
  }

  const a = consulta.data
  if (!a) {
    return (
      <section className="mx-auto flex w-full max-w-5xl flex-col gap-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-56 w-full" />
        <Skeleton className="h-40 w-full" />
      </section>
    )
  }

  const listaTemporadas = temporadas.data ?? []
  const estaFuera = a.estadoOperativo === 'FUERA_DE_SERVICIO'
  const ruta = `/panel/apartamentos/${encodeURIComponent(a.identificacion)}`

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-5">
      <Volver />

      {/* Encabezado */}
      <header className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
          <h1 className="font-titulo text-3xl font-semibold">{a.nombre}</h1>
          <span className="text-lg text-muted-foreground">{a.identificacion}</span>
          <EstadoOperativo estado={a.estadoOperativo} />
          <EtiquetaActivo activo={a.activo} />
        </div>

        {esAdmin && (
          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline" size="sm">
              <Link to={`${ruta}/editar`}>
                <Pencil aria-hidden="true" /> Editar
              </Link>
            </Button>
            <Button variant="outline" size="sm" onClick={() => setDialogo('tarifas')}>
              <CircleDollarSign aria-hidden="true" /> Definir tarifas
            </Button>
            {!a.activo && (
              <Button size="sm" onClick={() => setDialogo('activar')}>
                <Power aria-hidden="true" /> Activar
              </Button>
            )}
            <Button variant="outline" size="sm" onClick={() => setDialogo('bloqueo')}>
              <CalendarX2 aria-hidden="true" /> Registrar bloqueo
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={a.estadoOperativo === 'OCUPADO'}
              title={a.estadoOperativo === 'OCUPADO' ? 'Está ocupado' : undefined}
              onClick={() => setDialogo('servicio')}
            >
              {estaFuera ? <Power aria-hidden="true" /> : <Wrench aria-hidden="true" />}
              {estaFuera ? 'Restablecer servicio' : 'Poner fuera de servicio'}
            </Button>
            <Button asChild variant="outline" size="sm">
              <Link to={`${ruta}/novedades`}>
                <ScrollText aria-hidden="true" /> Novedades
              </Link>
            </Button>
            <Button variant="ghost" size="sm" className="text-error" onClick={() => setDialogo('retirar')}>
              <Ban aria-hidden="true" /> Retirar de la venta
            </Button>
          </div>
        )}
      </header>

      {/* Imágenes */}
      <Galeria apartamento={a} />

      <div className="grid gap-5 lg:grid-cols-[1fr_1fr]">
        {/* Datos y dotación */}
        <Tarjeta titulo="Datos">
          {a.descripcion && <p className="text-sm">{a.descripcion}</p>}
          <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
            <Dato icono={<BedDouble aria-hidden="true" className="size-4" />} etiqueta="Dormitorios" valor={a.dormitorios} />
            <Dato icono={<Users aria-hidden="true" className="size-4" />} etiqueta="Capacidad" valor={`${a.capacidadMaxima} ${a.capacidadMaxima === 1 ? 'persona' : 'personas'}`} />
          </dl>
          <h3 className="mt-5 text-sm font-semibold">Dotación</h3>
          <ul className="mt-2 flex flex-wrap gap-2">
            {a.dotacion.map((d) => (
              <li key={d} className="rounded-full bg-muted px-3 py-1 text-xs">
                {d}
              </li>
            ))}
          </ul>
        </Tarjeta>

        {/* Tarifas vigentes */}
        <Tarjeta titulo="Tarifas vigentes" descripcion="Por ocupante facturable y por noche">
          <TarifasVigentes apartamento={a} temporadas={listaTemporadas} />
        </Tarjeta>
      </div>

      {/* Bloqueos vigentes */}
      <Tarjeta titulo="Bloqueos vigentes">
        {a.bloqueosVigentes.length === 0 ? (
          <p className="text-sm text-muted-foreground">No tiene bloqueos vigentes.</p>
        ) : (
          <TablaResponsive
            columnas={[
              { clave: 'desde', titulo: 'Primera noche', celda: (b) => <Fecha valor={b.fechaInicio} /> },
              { clave: 'hasta', titulo: 'Vuelve a la venta', celda: (b) => <Fecha valor={b.fechaFin} /> },
              { clave: 'motivo', titulo: 'Motivo', celda: (b) => b.motivo },
            ]}
            filas={[...a.bloqueosVigentes].sort((x, y) => x.fechaInicio.localeCompare(y.fechaInicio))}
            claveFila={(b) => b.id}
            descripcion="Bloqueos vigentes"
          />
        )}
      </Tarjeta>

      {/* Histórico de tarifas: solo administración */}
      {esAdmin && <HistoricoTarifas identificacion={a.identificacion} temporadas={listaTemporadas} />}

      {esAdmin && (
        <>
          <DialogoTarifas
            identificacion={a.identificacion}
            temporadas={listaTemporadas}
            vigentes={a.tarifas}
            {...abrir('tarifas')}
          />
          <DialogoActivar apartamento={a} {...abrir('activar')} />
          <DialogoBloqueo apartamento={a} {...abrir('bloqueo')} />
          <DialogoServicio apartamento={a} {...abrir('servicio')} />
          <DialogoRetirar apartamento={a} {...abrir('retirar')} alRetirar={() => navigate('/panel/apartamentos')} />
        </>
      )}
    </section>
  )
}

function Volver() {
  return (
    <Link
      to="/panel/apartamentos"
      className="inline-flex w-fit items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
    >
      <ArrowLeft aria-hidden="true" className="size-4" /> Apartamentos
    </Link>
  )
}

function Tarjeta({ titulo, descripcion, children }: { titulo: string; descripcion?: string; children: ReactNode }) {
  return (
    <section className="rounded-tarjeta border border-border bg-card p-4 shadow-tarjeta md:p-5">
      <h2 className="font-titulo text-xl font-semibold">{titulo}</h2>
      {descripcion && <p className="text-xs text-muted-foreground">{descripcion}</p>}
      <div className="mt-3">{children}</div>
    </section>
  )
}

function Dato({ icono, etiqueta, valor }: { icono: ReactNode; etiqueta: string; valor: ReactNode }) {
  return (
    <div className="flex items-center gap-2 rounded-campo bg-muted/60 px-3 py-2">
      <span className="text-coral">{icono}</span>
      <div>
        <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
        <dd className="font-semibold">{valor}</dd>
      </div>
    </div>
  )
}

function Galeria({ apartamento }: { apartamento: ApartamentoDetalleResponse }) {
  const imagenes = [...apartamento.imagenes].sort((x, y) => Number(y.principal) - Number(x.principal))
  if (imagenes.length === 0) {
    return <FotoApartamento alt={apartamento.nombre} className="h-56 w-full rounded-tarjeta" />
  }
  const [principal, ...resto] = imagenes
  return (
    <div className="grid gap-2 sm:grid-cols-[2fr_1fr]">
      <div className="relative">
        <FotoApartamento
          src={principal.url}
          alt={`${apartamento.nombre}, foto principal`}
          className="h-56 w-full rounded-tarjeta bg-crema sm:h-72"
        />
        <span className="absolute top-3 left-3 rounded-full bg-ciruela/80 px-2.5 py-1 text-xs font-semibold text-white">
          Principal
        </span>
      </div>
      {resto.length > 0 && (
        <ul className="grid grid-cols-3 gap-2 sm:grid-cols-2 sm:content-start">
          {resto.map((img, i) => (
            <li key={img.url}>
              <FotoApartamento
                src={img.url}
                alt={`${apartamento.nombre}, foto ${i + 2}`}
                className="aspect-[4/3] w-full rounded-campo bg-crema"
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function nombreTemporada(id: string, temporadas: TemporadaResponse[]) {
  return temporadas.find((t) => t.id === id)?.nombre ?? id
}

function TarifasVigentes({
  apartamento,
  temporadas,
}: {
  apartamento: ApartamentoDetalleResponse
  temporadas: TemporadaResponse[]
}) {
  const porTemporada = new Map(apartamento.tarifas.map((t) => [t.idTemporada, t.valorPorOcupante]))
  const faltantes = temporadas.filter((t) => !porTemporada.has(t.id))
  const ids = temporadas.length > 0 ? temporadas.map((t) => t.id) : [...porTemporada.keys()]

  return (
    <>
      <ul className="divide-y divide-border">
        {ids.map((id) => (
          <li key={id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
            <span>Temporada {nombreTemporada(id, temporadas).toLowerCase()}</span>
            {porTemporada.has(id) ? (
              <Dinero valor={porTemporada.get(id)!} className="font-semibold" />
            ) : (
              <span className="text-xs font-semibold text-error">Sin tarifa</span>
            )}
          </li>
        ))}
      </ul>
      {!apartamento.activo && faltantes.length > 0 && (
        <p className="mt-3 text-xs text-muted-foreground">
          Para activarlo necesita tarifa en todas las temporadas.
        </p>
      )}
    </>
  )
}

function HistoricoTarifas({ identificacion, temporadas }: { identificacion: string; temporadas: TemporadaResponse[] }) {
  const [pagina, setPagina] = useState(0)
  const consulta = useHistoricoTarifas(identificacion, pagina)

  return (
    <Tarjeta titulo="Histórico de tarifas">
      {consulta.error ? (
        <AvisoError error={consulta.error} />
      ) : !consulta.data ? (
        <Skeleton className="h-24 w-full" />
      ) : consulta.data.contenido.length === 0 ? (
        <p className="text-sm text-muted-foreground">Aún no hay cambios de tarifa: rigen las tarifas iniciales.</p>
      ) : (
        <>
          <TablaResponsive
            columnas={[
              {
                clave: 'temporada',
                titulo: 'Temporada',
                celda: (t) => nombreTemporada(t.idTemporada, temporadas),
              },
              {
                clave: 'valor',
                titulo: 'Valor',
                className: 'md:text-right',
                celda: (t) => <Dinero valor={t.valorPorOcupante} />,
              },
              {
                clave: 'desde',
                titulo: 'Desde',
                celda: (t) => (t.vigenteDesde ? <Fecha valor={t.vigenteDesde} patron="d MMM yyyy" /> : 'Inicial'),
              },
              {
                clave: 'hasta',
                titulo: 'Hasta',
                celda: (t) =>
                  t.vigenteHasta ? (
                    <Fecha valor={t.vigenteHasta} patron="d MMM yyyy" />
                  ) : (
                    <span className="rounded-full bg-[#DDF2EA] px-2 py-0.5 text-xs font-semibold text-[#1C5E45]">
                      Vigente
                    </span>
                  ),
              },
            ]}
            filas={consulta.data.contenido}
            claveFila={(t) => `${t.idTemporada}-${t.vigenteDesde}-${t.vigenteHasta}-${t.valorPorOcupante}`}
            descripcion="Histórico de tarifas"
          />
          <Paginador
            className="mt-3"
            pagina={pagina}
            totalPaginas={consulta.data.totalPaginas}
            totalElementos={consulta.data.totalElementos}
            alCambiar={setPagina}
          />
        </>
      )}
    </Tarjeta>
  )
}
