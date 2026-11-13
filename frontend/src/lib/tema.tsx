import { createContext, useContext } from 'react'

export type Tema = 'light' | 'dark'

const CLAVE = 'coral-tema'

export interface TemaContexto {
  tema: Tema
  alternarTema: () => void
}

/** El valor lo pone TemaProvider (TemaProvider.tsx) */
export const ContextoTema = createContext<TemaContexto | null>(null)

export function leerTema(): Tema {
  try {
    return localStorage.getItem(CLAVE) === 'oscuro' ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

export function guardarTema(tema: Tema) {
  try {
    localStorage.setItem(CLAVE, tema === 'dark' ? 'oscuro' : 'claro')
  } catch {
    // Sin almacenamiento (modo privado): el tema dura solo esta visita
  }
}

export function useTema(): TemaContexto {
  const ctx = useContext(ContextoTema)
  if (!ctx) throw new Error('useTema debe usarse dentro de TemaProvider')
  return ctx
}
