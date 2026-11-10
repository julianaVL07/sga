import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import { cliente } from './cliente'
import type {
  AlojamientoResponse,
  ApartamentoDetalleResponse,
  ApartamentoDisponibleResponse,
  ApartamentoResponse,
  CotizacionResponse,
  CotizarEstanciaRequest,
  FiltrosDisponibles,
  Pagina,
  PoliticaCancelacionResponse,
  TemporadaResponse,
} from './tipos'

/*
 * Consultas públicas: responden sin token, para el visitante y para cualquier rol.
 * Cada consulta tiene su función (para usarla donde se necesite) y su hook de React Query.
 * Las páginas empiezan en 0 y traen 10 elementos, como el backend.
 */

// ---------- Funciones ----------

export async function listarApartamentos(pagina = 0): Promise<Pagina<ApartamentoResponse>> {
  const { data } = await cliente.get<Pagina<ApartamentoResponse>>('/api/apartamentos', { params: { pagina } })
  return data
}

export async function buscarDisponibles(
  filtros: FiltrosDisponibles,
  pagina = 0,
): Promise<Pagina<ApartamentoDisponibleResponse>> {
  const { data } = await cliente.get<Pagina<ApartamentoDisponibleResponse>>('/api/apartamentos/disponibles', {
    params: { ...limpiar(filtros), pagina },
  })
  return data
}

export async function consultarApartamento(id: string): Promise<ApartamentoDetalleResponse> {
  const { data } = await cliente.get<ApartamentoDetalleResponse>(`/api/apartamentos/${encodeURIComponent(id)}`)
  return data
}

export async function consultarAlojamiento(): Promise<AlojamientoResponse> {
  const { data } = await cliente.get<AlojamientoResponse>('/api/alojamiento')
  return data
}

export async function listarTemporadas(pagina = 0): Promise<Pagina<TemporadaResponse>> {
  const { data } = await cliente.get<Pagina<TemporadaResponse>>('/api/temporadas', { params: { pagina } })
  return data
}

export async function consultarTemporada(id: string): Promise<TemporadaResponse> {
  const { data } = await cliente.get<TemporadaResponse>(`/api/temporadas/${encodeURIComponent(id)}`)
  return data
}

export async function consultarPoliticaVigente(): Promise<PoliticaCancelacionResponse> {
  const { data } = await cliente.get<PoliticaCancelacionResponse>('/api/politica-cancelacion')
  return data
}

export async function cotizarEstancia(solicitud: CotizarEstanciaRequest): Promise<CotizacionResponse> {
  const { data } = await cliente.post<CotizacionResponse>('/api/cotizaciones', solicitud)
  return data
}

/** Quita los filtros vacíos para no enviarlos como parámetros */
function limpiar(filtros: FiltrosDisponibles): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(filtros).filter(
      ([, valor]) => valor !== undefined && valor !== null && valor !== '' && !(Array.isArray(valor) && valor.length === 0),
    ),
  )
}

// ---------- Llaves de React Query ----------

export const llavesPublicas = {
  apartamentos: (pagina: number) => ['apartamentos', 'lista', pagina] as const,
  disponibles: (filtros: FiltrosDisponibles, pagina: number) => ['apartamentos', 'disponibles', filtros, pagina] as const,
  apartamento: (id: string) => ['apartamentos', 'detalle', id] as const,
  alojamiento: ['alojamiento'] as const,
  temporadas: (pagina: number) => ['temporadas', 'lista', pagina] as const,
  temporada: (id: string) => ['temporadas', 'detalle', id] as const,
  politicaVigente: ['politica-cancelacion', 'vigente'] as const,
}

// ---------- Hooks ----------

export function useApartamentos(pagina = 0) {
  return useQuery({
    queryKey: llavesPublicas.apartamentos(pagina),
    queryFn: () => listarApartamentos(pagina),
    placeholderData: keepPreviousData,
  })
}

/** Busca solo cuando hay filtros (entrada, salida y ocupantes) */
export function useApartamentosDisponibles(filtros: FiltrosDisponibles | null, pagina = 0) {
  return useQuery({
    queryKey: llavesPublicas.disponibles(filtros ?? ({} as FiltrosDisponibles), pagina),
    queryFn: () => buscarDisponibles(filtros as FiltrosDisponibles, pagina),
    enabled: !!filtros?.entrada && !!filtros?.salida && !!filtros?.ocupantes,
    placeholderData: keepPreviousData,
  })
}

export function useApartamento(id: string | undefined) {
  return useQuery({
    queryKey: llavesPublicas.apartamento(id ?? ''),
    queryFn: () => consultarApartamento(id as string),
    enabled: !!id,
  })
}

export function useAlojamiento() {
  return useQuery({
    queryKey: llavesPublicas.alojamiento,
    queryFn: consultarAlojamiento,
    staleTime: 5 * 60 * 1000,
  })
}

export function useTemporadas(pagina = 0) {
  return useQuery({
    queryKey: llavesPublicas.temporadas(pagina),
    queryFn: () => listarTemporadas(pagina),
    placeholderData: keepPreviousData,
  })
}

export function useTemporada(id: string | undefined) {
  return useQuery({
    queryKey: llavesPublicas.temporada(id ?? ''),
    queryFn: () => consultarTemporada(id as string),
    enabled: !!id,
  })
}

export function usePoliticaVigente() {
  return useQuery({
    queryKey: llavesPublicas.politicaVigente,
    queryFn: consultarPoliticaVigente,
    staleTime: 5 * 60 * 1000,
  })
}

/** Cotizar no cambia nada en el servidor, pero se dispara a pedido: por eso es una mutación */
export function useCotizar() {
  return useMutation({ mutationFn: cotizarEstancia })
}
