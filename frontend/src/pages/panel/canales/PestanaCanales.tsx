import { useState } from 'react'
import { KeyRound, Network, PowerOff } from 'lucide-react'
import { toast } from 'sonner'
import {
  useCanales,
  useDesactivarCanal,
  useRegenerarCredencialCanal,
  type CanalVentaResponse,
  type CredencialCanalResponse,
} from '@/api/canales'
import {
  AvisoError,
  ConfirmarDialogo,
  Fecha,
  Paginador,
  TablaResponsive,
  Vacio,
  type ColumnaTabla,
} from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { EtiquetaActivo } from './etiquetas'
import { PATRON_FECHA_HORA } from './textos'

type Accion = 'regenerar' | 'desactivar' | null

/** Canales de venta: sin credenciales; la credencial nueva pasa al padre, que la muestra una vez */
export function PestanaCanales({ alEmitir }: { alEmitir: (credencial: CredencialCanalResponse) => void }) {
  const [pagina, setPagina] = useState(0)
  const consulta = useCanales(pagina)
  const regenerar = useRegenerarCredencialCanal()
  const desactivar = useDesactivarCanal()
  const [accion, setAccion] = useState<Accion>(null)
  const [elegido, setElegido] = useState<CanalVentaResponse | null>(null)

  const abrir = (tipo: Accion, canal: CanalVentaResponse) => {
    setElegido(canal)
    regenerar.reset()
    desactivar.reset()
    setAccion(tipo)
  }
  const ocupado = regenerar.isPending || desactivar.isPending

  const columnas: ColumnaTabla<CanalVentaResponse>[] = [
    {
      clave: 'nombre',
      titulo: 'Canal',
      ocultarEnTarjeta: true,
      celda: (c) => <span className="font-mono font-semibold">{c.nombre}</span>,
    },
    { clave: 'tipo', titulo: 'Tipo', celda: () => 'Externo' },
    { clave: 'estado', titulo: 'Estado', celda: (c) => <EtiquetaActivo activo={c.activo} /> },
    { clave: 'creado', titulo: 'Creado', celda: (c) => <Fecha valor={c.fechaCreacion} patron={PATRON_FECHA_HORA} /> },
  ]

  return (
    <>
      {consulta.error ? (
        <AvisoError titulo="No se pudieron consultar los canales" error={consulta.error} />
      ) : !consulta.data ? (
        <Skeleton className="h-60" />
      ) : consulta.data.contenido.length === 0 ? (
        <Vacio icono={Network} titulo="Sin canales" descripcion="Crea el primer canal de venta externo." />
      ) : (
        <>
          <TablaResponsive
            descripcion="Canales de venta"
            columnas={columnas}
            filas={consulta.data.contenido}
            claveFila={(c) => c.nombre}
            tituloTarjeta={(c) => (
              <span className="flex items-center justify-between gap-2">
                <span className="font-mono">{c.nombre}</span>
                <EtiquetaActivo activo={c.activo} />
              </span>
            )}
            acciones={(c) =>
              c.activo ? (
                <div className="flex flex-wrap justify-end gap-1">
                  <Button variant="ghost" size="sm" onClick={() => abrir('regenerar', c)}>
                    <KeyRound aria-hidden="true" /> Regenerar credencial
                  </Button>
                  <Button variant="ghost" size="sm" className="text-error" onClick={() => abrir('desactivar', c)}>
                    <PowerOff aria-hidden="true" /> Desactivar
                  </Button>
                </div>
              ) : (
                <span className="text-xs text-muted-foreground">Sin acciones</span>
              )
            }
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

      <ConfirmarDialogo
        abierto={accion === 'regenerar'}
        alCambiar={(abierto) => !ocupado && !abierto && setAccion(null)}
        titulo={`Regenerar la credencial de ${elegido?.nombre ?? ''}`}
        descripcion="La credencial actual dejará de funcionar de inmediato. El canal tendrá que configurar la nueva para seguir enviando reservas."
        textoConfirmar="Regenerar"
        textoCancelar="Volver"
        cargando={regenerar.isPending}
        alConfirmar={() =>
          elegido &&
          regenerar.mutate(elegido.nombre, {
            onSuccess: (respuesta) => {
              setAccion(null)
              alEmitir(respuesta)
            },
          })
        }
      >
        {regenerar.error && <AvisoError titulo="No se pudo regenerar" error={regenerar.error} />}
      </ConfirmarDialogo>

      <ConfirmarDialogo
        abierto={accion === 'desactivar'}
        alCambiar={(abierto) => !ocupado && !abierto && setAccion(null)}
        titulo={`Desactivar ${elegido?.nombre ?? ''}`}
        descripcion="El canal no se borra: se conservan sus reservas, sus conflictos y su bitácora, pero sus peticiones se rechazarán desde ahora. No se puede volver a activar."
        textoConfirmar="Desactivar"
        textoCancelar="Volver"
        peligroso
        cargando={desactivar.isPending}
        alConfirmar={() =>
          elegido &&
          desactivar.mutate(elegido.nombre, {
            onSuccess: (c) => {
              setAccion(null)
              toast.success('Canal desactivado', { description: c.nombre })
            },
          })
        }
      >
        {desactivar.error && <AvisoError titulo="No se pudo desactivar" error={desactivar.error} />}
      </ConfirmarDialogo>
    </>
  )
}
