import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { format } from 'date-fns'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { registrarHuesped } from '@/api/auth'
import { esErrorApi } from '@/api/cliente'
import { useAuth } from '@/auth/AuthContext'
import { rutaDespuesDeLogin } from '@/auth/destino'
import { AvisoError, Campo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PanelBienvenida } from './PanelBienvenida'

const hoy = () => format(new Date(), 'yyyy-MM-dd')

// Las mismas reglas que valida el backend al registrar
const esquema = z.object({
  nombre: z.string().trim().min(1, 'Escribe tu nombre').max(100, 'El nombre admite máximo 100 caracteres'),
  correo: z
    .string()
    .trim()
    .min(1, 'Escribe tu correo')
    .email('El correo no tiene un formato válido')
    .max(120, 'El correo admite máximo 120 caracteres'),
  contrasena: z
    .string()
    .min(8, 'La contraseña debe tener entre 8 y 72 caracteres')
    .max(72, 'La contraseña debe tener entre 8 y 72 caracteres')
    .regex(/^(?=.*\p{Lu})(?=.*\d).*$/u, 'La contraseña debe tener al menos una mayúscula y un número'),
  telefono: z.string().trim().min(1, 'Escribe tu teléfono').max(20, 'El teléfono admite máximo 20 caracteres'),
  documento: z.string().trim().min(1, 'Escribe tu documento').max(15, 'El documento admite máximo 15 caracteres'),
  fechaNacimiento: z
    .string()
    .min(1, 'Elige tu fecha de nacimiento')
    .refine((valor) => valor < hoy(), 'La fecha de nacimiento debe ser pasada'),
})

type Datos = z.infer<typeof esquema>
type NombreCampo = keyof Datos

const CAMPOS: NombreCampo[] = ['nombre', 'correo', 'contrasena', 'telefono', 'documento', 'fechaNacimiento']

export default function Registro() {
  const { autenticado, roles, login } = useAuth()
  const location = useLocation()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: { nombre: '', correo: '', contrasena: '', telefono: '', documento: '', fechaNacimiento: '' },
  })

  const registro = useMutation({
    mutationFn: async (datos: Datos) => {
      await registrarHuesped(datos)
      // Con la cuenta creada, entra de una vez
      await login(datos.correo, datos.contrasena)
    },
    onSuccess: () => toast.success('¡Bienvenido a Coral! Tu cuenta quedó creada.'),
    onError: (error) => {
      if (!esErrorApi(error)) return
      if (error.status === 409) {
        // El backend no dice cuál de los dos se repite: se marca en ambos
        setError('correo', { message: error.message })
        setError('documento', { message: error.message })
      } else if (error.status === 400) {
        Object.entries(error.errores).forEach(([campo, mensaje]) => {
          if (CAMPOS.includes(campo as NombreCampo)) setError(campo as NombreCampo, { message: mensaje })
        })
      }
    },
  })

  if (autenticado) {
    return <Navigate to={rutaDespuesDeLogin(roles, location.state)} replace />
  }

  const errorGeneral =
    registro.error && !(esErrorApi(registro.error) && [400, 409].includes(registro.error.status)) ? registro.error : null

  return (
    <div className="mx-auto grid max-w-5xl overflow-hidden rounded-tarjeta border border-border bg-card shadow-tarjeta md:grid-cols-[2fr_3fr]">
      <PanelBienvenida />

      <div className="px-6 py-8 md:px-10 md:py-12">
        <h1 className="font-titulo text-3xl font-semibold">Crear cuenta</h1>
        <p className="mt-1 text-sm text-muted-foreground">Regístrate para reservar y seguir tus estancias.</p>

        {errorGeneral && <AvisoError className="mt-6" error={errorGeneral} />}

        <form
          noValidate
          onSubmit={handleSubmit((datos) => registro.mutate(datos))}
          className="mt-6 grid gap-4 sm:grid-cols-2"
        >
          <Campo id="nombre" etiqueta="Nombre completo" error={errors.nombre?.message} className="sm:col-span-2">
            <Input autoComplete="name" {...register('nombre')} />
          </Campo>
          <Campo id="correo" etiqueta="Correo" error={errors.correo?.message} className="sm:col-span-2">
            <Input type="email" autoComplete="email" inputMode="email" {...register('correo')} />
          </Campo>
          <Campo
            id="contrasena"
            etiqueta="Contraseña"
            error={errors.contrasena?.message}
            ayuda="Mínimo 8 caracteres, con una mayúscula y un número."
            className="sm:col-span-2"
          >
            <Input type="password" autoComplete="new-password" {...register('contrasena')} />
          </Campo>
          <Campo id="telefono" etiqueta="Teléfono" error={errors.telefono?.message}>
            <Input type="tel" autoComplete="tel" inputMode="tel" {...register('telefono')} />
          </Campo>
          <Campo id="documento" etiqueta="Documento" error={errors.documento?.message}>
            <Input inputMode="numeric" {...register('documento')} />
          </Campo>
          <Campo id="fechaNacimiento" etiqueta="Fecha de nacimiento" error={errors.fechaNacimiento?.message} className="sm:col-span-2">
            <Input type="date" max={hoy()} autoComplete="bday" {...register('fechaNacimiento')} />
          </Campo>

          <Button type="submit" size="lg" className="mt-2 w-full sm:col-span-2" disabled={registro.isPending}>
            {registro.isPending && <Loader2 aria-hidden="true" className="animate-spin" />}
            Crear cuenta
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" state={location.state} className="font-semibold text-primary underline-offset-4 hover:underline">
            Iniciar sesión
          </Link>
        </p>
      </div>
    </div>
  )
}
