import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { consultarMiPerfil, iniciarSesion } from '@/api/auth'
import { registrarManejadores } from '@/api/cliente'
import type { Rol, UsuarioResponse } from '@/api/tipos'
import { borrarSesion, guardarSesion, leerSesion, type Sesion } from './sesion'

export const LLAVE_USUARIO_ACTUAL = ['usuarios', 'me'] as const

interface AuthContexto {
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

const Contexto = createContext<AuthContexto | null>(null)

/** Ruta a la que volver después de iniciar sesión (la guarda RutaProtegida o el 401) */
export interface EstadoDesde {
  desde?: { pathname: string; search?: string }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [sesion, setSesion] = useState<Sesion | null>(leerSesion)
  // El servidor rechazó el token (401): el login muestra el aviso de sesión vencida
  const [vencida, setVencida] = useState(false)

  const limpiar = useCallback(() => {
    borrarSesion()
    setSesion(null)
    queryClient.removeQueries({ queryKey: LLAVE_USUARIO_ACTUAL })
  }, [queryClient])

  const consultaUsuario = useQuery({
    queryKey: [...LLAVE_USUARIO_ACTUAL, sesion?.token],
    queryFn: consultarMiPerfil,
    enabled: !!sesion,
    staleTime: 5 * 60 * 1000,
  })

  // 401 y 403 que detecta el cliente HTTP
  useEffect(() => {
    registrarManejadores({
      alNoAutenticado: () => {
        const { pathname, search } = window.location
        // RutaProtegida también lee "vencida" para redirigir al mismo /login?expirado=1
        setVencida(true)
        limpiar()
        navigate('/login?expirado=1', { replace: true, state: { desde: { pathname, search } } satisfies EstadoDesde })
      },
      alSinPermiso: () => navigate('/sin-permiso', { replace: true }),
    })
  }, [limpiar, navigate])

  // Cierre automático cuando vence el token
  useEffect(() => {
    if (!sesion) return
    const restante = new Date(sesion.expiraEn).getTime() - Date.now()
    const vencer = () => {
      limpiar()
      toast.warning('Tu sesión venció', { description: 'Inicia sesión de nuevo para continuar.' })
    }
    if (restante <= 0) {
      vencer()
      return
    }
    const temporizador = window.setTimeout(vencer, restante)
    return () => window.clearTimeout(temporizador)
  }, [sesion, limpiar])

  const login = useCallback(
    async (correo: string, contrasena: string) => {
      const token = await iniciarSesion({ correo, contrasena })
      const nueva = guardarSesion(token)
      setVencida(false)
      setSesion(nueva)
      return nueva
    },
    [],
  )

  const logout = useCallback(() => {
    limpiar()
    if (location.pathname !== '/') navigate('/', { replace: true })
    toast.success('Cerraste sesión')
  }, [limpiar, location.pathname, navigate])

  const valor = useMemo<AuthContexto>(
    () => ({
      sesion,
      roles: sesion?.roles ?? [],
      usuario: sesion ? consultaUsuario.data : undefined,
      cargandoUsuario: !!sesion && consultaUsuario.isPending,
      autenticado: !!sesion,
      vencida,
      login,
      logout,
    }),
    [sesion, vencida, consultaUsuario.data, consultaUsuario.isPending, login, logout],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useAuth(): AuthContexto {
  const ctx = useContext(Contexto)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
