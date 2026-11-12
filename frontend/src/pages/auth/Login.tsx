import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, Navigate, useLocation, useSearchParams } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { z } from 'zod'
import { esErrorApi } from '@/api/cliente'
import { useAuth } from '@/auth/AuthContext'
import { rutaDespuesDeLogin } from '@/auth/destino'
import { AvisoError, Campo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PanelBienvenida } from './PanelBienvenida'

const esquema = z.object({
  correo: z.string().trim().min(1, 'Escribe tu correo').email('El correo no tiene un formato válido'),
  contrasena: z.string().min(1, 'Escribe tu contraseña'),
})

type Datos = z.infer<typeof esquema>

export default function Login() {
  const { autenticado, roles, login } = useAuth()
  const location = useLocation()
  const [parametros] = useSearchParams()
  const expirado = parametros.get('expirado') === '1'

  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<Datos>({ resolver: zodResolver(esquema), defaultValues: { correo: '', contrasena: '' } })
  // El correo escrito pasa a la recuperación de contraseña para no tener que repetirlo
  const correoEscrito = useWatch({ control, name: 'correo' })

  const inicio = useMutation({
    mutationFn: (datos: Datos) => login(datos.correo, datos.contrasena),
    onError: (error) => {
      if (esErrorApi(error) && error.status === 400) {
        Object.entries(error.errores).forEach(([campo, mensaje]) => {
          if (campo === 'correo' || campo === 'contrasena') setError(campo, { message: mensaje })
        })
      }
    },
  })

  // Al tener sesión (recién iniciada o ya abierta) se va a su destino
  if (autenticado) {
    return <Navigate to={rutaDespuesDeLogin(roles, location.state)} replace />
  }

  const errorGeneral = inicio.error && !(esErrorApi(inicio.error) && inicio.error.status === 400) ? inicio.error : null

  return (
    <div className="mx-auto grid max-w-4xl overflow-hidden rounded-tarjeta border border-border bg-card shadow-tarjeta md:grid-cols-2">
      <PanelBienvenida />

      <div className="px-6 py-8 md:px-10 md:py-14">
        <h1 className="font-titulo text-3xl font-semibold">Iniciar sesión</h1>
        <p className="mt-1 text-sm text-muted-foreground">Entra con tu correo y tu contraseña.</p>

        {expirado && !inicio.isPending && (
          <AvisoError className="mt-6" titulo="Tu sesión venció" mensaje="Inicia sesión de nuevo para continuar." />
        )}
        {errorGeneral && <AvisoError className="mt-6" error={errorGeneral} />}

        <form noValidate onSubmit={handleSubmit((datos) => inicio.mutate(datos))} className="mt-6 flex flex-col gap-4">
          <Campo id="correo" etiqueta="Correo" error={errors.correo?.message}>
            <Input type="email" autoComplete="email" inputMode="email" {...register('correo')} />
          </Campo>
          <Campo id="contrasena" etiqueta="Contraseña" error={errors.contrasena?.message}>
            <Input type="password" autoComplete="current-password" {...register('contrasena')} />
          </Campo>
          <Link
            to="/recuperar"
            state={{ correo: correoEscrito.trim() }}
            className="-mt-2 w-fit self-end text-sm font-medium text-primary underline-offset-4 hover:underline"
          >
            ¿Olvidaste tu contraseña?
          </Link>

          <Button type="submit" size="lg" className="mt-2 w-full" disabled={inicio.isPending}>
            {inicio.isPending && <Loader2 aria-hidden="true" className="animate-spin" />}
            Iniciar sesión
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          ¿No tienes cuenta?{' '}
          <Link to="/registro" state={location.state} className="font-semibold text-primary underline-offset-4 hover:underline">
            Crear cuenta
          </Link>
        </p>
      </div>
    </div>
  )
}
