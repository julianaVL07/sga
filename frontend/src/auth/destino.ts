import type { Rol } from '@/api/tipos'
import type { EstadoDesde } from './AuthContext'
import { rutaInicioPorRol } from './roles'

const RUTAS_DE_ACCESO = ['/login', '/registro']

/** Vuelve a la ruta protegida de la que venía; si no venía de ninguna, va al inicio de su rol */
export function rutaDespuesDeLogin(roles: Rol[], estado: unknown): string {
  const desde = (estado as EstadoDesde | null)?.desde
  if (desde?.pathname && !RUTAS_DE_ACCESO.includes(desde.pathname)) {
    return desde.pathname + (desde.search ?? '')
  }
  return rutaInicioPorRol(roles)
}
