import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { solicitarRecuperacion } from '@/api/auth'
import { esErrorApi } from '@/api/cliente'
import { AvisoError, Campo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PanelBienvenida } from './PanelBienvenida'

const esquema = z.object({
  correo: z
    .string()
    .trim()
    .min(1, 'Escribe tu correo')
    .email('El correo no tiene un formato válido')
    .max(120, 'El correo admite máximo 120 caracteres'),
})

type Datos = z.infer<typeof esquema>

/** Estado de navegación que comparten las dos pantallas de recuperación */
export interface EstadoRecuperacion {
  correo?: string
}

/** Paso 1 de la recuperación: pide el correo y envía el código de 6 dígitos */
export default function RecuperarContrasena() {
  const navigate = useNavigate()
  const location = useLocation()
  const correoInicial = (location.state as EstadoRecuperacion | null)?.correo ?? ''

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Datos>({ resolver: zodResolver(esquema), defaultValues: { correo: correoInicial } })

  const solicitud = useMutation({
    mutationFn: (datos: Datos) => solicitarRecuperacion(datos),
    onSuccess: (_, datos) => {
      // El backend responde igual exista o no la cuenta: el mensaje tampoco lo revela
      toast.success('Si el correo tiene una cuenta, te enviamos un código.')
      navigate('/recuperar/codigo', { state: { correo: datos.correo } satisfies EstadoRecuperacion })
    },
    onError: (error) => {
      if (esErrorApi(error) && error.status === 400 && error.errores.correo) {
        setError('correo', { message: error.errores.correo })
      }
    },
  })

  const errorGeneral =
    solicitud.error && !(esErrorApi(solicitud.error) && solicitud.error.status === 400) ? solicitud.error : null

  return (
    <div className="mx-auto grid max-w-4xl overflow-hidden rounded-tarjeta border border-border bg-card shadow-tarjeta md:grid-cols-2">
      <PanelBienvenida />

      <div className="px-6 py-8 md:px-10 md:py-14">
        <h1 className="font-titulo text-3xl font-semibold">Recuperar contraseña</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Escribe el correo de tu cuenta y te enviaremos un código de 6 dígitos para crear una contraseña nueva.
        </p>

        {errorGeneral && <AvisoError className="mt-6" error={errorGeneral} />}

        <form noValidate onSubmit={handleSubmit((datos) => solicitud.mutate(datos))} className="mt-6 flex flex-col gap-4">
          <Campo id="correo" etiqueta="Correo" error={errors.correo?.message}>
            <Input type="email" autoComplete="email" inputMode="email" autoFocus {...register('correo')} />
          </Campo>

          <Button type="submit" size="lg" className="mt-2 w-full" disabled={solicitud.isPending}>
            {solicitud.isPending && <Loader2 aria-hidden="true" className="animate-spin" />}
            Enviar código
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          ¿Ya tienes un código?{' '}
          <Link
            to="/recuperar/codigo"
            state={{ correo: correoInicial } satisfies EstadoRecuperacion}
            className="font-semibold text-primary underline-offset-4 hover:underline"
          >
            Escribirlo
          </Link>
        </p>
        <p className="mt-2 text-center text-sm text-muted-foreground">
          <Link to="/login" className="font-semibold text-primary underline-offset-4 hover:underline">
            Volver a iniciar sesión
          </Link>
        </p>
      </div>
    </div>
  )
}
