import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Rol } from '@/api/tipos'
import { tieneAlgunRol } from './roles'
import { useAuth, type EstadoDesde } from './AuthContext'

interface Props {
  /** Roles que pueden entrar; si no se indica, basta con haber iniciado sesión */
  roles?: Rol[]
  children?: ReactNode
}

/**
 * Sin sesión → /login (y vuelve aquí al iniciar sesión).
 * Con sesión pero sin el rol → /sin-permiso.
 */
export function RutaProtegida({ roles, children }: Props) {
  const { sesion, vencida } = useAuth()
  const location = useLocation()

  if (!sesion) {
    const estado: EstadoDesde = { desde: { pathname: location.pathname, search: location.search } }
    return <Navigate to={vencida ? '/login?expirado=1' : '/login'} replace state={estado} />
  }

  if (roles && !tieneAlgunRol(sesion.roles, roles)) {
    return <Navigate to="/sin-permiso" replace />
  }

  return children ? <>{children}</> : <Outlet />
}
