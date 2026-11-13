import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ContextoTema, guardarTema, leerTema, type Tema } from './tema'

export function TemaProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(leerTema)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', tema === 'dark')
    guardarTema(tema)
  }, [tema])

  const alternarTema = useCallback(() => setTema((t) => (t === 'dark' ? 'light' : 'dark')), [])
  const valor = useMemo(() => ({ tema, alternarTema }), [tema, alternarTema])

  return <ContextoTema.Provider value={valor}>{children}</ContextoTema.Provider>
}
