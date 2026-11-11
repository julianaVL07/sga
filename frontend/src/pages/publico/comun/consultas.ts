import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useTodasLasTemporadas } from '@/api/apartamentos'
import { cotizarEstancia, useAlojamiento } from '@/api/publico'
import type { CotizarEstanciaRequest } from '@/api/tipos'
import { esFechaValida, nochesEntre, reglasDe } from './reglas'

/** Parámetros del alojamiento ya interpretados (umbral de edad, anticipo, recargos...) */
export function useReglasAlojamiento() {
  const alojamiento = useAlojamiento()
  return { alojamiento, reglas: reglasDe(alojamiento.data) }
}

/** Todas las temporadas, para saber la temporada de cada noche */
export function useTemporadasAlojamiento() {
  const consulta = useTodasLasTemporadas()
  return { ...consulta, temporadas: consulta.data ?? [] }
}

/**
 * Cotización automática (POST /api/cotizaciones): se vuelve a pedir cada vez que cambia la solicitud.
 * Cotizar no cambia nada en el servidor, así que se trata como una consulta en caché.
 */
export function useCotizacion(solicitud: CotizarEstanciaRequest | null) {
  const valida =
    !!solicitud &&
    !!solicitud.identificacionApartamento &&
    esFechaValida(solicitud.fechaEntrada) &&
    esFechaValida(solicitud.fechaSalida) &&
    nochesEntre(solicitud.fechaEntrada, solicitud.fechaSalida) > 0 &&
    solicitud.ocupantes.length > 0
  return useQuery({
    queryKey: ['cotizacion', solicitud],
    queryFn: () => cotizarEstancia(solicitud as CotizarEstanciaRequest),
    enabled: valida,
    retry: false,
    placeholderData: keepPreviousData,
    staleTime: 60 * 1000,
  })
}
