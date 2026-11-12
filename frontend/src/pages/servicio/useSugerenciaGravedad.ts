import { useEffect, useRef, useState } from 'react'
import { sugerirGravedad, type GravedadNovedad } from '@/api/apartamentos'

/** Espera desde la última tecla antes de pedir la sugerencia */
const ESPERA_MS = 800
/** Con menos letras no hay de qué sugerir */
const LARGO_MINIMO = 8

export type SugerenciaGravedad =
  | { estado: 'inactiva' }
  | { estado: 'disponible'; gravedad: GravedadNovedad; explicacion: string }
  | { estado: 'no-disponible' }

/**
 * Sugerencia de gravedad de la IA para la descripción que se está escribiendo. Pide la sugerencia 800 ms después
 * de la última tecla y cancela la petición anterior. Si el backend responde disponible=false o falla por cualquier
 * motivo, queda en "no-disponible" y el formulario sigue funcionando igual.
 */
export function useSugerenciaGravedad() {
  const [sugerencia, setSugerencia] = useState<SugerenciaGravedad>({ estado: 'inactiva' })
  const temporizador = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const peticion = useRef<AbortController | null>(null)

  const cancelar = () => {
    clearTimeout(temporizador.current)
    peticion.current?.abort()
    peticion.current = null
  }

  useEffect(() => cancelar, [])

  const pedirSugerencia = (descripcion: string) => {
    cancelar()
    const texto = descripcion.trim()
    if (texto.length < LARGO_MINIMO) {
      setSugerencia({ estado: 'inactiva' })
      return
    }
    temporizador.current = setTimeout(async () => {
      const controlador = new AbortController()
      peticion.current = controlador
      try {
        const respuesta = await sugerirGravedad(texto, controlador.signal)
        if (controlador.signal.aborted) return
        setSugerencia(
          respuesta.disponible && respuesta.gravedad && respuesta.explicacion
            ? { estado: 'disponible', gravedad: respuesta.gravedad, explicacion: respuesta.explicacion }
            : { estado: 'no-disponible' },
        )
      } catch {
        if (!controlador.signal.aborted) setSugerencia({ estado: 'no-disponible' })
      }
    }, ESPERA_MS)
  }

  return { sugerencia, pedirSugerencia }
}
