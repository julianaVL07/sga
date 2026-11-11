import { useState, type FormEvent, type ReactNode } from 'react'
import { KeyRound, Loader2, Save } from 'lucide-react'
import { toast } from 'sonner'
import { esErrorApi } from '@/api/cliente'
import { useActualizarMiPerfil, useCambiarMiContrasena } from '@/api/usuarios'
import type { UsuarioResponse } from '@/api/tipos'
import { useAuth } from '@/auth/AuthContext'
import { NOMBRE_ROL } from '@/auth/roles'
import { AvisoError, Campo } from '@/components/coral'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { erroresDe } from '@/pages/panel/folio/erroresDe'
import { AYUDA_CONTRASENA, errorContrasena } from '@/pages/panel/usuarios/validaciones'

export default function MiPerfil() {
  const { usuario, cargandoUsuario } = useAuth()

  return (
    <section className="mx-auto flex w-full max-w-3xl flex-col gap-6">
      <header>
        <h1 className="font-titulo text-3xl font-semibold">Mi perfil</h1>
        <p className="mt-1 text-sm text-muted-foreground">Tus datos de contacto y tu contraseña.</p>
      </header>
      {usuario ? (
        <>
          {/* key: al cambiar de usuario o al guardar, el formulario parte de los datos del servidor */}
          <DatosPerfil key={`${usuario.id}-${usuario.nombre}-${usuario.telefono}-${usuario.fotoUrl}`} usuario={usuario} />
          <CambioContrasena />
        </>
      ) : cargandoUsuario ? (
        <>
          <Skeleton className="h-96" />
          <Skeleton className="h-64" />
        </>
      ) : (
        <AvisoError mensaje="No pudimos cargar tu perfil. Recarga la página para intentarlo de nuevo." />
      )}
    </section>
  )
}

function Tarjeta({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-5 rounded-tarjeta border border-border bg-card p-5 shadow-tarjeta md:p-6">
      <h2 className="font-titulo text-xl font-semibold">{titulo}</h2>
      {children}
    </div>
  )
}

function iniciales(nombre: string): string {
  const partes = nombre.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return '·'
  return ((partes[0][0] ?? '') + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase()
}

const esUrlFoto = (texto: string) => /^https?:\/\/\S+$/i.test(texto)

// ---------- Datos ----------

function DatosPerfil({ usuario }: { usuario: UsuarioResponse }) {
  const actualizar = useActualizarMiPerfil()
  const [nombre, setNombre] = useState(usuario.nombre)
  const [telefono, setTelefono] = useState(usuario.telefono ?? '')
  const [fotoUrl, setFotoUrl] = useState(usuario.fotoUrl ?? '')
  const [errores, setErrores] = useState<{ nombre?: string; telefono?: string; fotoUrl?: string }>({})

  const deServidor = erroresDe(actualizar.error)
  const foto = fotoUrl.trim()
  const vistaPrevia = esUrlFoto(foto) ? foto : undefined

  const enviar = (evento: FormEvent) => {
    evento.preventDefault()
    const nuevos = {
      nombre: nombre.trim() ? undefined : 'Escribe tu nombre',
      telefono: telefono.trim() ? undefined : 'Escribe tu teléfono',
      fotoUrl: foto && !esUrlFoto(foto) ? 'Debe empezar por http:// o https://' : undefined,
    }
    setErrores(nuevos)
    if (Object.values(nuevos).some(Boolean)) return
    actualizar.mutate(
      { nombre: nombre.trim(), telefono: telefono.trim(), ...(foto ? { fotoUrl: foto } : {}) },
      { onSuccess: () => toast.success('Perfil actualizado') },
    )
  }

  return (
    <Tarjeta titulo="Datos">
      <form noValidate onSubmit={enviar} className="flex flex-col gap-5">
        <div className="flex items-center gap-4">
          {/* key: si cambia la URL, el avatar vuelve a intentar cargar la imagen */}
          <Avatar key={vistaPrevia} className="size-20 shrink-0">
            {vistaPrevia && <AvatarImage src={vistaPrevia} alt="Vista previa de tu foto" className="object-cover" />}
            <AvatarFallback className="bg-primary text-2xl font-semibold text-primary-foreground">
              {iniciales(nombre || usuario.nombre)}
            </AvatarFallback>
          </Avatar>
          <div className="min-w-0">
            <p className="truncate font-semibold">{nombre || usuario.nombre}</p>
            <p className="text-sm text-muted-foreground">{NOMBRE_ROL[usuario.rol]}</p>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Campo id="perfil-nombre" etiqueta="Nombre" error={errores.nombre ?? deServidor.nombre} className="sm:col-span-2">
            <Input autoComplete="name" maxLength={100} value={nombre} onChange={(e) => setNombre(e.target.value)} />
          </Campo>
          <Campo id="perfil-correo" etiqueta="Correo" ayuda="El correo no se puede cambiar.">
            <Input type="email" value={usuario.correo} readOnly disabled />
          </Campo>
          <Campo id="perfil-telefono" etiqueta="Teléfono" error={errores.telefono ?? deServidor.telefono}>
            <Input type="tel" autoComplete="tel" maxLength={20} value={telefono} onChange={(e) => setTelefono(e.target.value)} />
          </Campo>
          <Campo
            id="perfil-foto"
            etiqueta="Foto (URL)"
            ayuda="Opcional. Dirección de una imagen que empiece por https://"
            error={errores.fotoUrl ?? deServidor.fotoUrl}
            className="sm:col-span-2"
          >
            <Input
              type="url"
              inputMode="url"
              maxLength={500}
              placeholder="https://…"
              value={fotoUrl}
              onChange={(e) => setFotoUrl(e.target.value)}
            />
          </Campo>
        </div>

        {actualizar.error && <AvisoError titulo="No se pudo guardar" error={actualizar.error} />}
        <div className="flex justify-end">
          <Button type="submit" disabled={actualizar.isPending} className="w-full sm:w-auto">
            {actualizar.isPending ? <Loader2 aria-hidden="true" className="animate-spin" /> : <Save aria-hidden="true" />}
            Guardar cambios
          </Button>
        </div>
      </form>
    </Tarjeta>
  )
}

// ---------- Contraseña ----------

interface ErroresContrasena {
  actual?: string
  nueva?: string
  confirmacion?: string
}

/** Lleva el 400 del servidor al campo que corresponde; null si no es de un campo */
function erroresDelServidor(error: unknown): ErroresContrasena | null {
  if (!esErrorApi(error) || error.status !== 400) return null
  const { contrasenaActual, contrasenaNueva } = error.errores
  if (contrasenaActual || contrasenaNueva) return { actual: contrasenaActual, nueva: contrasenaNueva }
  // "La contraseña actual no es correcta" llega sin errores por campo
  if (/actual/i.test(error.message)) return { actual: error.message }
  return null
}

function CambioContrasena() {
  const cambiar = useCambiarMiContrasena()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [errores, setErrores] = useState<ErroresContrasena>({})

  const deServidor = erroresDelServidor(cambiar.error)

  const enviar = (evento: FormEvent) => {
    evento.preventDefault()
    const nuevos: ErroresContrasena = {
      actual: actual ? undefined : 'Escribe tu contraseña actual',
      nueva: errorContrasena(nueva),
      confirmacion: confirmacion === nueva ? undefined : 'Las contraseñas no coinciden',
    }
    setErrores(nuevos)
    if (Object.values(nuevos).some(Boolean)) return
    cambiar.mutate(
      { contrasenaActual: actual, contrasenaNueva: nueva },
      {
        onSuccess: () => {
          setActual('')
          setNueva('')
          setConfirmacion('')
          toast.success('Contraseña cambiada', { description: 'Úsala la próxima vez que inicies sesión.' })
        },
      },
    )
  }

  return (
    <Tarjeta titulo="Contraseña">
      <form noValidate onSubmit={enviar} className="flex flex-col gap-4">
        <Campo id="contrasena-actual" etiqueta="Contraseña actual" error={errores.actual ?? deServidor?.actual}>
          <Input
            type="password"
            autoComplete="current-password"
            value={actual}
            onChange={(e) => {
              setActual(e.target.value)
              if (deServidor?.actual) cambiar.reset()
            }}
          />
        </Campo>
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo id="contrasena-nueva" etiqueta="Contraseña nueva" ayuda={AYUDA_CONTRASENA} error={errores.nueva ?? deServidor?.nueva}>
            <Input type="password" autoComplete="new-password" maxLength={72} value={nueva} onChange={(e) => setNueva(e.target.value)} />
          </Campo>
          <Campo id="contrasena-confirmacion" etiqueta="Repite la contraseña nueva" error={errores.confirmacion}>
            <Input
              type="password"
              autoComplete="new-password"
              maxLength={72}
              value={confirmacion}
              onChange={(e) => setConfirmacion(e.target.value)}
            />
          </Campo>
        </div>
        {cambiar.error && !deServidor && <AvisoError titulo="No se pudo cambiar la contraseña" error={cambiar.error} />}
        <div className="flex justify-end">
          <Button type="submit" disabled={cambiar.isPending} className="w-full sm:w-auto">
            {cambiar.isPending ? <Loader2 aria-hidden="true" className="animate-spin" /> : <KeyRound aria-hidden="true" />}
            Cambiar contraseña
          </Button>
        </div>
      </form>
    </Tarjeta>
  )
}
