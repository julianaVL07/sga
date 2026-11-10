import axios, { AxiosError } from 'axios'
import { borrarSesion, leerSesion } from '@/auth/sesion'
import type { ErrorResponse } from './tipos'

/**
 * Error normalizado de la API. Las pantallas leen `status`, `message` (el `mensaje` del backend)
 * y `errores` (campo → mensaje, en los 400 de validación).
 */
export class ErrorApi extends Error {
  readonly status: number
  readonly errores: Record<string, string>
  readonly path?: string

  constructor(status: number, mensaje: string, errores: Record<string, string> = {}, path?: string) {
    super(mensaje)
    this.name = 'ErrorApi'
    this.status = status
    this.errores = errores
    this.path = path
  }
}

export function esErrorApi(error: unknown): error is ErrorApi {
  return error instanceof ErrorApi
}

/** Mensaje para mostrar de cualquier error */
export function mensajeDeError(error: unknown): string {
  if (esErrorApi(error) || error instanceof Error) return error.message
  return 'Ocurrió un error inesperado.'
}

interface Manejadores {
  /** 401 fuera del login: la sesión ya no sirve */
  alNoAutenticado: () => void
  /** 403: el rol no tiene permiso */
  alSinPermiso: () => void
}

let manejadores: Manejadores = {
  alNoAutenticado: () => {},
  alSinPermiso: () => {},
}

/** La sesión registra aquí cómo navegar ante un 401 o un 403 */
export function registrarManejadores(nuevos: Manejadores): void {
  manejadores = nuevos
}

export const cliente = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: { 'Content-Type': 'application/json' },
  // Listas como parámetros repetidos: fechasNacimiento=a&fechasNacimiento=b
  paramsSerializer: { indexes: null },
})

cliente.interceptors.request.use((config) => {
  const sesion = leerSesion()
  if (sesion) {
    config.headers.Authorization = `Bearer ${sesion.token}`
  }
  return config
})

function esRutaDeAutenticacion(url: string | undefined): boolean {
  return !!url && url.includes('/api/auth/')
}

function esErrorResponse(dato: unknown): dato is ErrorResponse {
  return typeof dato === 'object' && dato !== null && 'mensaje' in dato && 'status' in dato
}

cliente.interceptors.response.use(
  (respuesta) => respuesta,
  (error: AxiosError) => {
    if (!error.response) {
      return Promise.reject(
        new ErrorApi(0, 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.'),
      )
    }

    const { status, data } = error.response
    const cuerpo = esErrorResponse(data) ? data : null
    const errorApi = new ErrorApi(
      status,
      cuerpo?.mensaje ?? mensajePorDefecto(status),
      cuerpo?.errores ?? {},
      cuerpo?.path,
    )

    if (status === 401 && !esRutaDeAutenticacion(error.config?.url)) {
      borrarSesion()
      manejadores.alNoAutenticado()
    } else if (status === 403) {
      manejadores.alSinPermiso()
    }
    // 400, 404, 409 y los demás llegan a la pantalla tal cual
    return Promise.reject(errorApi)
  },
)

function mensajePorDefecto(status: number): string {
  switch (status) {
    case 400:
      return 'Revisa los datos ingresados.'
    case 401:
      return 'Debes iniciar sesión.'
    case 403:
      return 'No tienes permiso para realizar esta operación.'
    case 404:
      return 'No encontramos lo que buscabas.'
    case 409:
      return 'No se pudo completar la operación.'
    default:
      return 'Ocurrió un error en el servidor. Inténtalo más tarde.'
  }
}
