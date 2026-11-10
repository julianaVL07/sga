import { keepPreviousData, useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { cliente } from './cliente'
import { consultarApartamento, listarApartamentos, listarTemporadas, llavesPublicas } from './publico'
import type {
  ApartamentoDetalleResponse,
  ApartamentoResponse,
  BloqueoResponse,
  FechaISO,
  Pagina,
  TemporadaResponse,
} from './tipos'

/*
 * Administración de apartamentos, servicio (preparación) y novedades.
 * Las consultas públicas (listar, detalle, temporadas) están en publico.ts; aquí solo se reutilizan.
 * Las páginas empiezan en 0 y traen 10 elementos, como el backend.
 */

// ---------- Tipos (docs/api/openapi.yaml) ----------

export type GravedadNovedad = 'BAJA' | 'MEDIA' | 'ALTA'

export interface NovedadResponse {
  id: string
  identificacionApartamento: string
  fecha: FechaISO
  autor: string
  descripcion: string
  gravedad: GravedadNovedad
  fechaAtencion: FechaISO | null
}

export interface RegistrarNovedadRequest {
  autor: string
  descripcion: string
  gravedad: GravedadNovedad
}

export interface ImagenRequest {
  url: string
  principal: boolean
}

export interface TarifaRequest {
  idTemporada: string
  valorPorOcupante: number
}

export interface RegistrarApartamentoRequest {
  identificacion: string
  nombre: string
  descripcion: string | null
  dormitorios: number
  capacidadMaxima: number
  dotacion: string[]
  imagenes: ImagenRequest[]
  tarifas?: TarifaRequest[] | null
}

export interface ActualizarApartamentoRequest {
  nombre: string
  descripcion: string | null
  dormitorios: number
  capacidadMaxima: number
  dotacion: string[]
}

export interface ActualizacionApartamentoResponse {
  apartamento: ApartamentoDetalleResponse
  /** Reservas activas o futuras cuyo grupo supera la nueva capacidad */
  reservasSobreCapacidad: string[]
  advertencia: string | null
}

export interface RegistroTarifaResponse {
  idTemporada: string
  valorPorOcupante: number
  vigenteDesde: FechaISO | null
  /** null si rige hoy */
  vigenteHasta: FechaISO | null
  vigente: boolean
}

export interface RegistrarBloqueoRequest {
  fechaInicio: FechaISO
  /** Primera noche que ya no queda bloqueada */
  fechaFin: FechaISO
  motivo: string
}

// ---------- Funciones ----------

const ruta = (id: string) => `/api/apartamentos/${encodeURIComponent(id)}`

/** Todos los apartamentos no retirados, recorriendo las páginas del listado público */
export async function listarTodosLosApartamentos(maximoPaginas = 10): Promise<ApartamentoResponse[]> {
  const primera = await listarApartamentos(0)
  const resto = await Promise.all(
    Array.from({ length: Math.max(0, Math.min(primera.totalPaginas, maximoPaginas) - 1) }, (_, i) =>
      listarApartamentos(i + 1),
    ),
  )
  return [primera, ...resto].flatMap((p) => p.contenido)
}

/** Todas las temporadas del alojamiento, recorriendo las páginas del listado público */
export async function listarTodasLasTemporadas(maximoPaginas = 10): Promise<TemporadaResponse[]> {
  const primera = await listarTemporadas(0)
  const resto = await Promise.all(
    Array.from({ length: Math.max(0, Math.min(primera.totalPaginas, maximoPaginas) - 1) }, (_, i) =>
      listarTemporadas(i + 1),
    ),
  )
  return [primera, ...resto].flatMap((p) => p.contenido)
}

export async function registrarApartamento(datos: RegistrarApartamentoRequest): Promise<ApartamentoDetalleResponse> {
  const { data } = await cliente.post<ApartamentoDetalleResponse>('/api/apartamentos', datos)
  return data
}

export async function actualizarApartamento(
  id: string,
  datos: ActualizarApartamentoRequest,
): Promise<ActualizacionApartamentoResponse> {
  const { data } = await cliente.put<ActualizacionApartamentoResponse>(ruta(id), datos)
  return data
}

export async function definirTarifas(id: string, tarifas: TarifaRequest[]): Promise<ApartamentoDetalleResponse> {
  const { data } = await cliente.put<ApartamentoDetalleResponse>(`${ruta(id)}/tarifas`, { tarifas })
  return data
}

export async function historicoTarifas(id: string, pagina = 0): Promise<Pagina<RegistroTarifaResponse>> {
  const { data } = await cliente.get<Pagina<RegistroTarifaResponse>>(`${ruta(id)}/tarifas/historico`, {
    params: { pagina },
  })
  return data
}

export async function activarApartamento(id: string): Promise<ApartamentoDetalleResponse> {
  const { data } = await cliente.put<ApartamentoDetalleResponse>(`${ruta(id)}/activar`)
  return data
}

export async function retirarApartamento(id: string): Promise<ApartamentoDetalleResponse> {
  const { data } = await cliente.put<ApartamentoDetalleResponse>(`${ruta(id)}/retirar`)
  return data
}

export async function registrarBloqueo(id: string, datos: RegistrarBloqueoRequest): Promise<BloqueoResponse> {
  const { data } = await cliente.post<BloqueoResponse>(`${ruta(id)}/bloqueos`, datos)
  return data
}

export async function listarPendientesPreparacion(pagina = 0): Promise<Pagina<ApartamentoResponse>> {
  const { data } = await cliente.get<Pagina<ApartamentoResponse>>('/api/apartamentos/pendientes-preparacion', {
    params: { pagina },
  })
  return data
}

export async function iniciarPreparacion(id: string): Promise<ApartamentoResponse> {
  const { data } = await cliente.put<ApartamentoResponse>(`${ruta(id)}/iniciar-preparacion`)
  return data
}

export async function terminarPreparacion(id: string): Promise<ApartamentoResponse> {
  const { data } = await cliente.put<ApartamentoResponse>(`${ruta(id)}/terminar-preparacion`)
  return data
}

export async function ponerFueraDeServicio(id: string): Promise<ApartamentoResponse> {
  const { data } = await cliente.put<ApartamentoResponse>(`${ruta(id)}/poner-fuera-de-servicio`)
  return data
}

export async function restablecerServicio(id: string): Promise<ApartamentoResponse> {
  const { data } = await cliente.put<ApartamentoResponse>(`${ruta(id)}/restablecer-servicio`)
  return data
}

export async function registrarNovedad(id: string, datos: RegistrarNovedadRequest): Promise<NovedadResponse> {
  const { data } = await cliente.post<NovedadResponse>(`${ruta(id)}/novedades`, datos)
  return data
}

export async function historialNovedades(id: string, pagina = 0): Promise<Pagina<NovedadResponse>> {
  const { data } = await cliente.get<Pagina<NovedadResponse>>(`${ruta(id)}/novedades`, { params: { pagina } })
  return data
}

// ---------- Llaves de React Query ----------

/* Todas empiezan por 'apartamentos' para que al invalidar esa raíz se refresquen también las públicas */
export const llavesApartamentos = {
  raiz: ['apartamentos'] as const,
  todos: ['apartamentos', 'todos'] as const,
  pendientes: (pagina: number) => ['apartamentos', 'pendientes-preparacion', pagina] as const,
  historicoTarifas: (id: string, pagina: number) => ['apartamentos', 'tarifas-historico', id, pagina] as const,
  novedades: (id: string, pagina: number) => ['apartamentos', 'novedades', id, pagina] as const,
  temporadasTodas: ['temporadas', 'todas'] as const,
}

// ---------- Consultas ----------

export function useTodosLosApartamentos() {
  return useQuery({ queryKey: llavesApartamentos.todos, queryFn: () => listarTodosLosApartamentos() })
}

/** Detalle de varios apartamentos a la vez; comparte la caché con useApartamento de publico.ts */
export function useDetallesApartamentos(ids: string[]) {
  return useQueries({
    queries: ids.map((id) => ({
      queryKey: llavesPublicas.apartamento(id),
      queryFn: () => consultarApartamento(id),
    })),
  })
}

export function useTodasLasTemporadas() {
  return useQuery({
    queryKey: llavesApartamentos.temporadasTodas,
    queryFn: () => listarTodasLasTemporadas(),
    staleTime: 5 * 60 * 1000,
  })
}

export function usePendientesPreparacion(pagina = 0) {
  return useQuery({
    queryKey: llavesApartamentos.pendientes(pagina),
    queryFn: () => listarPendientesPreparacion(pagina),
    placeholderData: keepPreviousData,
  })
}

export function useHistoricoTarifas(id: string | undefined, pagina = 0, habilitada = true) {
  return useQuery({
    queryKey: llavesApartamentos.historicoTarifas(id ?? '', pagina),
    queryFn: () => historicoTarifas(id as string, pagina),
    enabled: !!id && habilitada,
    placeholderData: keepPreviousData,
  })
}

export function useNovedades(id: string | undefined, pagina = 0) {
  return useQuery({
    queryKey: llavesApartamentos.novedades(id ?? '', pagina),
    queryFn: () => historialNovedades(id as string, pagina),
    enabled: !!id,
    placeholderData: keepPreviousData,
  })
}

// ---------- Mutaciones ----------

/** Cualquier cambio en un apartamento refresca apartamentos y calendario (bloqueos, estado operativo) */
function useRefrescarApartamentos() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: llavesApartamentos.raiz }),
      queryClient.invalidateQueries({ queryKey: ['calendario'] }),
    ])
}

export function useRegistrarApartamento() {
  const refrescar = useRefrescarApartamentos()
  return useMutation({ mutationFn: registrarApartamento, onSuccess: refrescar })
}

export function useActualizarApartamento(id: string) {
  const refrescar = useRefrescarApartamentos()
  return useMutation({
    mutationFn: (datos: ActualizarApartamentoRequest) => actualizarApartamento(id, datos),
    onSuccess: refrescar,
  })
}

export function useDefinirTarifas(id: string) {
  const refrescar = useRefrescarApartamentos()
  return useMutation({ mutationFn: (tarifas: TarifaRequest[]) => definirTarifas(id, tarifas), onSuccess: refrescar })
}

export function useActivarApartamento() {
  const refrescar = useRefrescarApartamentos()
  return useMutation({ mutationFn: activarApartamento, onSuccess: refrescar })
}

export function useRetirarApartamento() {
  const refrescar = useRefrescarApartamentos()
  return useMutation({ mutationFn: retirarApartamento, onSuccess: refrescar })
}

export function useRegistrarBloqueo(id: string) {
  const refrescar = useRefrescarApartamentos()
  return useMutation({
    mutationFn: (datos: RegistrarBloqueoRequest) => registrarBloqueo(id, datos),
    onSuccess: refrescar,
  })
}

export function useIniciarPreparacion() {
  const refrescar = useRefrescarApartamentos()
  return useMutation({ mutationFn: iniciarPreparacion, onSuccess: refrescar })
}

export function useTerminarPreparacion() {
  const refrescar = useRefrescarApartamentos()
  return useMutation({ mutationFn: terminarPreparacion, onSuccess: refrescar })
}

export function usePonerFueraDeServicio() {
  const refrescar = useRefrescarApartamentos()
  return useMutation({ mutationFn: ponerFueraDeServicio, onSuccess: refrescar })
}

export function useRestablecerServicio() {
  const refrescar = useRefrescarApartamentos()
  return useMutation({ mutationFn: restablecerServicio, onSuccess: refrescar })
}

/** El apartamento se elige en el formulario, por eso viaja con los datos */
export function useRegistrarNovedad() {
  const refrescar = useRefrescarApartamentos()
  return useMutation({
    mutationFn: ({ id, datos }: { id: string; datos: RegistrarNovedadRequest }) => registrarNovedad(id, datos),
    onSuccess: refrescar,
  })
}
