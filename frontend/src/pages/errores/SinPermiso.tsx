import { Link } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { rutaInicioPorRol } from '@/auth/roles'
import { Vacio } from '@/components/coral'
import { Button } from '@/components/ui/button'

export default function SinPermiso() {
  const { autenticado, roles } = useAuth()
  return (
    <div className="mx-auto max-w-xl rounded-tarjeta border border-border bg-card shadow-tarjeta">
      <h1 className="sr-only">Sin permiso</h1>
      <Vacio
        icono={ShieldAlert}
        titulo="No tienes permiso para ver esta página"
        descripcion="Tu cuenta no tiene acceso a esta sección. Si crees que es un error, comunícate con la administración de Coral."
        accion={
          <Button asChild>
            <Link to={autenticado ? rutaInicioPorRol(roles) : '/'}>Volver a mi inicio</Link>
          </Button>
        }
      />
    </div>
  )
}
