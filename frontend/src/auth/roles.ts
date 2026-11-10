import type { Rol } from '@/api/tipos'

export const ROLES_PANEL: Rol[] = ['RECEPCIONISTA', 'ADMINISTRADOR']
export const ROLES_ADMIN: Rol[] = ['ADMINISTRADOR']
export const ROLES_SERVICIO: Rol[] = ['PERSONAL_SERVICIO', 'ADMINISTRADOR']
export const ROLES_HUESPED: Rol[] = ['HUESPED']

export const NOMBRE_ROL: Record<Rol, string> = {
  HUESPED: 'Huésped',
  RECEPCIONISTA: 'Recepción',
  ADMINISTRADOR: 'Administración',
  PERSONAL_SERVICIO: 'Personal de servicio',
}

/** Página de inicio de cada rol después de iniciar sesión */
export function rutaInicioPorRol(roles: Rol[]): string {
  if (roles.includes('ADMINISTRADOR') || roles.includes('RECEPCIONISTA')) return '/panel'
  if (roles.includes('PERSONAL_SERVICIO')) return '/servicio'
  return '/'
}

export function tieneAlgunRol(roles: Rol[] | undefined, permitidos: Rol[]): boolean {
  return !!roles && roles.some((rol) => permitidos.includes(rol))
}
