import { useTodasLasReservas } from '@/api/ocupacion'

/**
 * Titular de cada reserva activa (en curso, confirmada o pendiente), por código.
 * El calendario solo trae el código; con esto las barras muestran el nombre.
 */
export function useTitulares(): Record<string, string> {
  const enCurso = useTodasLasReservas({ estado: 'EN_CURSO' })
  const confirmadas = useTodasLasReservas({ estado: 'CONFIRMADA' })
  const pendientes = useTodasLasReservas({ estado: 'PENDIENTE' })
  return Object.fromEntries(
    [enCurso.data, confirmadas.data, pendientes.data].flatMap((lista) => lista ?? []).map((r) => [r.codigo, r.nombreTitular]),
  )
}
