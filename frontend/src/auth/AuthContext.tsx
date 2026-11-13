import { createContext, useContext } from 'react'
import type { Rol, UsuarioResponse } from '@/api/tipos'
import type { Sesion } from './sesion'

export const LLAVE_USUARIO_ACTUAL = ['usuarios', 'me'] as const

export interface AuthContexto {
  sesion: Sesion | null
  roles: Rol[]
  usuario: UsuarioResponse | undefined
  cargandoUsuario: boolean
  autenticado: boolean
  /** El servidor rechazó el token: se cerró la sesión por vencimiento */
  vencida: boolean
  /** Inicia sesión y devuelve la sesión creada; si falla, lanza el ErrorApi */
  login: (correo: string, contrasena: string) => Promise<Sesion>
  /** Cierra la sesión por decisión del usuario y vuelve al inicio */
  logout: () => void
}

/** El valor lo pone AuthProvider (AuthProvider.tsx) */
export const ContextoAuth = createContext<AuthContexto | null>(null)

/** Ruta a la que volver después de iniciar sesión (la guarda RutaProtegida o el 401) */
export interface EstadoDesde {
  desde?: { pathname: string; search?: string }
}

export function useAuth(): AuthContexto {
  const ctx = useContext(ContextoAuth)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
