import { useState } from 'react'
import { toast } from 'sonner'
import { useCambiarRolUsuario, useCrearUsuario, type CrearUsuarioRequest } from '@/api/usuarios'
import type { Rol, UsuarioResponse } from '@/api/tipos'
import { NOMBRE_ROL } from '@/auth/roles'
import { Campo } from '@/components/coral'
import { Input } from '@/components/ui/input'
import { DialogoFormulario } from '@/pages/panel/folio/DialogoFormulario'
import { erroresDe } from '@/pages/panel/folio/erroresDe'
import { useAlAbrir } from '@/pages/panel/folio/useAlAbrir'
import { hoyISO } from '@/pages/publico/comun/reglas'
import { claseSelect } from '@/pages/publico/comun/estilos'
import { ROLES } from './listaRoles'
import { AYUDA_CONTRASENA, errorContrasena, esCorreo } from './validaciones'

interface Base {
  abierto: boolean
  alCambiar: (abierto: boolean) => void
}

// ---------- Crear usuario ----------

type Datos = Omit<CrearUsuarioRequest, 'rol'> & { rol: Rol }
const VACIO: Datos = {
  nombre: '',
  correo: '',
  contrasena: '',
  telefono: '',
  documento: '',
  fechaNacimiento: '',
  rol: 'RECEPCIONISTA',
}

export function DialogoCrearUsuario({ abierto, alCambiar }: Base) {
  const crear = useCrearUsuario()
  const [datos, setDatos] = useState<Datos>(VACIO)
  const [errores, setErrores] = useState<Partial<Record<keyof Datos, string>>>({})

  useAlAbrir(abierto, () => {
    setDatos(VACIO)
    setErrores({})
    crear.reset()
  })

  const campo = (clave: keyof Datos) => ({
    value: datos[clave],
    onChange: (e: { target: { value: string } }) => setDatos((d) => ({ ...d, [clave]: e.target.value })),
  })
  const deServidor = erroresDe(crear.error)
  const error = (clave: keyof Datos) => errores[clave] ?? deServidor[clave]

  const enviar = () => {
    const nuevos: Partial<Record<keyof Datos, string>> = {}
    if (!datos.nombre.trim()) nuevos.nombre = 'Escribe el nombre'
    if (!esCorreo(datos.correo.trim())) nuevos.correo = 'Escribe un correo válido'
    nuevos.contrasena = errorContrasena(datos.contrasena)
    if (!datos.telefono.trim()) nuevos.telefono = 'Escribe el teléfono'
    if (!datos.documento.trim()) nuevos.documento = 'Escribe el documento'
    if (!datos.fechaNacimiento) nuevos.fechaNacimiento = 'Escribe la fecha de nacimiento'
    else if (datos.fechaNacimiento >= hoyISO()) nuevos.fechaNacimiento = 'Debe ser una fecha pasada'
    setErrores(nuevos)
    if (Object.values(nuevos).some(Boolean)) return

    crear.mutate(
      {
        ...datos,
        nombre: datos.nombre.trim(),
        correo: datos.correo.trim(),
        telefono: datos.telefono.trim(),
        documento: datos.documento.trim(),
      },
      {
        onSuccess: (u) => {
          toast.success('Usuario creado', { description: `${u.nombre} · ${NOMBRE_ROL[u.rol]}` })
          alCambiar(false)
        },
      },
    )
  }

  return (
    <DialogoFormulario
      id="form-usuario"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo="Nuevo usuario"
      descripcion="Crea una cuenta de cualquier rol. La persona inicia sesión con este correo y esta contraseña."
      textoEnviar="Crear usuario"
      cargando={crear.isPending}
      error={crear.error}
      tituloError="No se pudo crear el usuario"
      className="sm:max-w-lg"
      alEnviar={enviar}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Campo id="usuario-nombre" etiqueta="Nombre" error={error('nombre')} className="sm:col-span-2">
          <Input autoComplete="off" maxLength={100} {...campo('nombre')} />
        </Campo>
        <Campo id="usuario-correo" etiqueta="Correo" error={error('correo')} className="sm:col-span-2">
          <Input type="email" autoComplete="off" maxLength={120} {...campo('correo')} />
        </Campo>
        <Campo id="usuario-contrasena" etiqueta="Contraseña" error={error('contrasena')} ayuda={AYUDA_CONTRASENA} className="sm:col-span-2">
          <Input type="password" autoComplete="new-password" maxLength={72} {...campo('contrasena')} />
        </Campo>
        <Campo id="usuario-telefono" etiqueta="Teléfono" error={error('telefono')}>
          <Input type="tel" maxLength={20} {...campo('telefono')} />
        </Campo>
        <Campo id="usuario-documento" etiqueta="Documento" error={error('documento')}>
          <Input inputMode="numeric" maxLength={15} {...campo('documento')} />
        </Campo>
        <Campo id="usuario-nacimiento" etiqueta="Fecha de nacimiento" error={error('fechaNacimiento')}>
          <Input type="date" max={hoyISO()} {...campo('fechaNacimiento')} />
        </Campo>
        <Campo id="usuario-rol" etiqueta="Rol" error={error('rol')}>
          <select className={claseSelect} {...campo('rol')}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {NOMBRE_ROL[r]}
              </option>
            ))}
          </select>
        </Campo>
      </div>
    </DialogoFormulario>
  )
}

// ---------- Cambiar rol ----------

export function DialogoCambiarRol({ usuario, abierto, alCambiar }: Base & { usuario: UsuarioResponse | null }) {
  const cambiar = useCambiarRolUsuario()
  const [rol, setRol] = useState<Rol>('HUESPED')
  const [aviso, setAviso] = useState<string>()

  useAlAbrir(abierto, () => {
    setRol(usuario?.rol ?? 'HUESPED')
    setAviso(undefined)
    cambiar.reset()
  })

  if (!usuario) return null

  const enviar = () => {
    if (rol === usuario.rol) {
      setAviso('Elige un rol distinto al actual.')
      return
    }
    setAviso(undefined)
    cambiar.mutate(
      { id: usuario.id, rol },
      {
        onSuccess: (u) => {
          toast.success('Rol cambiado', { description: `${u.nombre} ahora es ${NOMBRE_ROL[u.rol].toLowerCase()}` })
          alCambiar(false)
        },
      },
    )
  }

  return (
    <DialogoFormulario
      id="form-rol"
      abierto={abierto}
      alCambiar={alCambiar}
      titulo={`Cambiar el rol de ${usuario.nombre}`}
      descripcion={`Rol actual: ${NOMBRE_ROL[usuario.rol]}.`}
      textoEnviar="Cambiar rol"
      cargando={cambiar.isPending}
      error={cambiar.error}
      tituloError="No se pudo cambiar el rol"
      alEnviar={enviar}
    >
      <Campo id="rol-nuevo" etiqueta="Nuevo rol" error={aviso}>
        <select className={claseSelect} value={rol} onChange={(e) => setRol(e.target.value as Rol)}>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {NOMBRE_ROL[r]}
              {r === usuario.rol ? ' (actual)' : ''}
            </option>
          ))}
        </select>
      </Campo>
    </DialogoFormulario>
  )
}
