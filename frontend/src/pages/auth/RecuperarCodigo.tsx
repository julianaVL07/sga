import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { restablecerContrasena } from '@/api/auth'
import { esErrorApi } from '@/api/cliente'
import { AvisoError, Campo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PanelBienvenida } from './PanelBienvenida'
import type { EstadoRecuperacion } from './RecuperarContrasena'

// La contraseña nueva sigue las mismas reglas del registro
const esquema = z
  .object({
    correo: z
      .string()
      .trim()
      .min(1, 'Escribe tu correo')
      .email('El correo no tiene un formato válido')
      .max(120, 'El correo admite máximo 120 caracteres'),
    codigo: z
      .string()
      .trim()
      .regex(/^\d{6}$/, 'El código tiene 6 dígitos'),
    nuevaContrasena: z
      .string()
      .min(8, 'La contraseña debe tener entre 8 y 72 caracteres')
      .max(72, 'La contraseña debe tener entre 8 y 72 caracteres')
      .regex(/^(?=.*\p{Lu})(?=.*\d).*$/u, 'La contraseña debe tener al menos una mayúscula y un número'),
    confirmacion: z.string().min(1, 'Repite la contraseña nueva'),
  })
  .refine((datos) => datos.nuevaContrasena === datos.confirmacion, {
    path: ['confirmacion'],
    message: 'Las contraseñas no coinciden',
  })

type Datos = z.infer<typeof esquema>
type CampoBackend = 'correo' | 'codigo' | 'nuevaContrasena'

const CAMPOS: CampoBackend[] = ['correo', 'codigo', 'nuevaContrasena']

/** Paso 2 de la recuperación: el código que llegó al correo y la contraseña nueva */
export default function RecuperarCodigo() {
  const navigate = useNavigate()
  const location = useLocation()
  const correoInicial = (location.state as EstadoRecuperacion | null)?.correo ?? ''

  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: { correo: correoInicial, codigo: '', nuevaContrasena: '', confirmacion: '' },
  })
  const correoEscrito = useWatch({ control, name: 'correo' })

  const cambio = useMutation({
    mutationFn: ({ correo, codigo, nuevaContrasena }: Datos) =>
      restablecerContrasena({ correo, codigo, nuevaContrasena }),
    onSuccess: () => {
      toast.success('Listo, tu contraseña quedó cambiada. Inicia sesión con la nueva.')
      navigate('/login', { replace: true })
    },
    onError: (error) => {
      if (!esErrorApi(error) || error.status !== 400) return
      const deCampos = Object.entries(error.errores).filter(([campo]) => CAMPOS.includes(campo as CampoBackend))
      if (deCampos.length > 0) {
        deCampos.forEach(([campo, mensaje]) => setError(campo as CampoBackend, { message: mensaje }))
      } else {
        // Código incorrecto, vencido o ya usado: el backend no dice cuál, para no revelar cuentas
        setError('codigo', { message: error.message })
      }
    },
  })

  const errorGeneral = cambio.error && !(esErrorApi(cambio.error) && cambio.error.status === 400) ? cambio.error : null

  return (
    <div className="mx-auto grid max-w-4xl overflow-hidden rounded-tarjeta border border-border bg-card shadow-tarjeta md:grid-cols-2">
      <PanelBienvenida />

      <div className="px-6 py-8 md:px-10 md:py-14">
        <h1 className="font-titulo text-3xl font-semibold">Crear contraseña nueva</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Escribe el código de 6 dígitos que te enviamos. Vence a los 15 minutos y sirve una sola vez.
        </p>

        {errorGeneral && <AvisoError className="mt-6" error={errorGeneral} />}

        <form noValidate onSubmit={handleSubmit((datos) => cambio.mutate(datos))} className="mt-6 flex flex-col gap-4">
          <Campo id="correo" etiqueta="Correo" error={errors.correo?.message}>
            <Input type="email" autoComplete="email" inputMode="email" {...register('correo')} />
          </Campo>
          <Campo id="codigo" etiqueta="Código" error={errors.codigo?.message}>
            <Input
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="000000"
              autoFocus={!!correoInicial}
              className="font-mono tracking-[0.4em]"
              {...register('codigo')}
            />
          </Campo>
          <Campo
            id="nuevaContrasena"
            etiqueta="Contraseña nueva"
            error={errors.nuevaContrasena?.message}
            ayuda="Mínimo 8 caracteres, con una mayúscula y un número."
          >
            <Input type="password" autoComplete="new-password" {...register('nuevaContrasena')} />
          </Campo>
          <Campo id="confirmacion" etiqueta="Repite la contraseña nueva" error={errors.confirmacion?.message}>
            <Input type="password" autoComplete="new-password" {...register('confirmacion')} />
          </Campo>

          <Button type="submit" size="lg" className="mt-2 w-full" disabled={cambio.isPending}>
            {cambio.isPending && <Loader2 aria-hidden="true" className="animate-spin" />}
            Cambiar contraseña
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          ¿No te llegó o ya venció?{' '}
          <Link
            to="/recuperar"
            state={{ correo: correoEscrito.trim() } satisfies EstadoRecuperacion}
            className="font-semibold text-primary underline-offset-4 hover:underline"
          >
            Pedir otro código
          </Link>
        </p>
      </div>
    </div>
  )
}
