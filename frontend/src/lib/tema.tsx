import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

export type Tema = 'light' | 'dark'

const CLAVE = 'coral-tema'

interface TemaContexto {
  tema: Tema
  alternarTema: () => void
}

const Contexto = createContext<TemaContexto | null>(null)

function leerTema(): Tema {
  try {
    return localStorage.getItem(CLAVE) === 'oscuro' ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

export function TemaProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(leerTema)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', tema === 'dark')
    try {
      localStorage.setItem(CLAVE, tema === 'dark' ? 'oscuro' : 'claro')
    } catch {
      // Sin almacenamiento (modo privado): el tema dura solo esta visita
    }
  }, [tema])

  const alternarTema = useCallback(() => setTema((t) => (t === 'dark' ? 'light' : 'dark')), [])
  const valor = useMemo(() => ({ tema, alternarTema }), [tema, alternarTema])

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useTema(): TemaContexto {
  const ctx = useContext(Contexto)
  if (!ctx) throw new Error('useTema debe usarse dentro de TemaProvider')
  return ctx
}
