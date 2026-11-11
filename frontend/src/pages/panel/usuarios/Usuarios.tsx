import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Plus, UserCog, UserX, Users as IconoUsuarios } from 'lucide-react'
import { toast } from 'sonner'
import { useDesactivarUsuario, useUsuarios, type FiltrosUsuarios } from '@/api/usuarios'
import type { Rol, UsuarioResponse } from '@/api/tipos'
import { useAuth } from '@/auth/AuthContext'
import { NOMBRE_ROL } from '@/auth/roles'
import {
  AvisoError,
  Campo,
  ConfirmarDialogo,
  Paginador,
  TablaResponsive,
  Vacio,
  type ColumnaTabla,
} from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { claseSelect } from '@/pages/publico/comun/estilos'
import { DialogoCambiarRol, DialogoCrearUsuario } from './DialogosUsuario'
import { ROLES } from './listaRoles'

type Dialogo = 'crear' | 'rol' | 'desactivar' | null

export default function Usuarios() {
  const { usuario: yo } = useAuth()
  const [params, setParams] = useSearchParams()
  const pagina = Number(params.get('pagina') ?? 0) || 0
  const rol = (ROLES as string[]).includes(params.get('rol') ?? '') ? (params.get('rol') as Rol) : undefined
  const activoTexto = params.get('activo')
  const activo = activoTexto === 'true' ? true : activoTexto === 'false' ? false : undefined
  const filtros: FiltrosUsuarios = { rol, activo }
  const consulta = useUsuarios(filtros, pagina)
  const desactivar = useDesactivarUsuario()

  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [elegido, setElegido] = useState<UsuarioResponse | null>(null)

  const filtrar = (clave: 'rol' | 'activo', valor: string) => {
    const siguientes = new URLSearchParams(params)
    if (valor) siguientes.set(clave, valor)
    else siguientes.delete(clave)
    siguientes.delete('pagina')
    setParams(siguientes)
  }
  const irAPagina = (n: number) => {
    const siguientes = new URLSearchParams(params)
    if (n > 0) siguientes.set('pagina', String(n))
    else siguientes.delete('pagina')
    setParams(siguientes)
  }

  const abrir = (tipo: Dialogo, u: UsuarioResponse | null = null) => {
    setElegido(u)
    desactivar.reset()
    setDialogo(tipo)
  }
  const alCambiar = (abierto: boolean) => {
    if (!abierto) setDialogo(null)
  }

  const columnas: ColumnaTabla<UsuarioResponse>[] = [
    {
      clave: 'usuario',
      titulo: 'Usuario',
      ocultarEnTarjeta: true,
      className: 'whitespace-normal',
      celda: (u) => <NombreUsuario usuario={u} esYo={u.id === yo?.id} />,
    },
    { clave: 'rol', titulo: 'Rol', celda: (u) => NOMBRE_ROL[u.rol] },
    { clave: 'documento', titulo: 'Documento', celda: (u) => u.documento },
    { clave: 'telefono', titulo: 'Teléfono', celda: (u) => u.telefono },
    { clave: 'estado', titulo: 'Estado', celda: (u) => <EtiquetaActivo activo={u.activo} /> },
  ]

  return (
    <section className="mx-auto flex w-full max-w-6xl flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-titulo text-3xl font-semibold">Usuarios</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Cuentas de huéspedes y del equipo. Un usuario no se borra: se desactiva.
          </p>
        </div>
        <Button onClick={() => abrir('crear')}>
          <Plus aria-hidden="true" /> Nuevo usuario
        </Button>
      </header>

      <div className="grid grid-cols-2 gap-3 sm:flex sm:flex-wrap">
        <Campo id="filtro-rol" etiqueta="Rol" className="sm:w-56">
          <select className={claseSelect} value={rol ?? ''} onChange={(e) => filtrar('rol', e.target.value)}>
            <option value="">Todos</option>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {NOMBRE_ROL[r]}
              </option>
            ))}
          </select>
        </Campo>
        <Campo id="filtro-activo" etiqueta="Estado" className="sm:w-44">
          <select className={claseSelect} value={activoTexto ?? ''} onChange={(e) => filtrar('activo', e.target.value)}>
            <option value="">Todos</option>
            <option value="true">Activos</option>
            <option value="false">Inactivos</option>
          </select>
        </Campo>
      </div>

      <div className="rounded-tarjeta md:border md:border-border md:bg-card md:shadow-tarjeta">
        {consulta.error ? (
          <AvisoError titulo="No se pudieron consultar los usuarios" error={consulta.error} />
        ) : !consulta.data ? (
          <Skeleton className="h-80" />
        ) : consulta.data.contenido.length === 0 ? (
          <Vacio icono={IconoUsuarios} titulo="Sin usuarios" descripcion="No hay usuarios con estos filtros." />
        ) : (
          <TablaResponsive
            descripcion="Usuarios"
            columnas={columnas}
            filas={consulta.data.contenido}
            claveFila={(u) => String(u.id)}
            tituloTarjeta={(u) => <NombreUsuario usuario={u} esYo={u.id === yo?.id} />}
            acciones={(u) => (
              <div className="flex flex-wrap justify-end gap-1">
                <Button variant="ghost" size="sm" onClick={() => abrir('rol', u)}>
                  <UserCog aria-hidden="true" /> Cambiar rol
                </Button>
                {u.activo && (
                  <Button variant="ghost" size="sm" className="text-error" onClick={() => abrir('desactivar', u)}>
                    <UserX aria-hidden="true" /> Desactivar
                  </Button>
                )}
              </div>
            )}
          />
        )}
      </div>
      {consulta.data && (
        <Paginador
          pagina={pagina}
          totalPaginas={consulta.data.totalPaginas}
          totalElementos={consulta.data.totalElementos}
          alCambiar={irAPagina}
        />
      )}

      <DialogoCrearUsuario abierto={dialogo === 'crear'} alCambiar={alCambiar} />
      <DialogoCambiarRol usuario={elegido} abierto={dialogo === 'rol'} alCambiar={alCambiar} />
      <ConfirmarDialogo
        abierto={dialogo === 'desactivar'}
        alCambiar={(abierto) => !desactivar.isPending && alCambiar(abierto)}
        titulo={`Desactivar a ${elegido?.nombre ?? ''}`}
        descripcion="No podrá volver a iniciar sesión. Sus datos y sus reservas se conservan. No se puede desactivar a quien tiene reservas pendientes, confirmadas o en curso, ni a uno mismo."
        textoConfirmar="Desactivar"
        textoCancelar="Volver"
        peligroso
        cargando={desactivar.isPending}
        alConfirmar={() =>
          elegido &&
          desactivar.mutate(elegido.id, {
            onSuccess: (u) => {
              setDialogo(null)
              toast.success('Usuario desactivado', { description: u.nombre })
            },
          })
        }
      >
        {desactivar.error && <AvisoError titulo="No se pudo desactivar" error={desactivar.error} />}
      </ConfirmarDialogo>
    </section>
  )
}

function NombreUsuario({ usuario, esYo }: { usuario: UsuarioResponse; esYo: boolean }) {
  return (
    <span className="flex min-w-0 flex-col">
      <span className="font-medium">
        {usuario.nombre}
        {esYo && <span className="ml-1.5 text-xs font-normal text-muted-foreground">(tú)</span>}
      </span>
      <span className="truncate text-xs font-normal text-muted-foreground">{usuario.correo}</span>
    </span>
  )
}

function EtiquetaActivo({ activo }: { activo: boolean }) {
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap"
      style={activo ? { backgroundColor: '#DDF2EA', color: '#1C5E45' } : { backgroundColor: '#EFE7EC', color: '#57434F' }}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {activo ? 'Activo' : 'Inactivo'}
    </span>
  )
}
