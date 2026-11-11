import { useState } from 'react'

/** Ejecuta `reiniciar` cada vez que el diálogo se abre (patrón de "ajustar estado al cambiar una prop") */
export function useAlAbrir(abierto: boolean, reiniciar: () => void) {
  const [anterior, setAnterior] = useState(abierto)
  if (abierto !== anterior) {
    setAnterior(abierto)
    if (abierto) reiniciar()
  }
}
