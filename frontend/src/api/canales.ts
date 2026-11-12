import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cliente } from './cliente'
import type { FechaISO, Pagina } from './tipos'

/*
 * Canales de venta externos (solo ADMINISTRADOR): los canales y su credencial, los conflictos que dejan sus
 * reservas (RN-18) y la bitácora de mensajes (7.8). Las páginas empiezan en 0 y traen 10 elementos.
 * La credencial en claro solo llega al crear el canal o al regenerarla: nunca se guarda en la caché.
 */

// ---------- Tipos (docs/api/openapi.yaml) ----------

/** "2026-11-12T10:30:00" */
export type FechaHoraISO = string

export type TipoEventoCanal = 'CONSULTA_DISPONIBILIDAD' | 'CREACION_RESERVA' | 'CANCELACION_RESERVA'

export type ResultadoEventoCanal =
  | 'ACEPTADO'
  | 'RECHAZADO_CONFLICTO'
  | 'RECHAZADO_INVALIDO'
  | 'DUPLICADO_IGNORADO'
  | 'RECHAZADO_CREDENCIAL'
  | 'RECHAZADO_CANAL_INACTIVO'

export type EstadoConflictoCanal = 'PENDIENTE' | 'RESUELTO'

export interface CanalVentaResponse {
  nombre: string
  tipo: 'EXTERNO'
  activo: boolean
  fechaCreacion: FechaHoraISO
}

/** Solo en la respuesta de crear y de regenerar: la credencial se muestra una sola vez */
export interface CredencialCanalResponse extends CanalVentaResponse {
  credencial: string
}

export interface ConflictoCanalResponse {
  id: string
  canal: string
  identificadorExterno: string
  detalle: string
  fecha: FechaISO
  estado: EstadoConflictoCanal
  motivoResolucion: string | null
  resueltoPor: string | null
  fechaResolucion: FechaISO | null
}

export interface EventoBitacoraResponse {
  id: string
  fecha: FechaHoraISO
  canal: string
  tipoEvento: TipoEventoCanal
  identificadorExterno: string | null
  resultado: ResultadoEventoCanal
}

export interface FiltrosBitacora {
  canal?: string
  desde?: FechaISO
  hasta?: FechaISO
}

// ---------- Funciones ----------

const ruta = (nombre: string) => `/api/canales/${encodeURIComponent(nombre)}`

export async function listarCanales(pagina = 0): Promise<Pagina<CanalVentaResponse>> {
  const { data } = await cliente.get<Pagina<CanalVentaResponse>>('/api/canales', { params: { pagina } })
  return data
}

export async function crearCanal(nombre: string): Promise<CredencialCanalResponse> {
  const { data } = await cliente.post<CredencialCanalResponse>('/api/canales', { nombre })
  return data
}

export async function regenerarCredencialCanal(nombre: string): Promise<CredencialCanalResponse> {
  const { data } = await cliente.put<CredencialCanalResponse>(`${ruta(nombre)}/regenerar-credencial`)
  return data
}

export async function desactivarCanal(nombre: string): Promise<CanalVentaResponse> {
  const { data } = await cliente.put<CanalVentaResponse>(`${ruta(nombre)}/desactivar`)
  return data
}

export async function listarConflictosCanal(
  estado: EstadoConflictoCanal | undefined,
  pagina = 0,
): Promise<Pagina<ConflictoCanalResponse>> {
  const params: Record<string, unknown> = { pagina }
  if (estado) params.estado = estado
  const { data } = await cliente.get<Pagina<ConflictoCanalResponse>>('/api/canales/conflictos', { params })
  return data
}

export async function resolverConflictoCanal(id: string, motivo: string): Promise<ConflictoCanalResponse> {
  const { data } = await cliente.put<ConflictoCanalResponse>(
    `/api/canales/conflictos/${encodeURIComponent(id)}/resolver`,
    { motivo },
  )
  return data
}

export async function listarBitacoraCanales(
  filtros: FiltrosBitacora,
  pagina = 0,
): Promise<Pagina<EventoBitacoraResponse>> {
  const params: Record<string, unknown> = { pagina }
  if (filtros.canal) params.canal = filtros.canal
  if (filtros.desde) params.desde = filtros.desde
  if (filtros.hasta) params.hasta = filtros.hasta
  const { data } = await cliente.get<Pagina<EventoBitacoraResponse>>('/api/canales/eventos', { params })
  return data
}

// ---------- Llaves de React Query ----------

export const llavesCanales = {
  raiz: ['canales'] as const,
  lista: (pagina: number) => ['canales', 'lista', pagina] as const,
  conflictos: (estado: EstadoConflictoCanal | undefined, pagina: number) =>
    ['canales', 'conflictos', estado ?? 'todos', pagina] as const,
  bitacora: (filtros: FiltrosBitacora, pagina: number) => ['canales', 'bitacora', filtros, pagina] as const,
}

// ---------- Consultas ----------

export function useCanales(pagina = 0) {
  return useQuery({
    queryKey: llavesCanales.lista(pagina),
    queryFn: () => listarCanales(pagina),
    placeholderData: keepPreviousData,
  })
}

export function useConflictosCanal(estado: EstadoConflictoCanal | undefined, pagina = 0) {
  return useQuery({
    queryKey: llavesCanales.conflictos(estado, pagina),
    queryFn: () => listarConflictosCanal(estado, pagina),
    placeholderData: keepPreviousData,
  })
}

export function useBitacoraCanales(filtros: FiltrosBitacora, pagina = 0) {
  return useQuery({
    queryKey: llavesCanales.bitacora(filtros, pagina),
    queryFn: () => listarBitacoraCanales(filtros, pagina),
    placeholderData: keepPreviousData,
  })
}

// ---------- Mutaciones ----------

function useRefrescarCanales() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: llavesCanales.raiz })
}

export function useCrearCanal() {
  const refrescar = useRefrescarCanales()
  return useMutation({ mutationFn: crearCanal, onSuccess: refrescar })
}

export function useRegenerarCredencialCanal() {
  const refrescar = useRefrescarCanales()
  return useMutation({ mutationFn: regenerarCredencialCanal, onSuccess: refrescar })
}

export function useDesactivarCanal() {
  const refrescar = useRefrescarCanales()
  return useMutation({ mutationFn: desactivarCanal, onSuccess: refrescar })
}

export function useResolverConflictoCanal() {
  const refrescar = useRefrescarCanales()
  return useMutation({
    mutationFn: ({ id, motivo }: { id: string; motivo: string }) => resolverConflictoCanal(id, motivo),
    onSuccess: refrescar,
  })
}
