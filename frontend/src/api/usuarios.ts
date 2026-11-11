import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { LLAVE_USUARIO_ACTUAL } from '@/auth/AuthContext'
import { cliente } from './cliente'
import type { FechaISO, Pagina, Rol, UsuarioResponse } from './tipos'

/*
 * Usuarios: administración (solo ADMINISTRADOR) y el perfil propio (cualquier rol).
 * La consulta del perfil propio (consultarMiPerfil) está en auth.ts y la usa AuthContext.
 * Las páginas empiezan en 0 y traen 10 elementos, como el backend.
 */

// ---------- Tipos (docs/api/openapi.yaml) ----------

export interface CrearUsuarioRequest {
  nombre: string
  correo: string
  contrasena: string
  telefono: string
  documento: string
  fechaNacimiento: FechaISO
  rol: Rol
}

export interface ActualizarPerfilRequest {
  nombre: string
  telefono: string
  /** Opcional; debe empezar por http:// o https:// */
  fotoUrl?: string
}

export interface CambioContrasenaRequest {
  contrasenaActual: string
  contrasenaNueva: string
}

export interface FiltrosUsuarios {
  rol?: Rol
  activo?: boolean
}

// ---------- Funciones ----------

const ruta = (id: number) => `/api/usuarios/${id}`

export async function listarUsuarios(filtros: FiltrosUsuarios, pagina = 0): Promise<Pagina<UsuarioResponse>> {
  const params: Record<string, unknown> = { pagina }
  if (filtros.rol) params.rol = filtros.rol
  if (filtros.activo !== undefined) params.activo = filtros.activo
  const { data } = await cliente.get<Pagina<UsuarioResponse>>('/api/usuarios', { params })
  return data
}

export async function crearUsuario(datos: CrearUsuarioRequest): Promise<UsuarioResponse> {
  const { data } = await cliente.post<UsuarioResponse>('/api/usuarios', datos)
  return data
}

export async function cambiarRolUsuario(id: number, rol: Rol): Promise<UsuarioResponse> {
  const { data } = await cliente.put<UsuarioResponse>(`${ruta(id)}/rol`, { rol })
  return data
}

export async function desactivarUsuario(id: number): Promise<UsuarioResponse> {
  const { data } = await cliente.put<UsuarioResponse>(`${ruta(id)}/desactivar`)
  return data
}

export async function actualizarMiPerfil(datos: ActualizarPerfilRequest): Promise<UsuarioResponse> {
  const { data } = await cliente.put<UsuarioResponse>('/api/usuarios/me', datos)
  return data
}

export async function cambiarMiContrasena(datos: CambioContrasenaRequest): Promise<void> {
  await cliente.put('/api/usuarios/me/contrasena', datos)
}

// ---------- Llaves de React Query ----------

export const llavesUsuarios = {
  raiz: ['usuarios', 'lista'] as const,
  lista: (filtros: FiltrosUsuarios, pagina: number) => ['usuarios', 'lista', filtros, pagina] as const,
}

// ---------- Consultas ----------

export function useUsuarios(filtros: FiltrosUsuarios, pagina = 0) {
  return useQuery({
    queryKey: llavesUsuarios.lista(filtros, pagina),
    queryFn: () => listarUsuarios(filtros, pagina),
    placeholderData: keepPreviousData,
  })
}

// ---------- Mutaciones ----------

function useRefrescarUsuarios() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: llavesUsuarios.raiz })
}

export function useCrearUsuario() {
  const refrescar = useRefrescarUsuarios()
  return useMutation({ mutationFn: crearUsuario, onSuccess: refrescar })
}

export function useCambiarRolUsuario() {
  const refrescar = useRefrescarUsuarios()
  return useMutation({
    mutationFn: ({ id, rol }: { id: number; rol: Rol }) => cambiarRolUsuario(id, rol),
    onSuccess: refrescar,
  })
}

export function useDesactivarUsuario() {
  const refrescar = useRefrescarUsuarios()
  return useMutation({ mutationFn: desactivarUsuario, onSuccess: refrescar })
}

/** El perfil propio lo lee AuthContext: al guardarlo se refresca el usuario de la sesión (nombre y foto del menú) */
export function useActualizarMiPerfil() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: actualizarMiPerfil,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LLAVE_USUARIO_ACTUAL }),
  })
}

export function useCambiarMiContrasena() {
  return useMutation({ mutationFn: cambiarMiContrasena })
}
