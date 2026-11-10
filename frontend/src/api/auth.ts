import { cliente } from './cliente'
import type {
  LoginRequest,
  RegistroUsuarioRequest,
  TokenResponse,
  UsuarioRegistradoResponse,
  UsuarioResponse,
} from './tipos'

export async function iniciarSesion(credenciales: LoginRequest): Promise<TokenResponse> {
  const { data } = await cliente.post<TokenResponse>('/api/auth/login', credenciales)
  return data
}

export async function registrarHuesped(datos: RegistroUsuarioRequest): Promise<UsuarioRegistradoResponse> {
  const { data } = await cliente.post<UsuarioRegistradoResponse>('/api/auth/registro', datos)
  return data
}

export async function consultarMiPerfil(): Promise<UsuarioResponse> {
  const { data } = await cliente.get<UsuarioResponse>('/api/usuarios/me')
  return data
}
