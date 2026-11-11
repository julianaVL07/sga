import { esErrorApi } from '@/api/cliente'

/** Errores por campo de un 400 de validación (vacío si el error no es de la API) */
export function erroresDe(error: unknown): Record<string, string | undefined> {
  return esErrorApi(error) ? error.errores : {}
}
