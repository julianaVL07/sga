import { keepPreviousData, useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { cliente } from './cliente'
import type { EstadoReserva, FechaISO, Pagina } from './tipos'

/*
 * Operación de recepción: llegadas y salidas del día, reservas por estado, calendario de ocupación
 * y el registro de llegada y salida. Roles: RECEPCIONISTA o ADMINISTRADOR.
 * Las páginas empiezan en 0 y traen 10 elementos, como el backend.
 */

// ---------- Tipos (docs/api/openapi.yaml) ----------

export type CanalOrigen = 'PORTAL' | 'DIRECTO' | 'EXTERNO'

export interface ReservaResumenResponse {
  codigo: string
  estado: EstadoReserva
  apartamento: string
  fechaEntrada: FechaISO
  fechaSalida: FechaISO
  nombreTitular: string
  totalOcupantes: number
  canalOrigen: CanalOrigen
  /** Valor congelado de la estancia, en pesos */
  valorTotal: number
}

export type EstadoNoche = 'LIBRE' | 'RESERVADA' | 'BLOQUEADA'

export interface NocheCalendarioResponse {
  /** La noche va de esta fecha a la siguiente */
  fecha: FechaISO
  estado: EstadoNoche
  codigoReserva: string | null
  estadoReserva: EstadoReserva | null
  motivoBloqueo: string | null
}

export interface CalendarioOcupacionResponse {
  apartamento: string
  desde: FechaISO
  /** Excluida */
  hasta: FechaISO
  noches: NocheCalendarioResponse[]
}

/** Lo que devuelve registrar llegada o salida (ReservaDetalleResponse); aquí solo se leen estos campos */
export interface ReservaActualizada {
  codigo: string
  estado: EstadoReserva
}

export interface FiltrosReservas {
  estado?: EstadoReserva
  apartamento?: string
  /** Fecha de entrada desde (incluida) */
  desde?: FechaISO
  /** Fecha de entrada hasta (incluida) */
  hasta?: FechaISO
}

/** El backend acepta máximo 62 noches por consulta del calendario */
export const MAXIMO_NOCHES_CALENDARIO = 62

// ---------- Funciones ----------

export async function listarLlegadas(fecha: FechaISO, pagina = 0): Promise<Pagina<ReservaResumenResponse>> {
  const { data } = await cliente.get<Pagina<ReservaResumenResponse>>('/api/reservas/llegadas', {
    params: { fecha, pagina },
  })
  return data
}

export async function listarSalidas(fecha: FechaISO, pagina = 0): Promise<Pagina<ReservaResumenResponse>> {
  const { data } = await cliente.get<Pagina<ReservaResumenResponse>>('/api/reservas/salidas', {
    params: { fecha, pagina },
  })
  return data
}

export async function listarReservas(filtros: FiltrosReservas, pagina = 0): Promise<Pagina<ReservaResumenResponse>> {
  const { data } = await cliente.get<Pagina<ReservaResumenResponse>>('/api/reservas', {
    params: { ...filtros, pagina },
  })
  return data
}

/** Recorre todas las páginas (hasta un tope) y junta el contenido */
export async function listarTodasLasReservas(
  filtros: FiltrosReservas,
  maximoPaginas = 10,
): Promise<ReservaResumenResponse[]> {
  const primera = await listarReservas(filtros, 0)
  const resto = await Promise.all(
    Array.from({ length: Math.max(0, Math.min(primera.totalPaginas, maximoPaginas) - 1) }, (_, i) =>
      listarReservas(filtros, i + 1),
    ),
  )
  return [primera, ...resto].flatMap((p) => p.contenido)
}

export async function consultarCalendario(
  apartamento: string,
  desde: FechaISO,
  hasta: FechaISO,
): Promise<CalendarioOcupacionResponse> {
  const { data } = await cliente.get<CalendarioOcupacionResponse>('/api/reservas/calendario', {
    params: { apartamento, desde, hasta },
  })
  return data
}

export async function registrarLlegada(codigo: string): Promise<ReservaActualizada> {
  const { data } = await cliente.put<ReservaActualizada>(
    `/api/reservas/${encodeURIComponent(codigo)}/registrar-llegada`,
  )
  return data
}

export async function registrarSalida(codigo: string): Promise<ReservaActualizada> {
  const { data } = await cliente.put<ReservaActualizada>(`/api/reservas/${encodeURIComponent(codigo)}/registrar-salida`)
  return data
}

// ---------- Llaves de React Query ----------

export const llavesOcupacion = {
  todas: ['reservas'] as const,
  llegadas: (fecha: FechaISO, pagina: number) => ['reservas', 'llegadas', fecha, pagina] as const,
  salidas: (fecha: FechaISO, pagina: number) => ['reservas', 'salidas', fecha, pagina] as const,
  lista: (filtros: FiltrosReservas, pagina: number) => ['reservas', 'lista', filtros, pagina] as const,
  listaCompleta: (filtros: FiltrosReservas) => ['reservas', 'lista-completa', filtros] as const,
  calendarioTodo: ['calendario'] as const,
  calendario: (apartamento: string, desde: FechaISO, hasta: FechaISO) =>
    ['calendario', apartamento, desde, hasta] as const,
}

// ---------- Hooks ----------

export function useLlegadas(fecha: FechaISO, pagina = 0) {
  return useQuery({
    queryKey: llavesOcupacion.llegadas(fecha, pagina),
    queryFn: () => listarLlegadas(fecha, pagina),
    placeholderData: keepPreviousData,
  })
}

export function useSalidas(fecha: FechaISO, pagina = 0) {
  return useQuery({
    queryKey: llavesOcupacion.salidas(fecha, pagina),
    queryFn: () => listarSalidas(fecha, pagina),
    placeholderData: keepPreviousData,
  })
}

export function useReservas(filtros: FiltrosReservas, pagina = 0) {
  return useQuery({
    queryKey: llavesOcupacion.lista(filtros, pagina),
    queryFn: () => listarReservas(filtros, pagina),
    placeholderData: keepPreviousData,
  })
}

/** Todas las reservas que cumplen el filtro, sin paginar (para cruzar datos en pantalla) */
export function useTodasLasReservas(filtros: FiltrosReservas, habilitada = true) {
  return useQuery({
    queryKey: llavesOcupacion.listaCompleta(filtros),
    queryFn: () => listarTodasLasReservas(filtros),
    enabled: habilitada,
  })
}

/** Una consulta por apartamento: el backend da el calendario de un apartamento a la vez */
export function useCalendarios(apartamentos: string[], desde: FechaISO, hasta: FechaISO) {
  return useQueries({
    queries: apartamentos.map((apartamento) => ({
      queryKey: llavesOcupacion.calendario(apartamento, desde, hasta),
      queryFn: () => consultarCalendario(apartamento, desde, hasta),
      placeholderData: keepPreviousData,
    })),
  })
}

/** Después de una llegada o salida cambian las reservas, el calendario y el estado de los apartamentos */
function useRefrescarOcupacion() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: llavesOcupacion.todas }),
      queryClient.invalidateQueries({ queryKey: llavesOcupacion.calendarioTodo }),
      queryClient.invalidateQueries({ queryKey: ['apartamentos'] }),
    ])
}

export function useRegistrarLlegada() {
  const refrescar = useRefrescarOcupacion()
  return useMutation({ mutationFn: registrarLlegada, onSuccess: refrescar })
}

export function useRegistrarSalida() {
  const refrescar = useRefrescarOcupacion()
  return useMutation({ mutationFn: registrarSalida, onSuccess: refrescar })
}
