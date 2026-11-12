import { cliente } from './cliente'
import type {
  LoginRequest,
  RegistroUsuarioRequest,
  RestablecerContrasenaRequest,
  SolicitarRecuperacionRequest,
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

/** Pide el código para restablecer la contraseña. El backend responde 202 aunque el correo no exista */
export async function solicitarRecuperacion(datos: SolicitarRecuperacionRequest): Promise<void> {
  await cliente.post('/api/auth/recuperacion/solicitar', datos)
}

/** Cambia la contraseña con el código que llegó al correo (204) */
export async function restablecerContrasena(datos: RestablecerContrasenaRequest): Promise<void> {
  await cliente.post('/api/auth/recuperacion/restablecer', datos)
}

export async function consultarMiPerfil(): Promise<UsuarioResponse> {
  const { data } = await cliente.get<UsuarioResponse>('/api/usuarios/me')
  return data
}
