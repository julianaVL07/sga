import type { Rol, TokenResponse } from '@/api/tipos'

/** Sesión guardada en sessionStorage: se pierde al cerrar la pestaña */
export interface Sesion {
  token: string
  tipo: string
  expiraEn: string
  roles: Rol[]
}

const CLAVE = 'coral-sesion'

export function sesionVencida(sesion: Sesion): boolean {
  return new Date(sesion.expiraEn).getTime() <= Date.now()
}

export function leerSesion(): Sesion | null {
  try {
    const crudo = sessionStorage.getItem(CLAVE)
    if (!crudo) return null
    const sesion = JSON.parse(crudo) as Sesion
    if (!sesion.token || !sesion.expiraEn || sesionVencida(sesion)) {
      sessionStorage.removeItem(CLAVE)
      return null
    }
    return sesion
  } catch {
    return null
  }
}

export function guardarSesion(token: TokenResponse): Sesion {
  const sesion: Sesion = {
    token: token.token,
    tipo: token.tipo,
    expiraEn: token.expiraEn,
    roles: token.roles,
  }
  try {
    sessionStorage.setItem(CLAVE, JSON.stringify(sesion))
  } catch {
    // Sin almacenamiento: la sesión vive solo en memoria mientras la pestaña siga abierta
  }
  return sesion
}

export function borrarSesion(): void {
  try {
    sessionStorage.removeItem(CLAVE)
  } catch {
    // Nada que borrar
  }
}
