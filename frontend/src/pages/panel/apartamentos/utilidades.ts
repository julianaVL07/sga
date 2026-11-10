import { useAuth } from '@/auth/AuthContext'

/** Las acciones de cambio sobre apartamentos son solo del administrador */
export function useEsAdministrador(): boolean {
  return useAuth().roles.includes('ADMINISTRADOR')
}

/** Valor escrito en pesos ("320.000" o "320000") a número; NaN si no es válido */
export function aPesos(texto: string): number {
  const limpio = texto.replace(/[$.\s]/g, '')
  return limpio === '' ? NaN : Number(limpio)
}
