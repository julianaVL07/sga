import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cliente, ErrorApi } from './cliente'
import type { ErrorResponse, EstadoReserva, FechaISO, OcupanteRequest, Pagina } from './tipos'

/*
 * Reservas: crear, listar, consultar, modificar, confirmar, cancelar y no-show, y el folio en solo lectura.
 * Las consultas públicas (apartamentos, temporadas, política, cotización) están en publico.ts.
 * Las páginas empiezan en 0 y traen 10 elementos, como el backend.
 */

// ---------- Tipos (docs/api/openapi.yaml) ----------

export type CanalOrigen = 'PORTAL' | 'DIRECTO' | 'EXTERNO'

export interface TitularRequest {
  nombre: string
  tipoDocumento: string
  numeroDocumento: string
  email: string
  telefono: string
}

export interface CrearReservaRequest {
  identificacionApartamento: string
  fechaEntrada: FechaISO
  fechaSalida: FechaISO
  /** "16:30"; opcional al crear, obligatoria para confirmar (RN-09) */
  horaEstimadaLlegada?: string
  canalOrigen: CanalOrigen
  titular: TitularRequest
  ocupantes: OcupanteRequest[]
  cantidadMascotas?: number
  pesoMascotaMasPesadaKg?: number
}

/** Todos los campos son opcionales; el que no se envía se conserva */
export interface ModificarReservaRequest {
  identificacionApartamento?: string
  fechaEntrada?: FechaISO
  fechaSalida?: FechaISO
  ocupantes?: OcupanteRequest[]
  cantidadMascotas?: number
  pesoMascotaMasPesadaKg?: number
}

export interface TitularResponse {
  nombre: string
  tipoDocumento: string
  numeroDocumento: string
  email: string
  telefono: string
}

export interface OcupanteResponse {
  nombre: string
  tipoDocumento: string | null
  numeroDocumento: string | null
  fechaNacimiento: FechaISO
}

export interface ReservaDetalleResponse {
  codigo: string
  estado: EstadoReserva
  canalOrigen: CanalOrigen
  identificacionApartamento: string
  fechaEntrada: FechaISO
  fechaSalida: FechaISO
  noches: number
  horaEstimadaLlegada: string | null
  titular: TitularResponse
  ocupantes: OcupanteResponse[]
  totalOcupantes: number
  /** Valor congelado en pesos (RN-22) */
  valorTotal: number
  versionPoliticaCancelacion: number
  cantidadMascotas: number
  pesoMascotaMasPesadaKg: number
  fechaCreacion: string
  fechaConfirmacion: string | null
  motivoCancelacion: string | null
}

export interface ReservaResumenResponse {
  codigo: string
  estado: EstadoReserva
  apartamento: string
  fechaEntrada: FechaISO
  fechaSalida: FechaISO
  nombreTitular: string
  totalOcupantes: number
  canalOrigen: CanalOrigen
  valorTotal: number
}

export interface ModificacionReservaResponse {
  reserva: ReservaDetalleResponse
  /** En pesos; negativa si baja el valor */
  diferencia: number
}

export interface RetencionResponse {
  codigo: string
  estado: EstadoReserva
  /** En pesos (RN-13) */
  retencion: number
}

export type MedioPago = 'TARJETA_DEBITO_CREDITO' | 'TRANSFERENCIA_BANCARIA'

export type TipoCargo = 'ALOJAMIENTO' | 'SERVICIO_ADICIONAL' | 'AJUSTE_MODIFICACION' | 'PENALIDAD_CANCELACION_NOSHOW'

export interface CargoResponse {
  tipo: TipoCargo
  concepto: string
  valor: number
  fecha: FechaISO
}

export interface PagoResponse {
  valor: number
  medio: MedioPago
  fecha: FechaISO
}

export interface FolioResponse {
  codigoReserva: string
  fechaApertura: FechaISO
  depositoGarantia: number
  cargos: CargoResponse[]
  pagos: PagoResponse[]
  totalCargos: number
  totalPagado: number
  /** Cargos menos pagos; negativo si hay saldo a favor */
  saldo: number
  cerrado: boolean
  fechaCierre: FechaISO | null
  autorizadoPor: string | null
  motivoAutorizacion: string | null
}

export interface FiltrosReservas {
  estado?: EstadoReserva
  apartamento?: string
  canal?: CanalOrigen
  /** Número de documento del titular */
  titular?: string
  desde?: FechaISO
  hasta?: FechaISO
}

// ---------- Funciones ----------

const ruta = (codigo: string) => `/api/reservas/${encodeURIComponent(codigo)}`

/** Quita los filtros vacíos para no enviarlos como parámetros */
function limpiar<T extends object>(filtros: T): Record<string, unknown> {
  return Object.fromEntries(Object.entries(filtros).filter(([, v]) => v !== undefined && v !== null && v !== ''))
}

export async function listarReservas(filtros: FiltrosReservas, pagina = 0): Promise<Pagina<ReservaResumenResponse>> {
  const { data } = await cliente.get<Pagina<ReservaResumenResponse>>('/api/reservas', {
    params: { ...limpiar(filtros), pagina },
  })
  return data
}

export async function listarMisReservas(
  estado: EstadoReserva | undefined,
  pagina = 0,
): Promise<Pagina<ReservaResumenResponse>> {
  const { data } = await cliente.get<Pagina<ReservaResumenResponse>>('/api/reservas/mias', {
    params: limpiar({ estado, pagina }),
  })
  return data
}

export async function consultarReserva(codigo: string): Promise<ReservaDetalleResponse> {
  const { data } = await cliente.get<ReservaDetalleResponse>(ruta(codigo))
  return data
}

/**
 * Crea la reserva. El 403 por canal (el huésped solo reserva por el portal y a su nombre) no se deja pasar
 * al interceptor, que llevaría a /sin-permiso: llega al asistente como ErrorApi para explicarlo ahí.
 */
export async function crearReserva(datos: CrearReservaRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await cliente.post<ReservaDetalleResponse | ErrorResponse>('/api/reservas', datos, {
    validateStatus: (status) => (status >= 200 && status < 300) || status === 403,
  })
  if (respuesta.status === 403) {
    const cuerpo = respuesta.data as Partial<ErrorResponse> | undefined
    throw new ErrorApi(403, cuerpo?.mensaje ?? 'No tienes permiso para crear esta reserva.', {}, cuerpo?.path)
  }
  return respuesta.data as ReservaDetalleResponse
}

export async function modificarReserva(
  codigo: string,
  datos: ModificarReservaRequest,
): Promise<ModificacionReservaResponse> {
  const { data } = await cliente.put<ModificacionReservaResponse>(ruta(codigo), datos)
  return data
}

export async function confirmarReserva(codigo: string, horaEstimadaLlegada?: string): Promise<ReservaDetalleResponse> {
  const { data } = await cliente.put<ReservaDetalleResponse>(
    `${ruta(codigo)}/confirmar`,
    horaEstimadaLlegada ? { horaEstimadaLlegada } : {},
  )
  return data
}

export async function cancelarReserva(codigo: string, motivo: string): Promise<RetencionResponse> {
  const { data } = await cliente.put<RetencionResponse>(`${ruta(codigo)}/cancelar`, { motivo })
  return data
}

export async function declararNoShow(codigo: string): Promise<RetencionResponse> {
  const { data } = await cliente.put<RetencionResponse>(`${ruta(codigo)}/no-show`)
  return data
}

export async function consultarFolio(codigo: string): Promise<FolioResponse> {
  const { data } = await cliente.get<FolioResponse>(`${ruta(codigo)}/folio`)
  return data
}

// ---------- Llaves de React Query ----------

export const llavesReservas = {
  raiz: ['reservas'] as const,
  lista: (filtros: FiltrosReservas, pagina: number) => ['reservas', 'lista', filtros, pagina] as const,
  mias: (estado: EstadoReserva | undefined, pagina: number) => ['reservas', 'mias', estado ?? 'todas', pagina] as const,
  detalle: (codigo: string) => ['reservas', 'detalle', codigo] as const,
  folio: (codigo: string) => ['reservas', 'folio', codigo] as const,
}

// ---------- Consultas ----------

export function useReservas(filtros: FiltrosReservas, pagina = 0) {
  return useQuery({
    queryKey: llavesReservas.lista(filtros, pagina),
    queryFn: () => listarReservas(filtros, pagina),
    placeholderData: keepPreviousData,
  })
}

export function useMisReservas(estado: EstadoReserva | undefined, pagina = 0) {
  return useQuery({
    queryKey: llavesReservas.mias(estado, pagina),
    queryFn: () => listarMisReservas(estado, pagina),
    placeholderData: keepPreviousData,
  })
}

export function useReserva(codigo: string | undefined) {
  return useQuery({
    queryKey: llavesReservas.detalle(codigo ?? ''),
    queryFn: () => consultarReserva(codigo as string),
    enabled: !!codigo,
  })
}

export function useFolio(codigo: string | undefined) {
  return useQuery({
    queryKey: llavesReservas.folio(codigo ?? ''),
    queryFn: () => consultarFolio(codigo as string),
    enabled: !!codigo,
    retry: false,
  })
}

// ---------- Mutaciones ----------

/** Cualquier cambio en una reserva refresca reservas, calendario y disponibilidad */
function useRefrescarReservas() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: llavesReservas.raiz }),
      queryClient.invalidateQueries({ queryKey: ['calendario'] }),
      queryClient.invalidateQueries({ queryKey: ['apartamentos', 'disponibles'] }),
    ])
}

export function useCrearReserva() {
  const refrescar = useRefrescarReservas()
  return useMutation({ mutationFn: crearReserva, onSuccess: refrescar })
}

export function useModificarReserva(codigo: string) {
  const refrescar = useRefrescarReservas()
  return useMutation({
    mutationFn: (datos: ModificarReservaRequest) => modificarReserva(codigo, datos),
    onSuccess: refrescar,
  })
}

export function useConfirmarReserva(codigo: string) {
  const refrescar = useRefrescarReservas()
  return useMutation({
    mutationFn: (hora?: string) => confirmarReserva(codigo, hora),
    onSuccess: refrescar,
  })
}

export function useCancelarReserva(codigo: string) {
  const refrescar = useRefrescarReservas()
  return useMutation({ mutationFn: (motivo: string) => cancelarReserva(codigo, motivo), onSuccess: refrescar })
}

export function useDeclararNoShow(codigo: string) {
  const refrescar = useRefrescarReservas()
  return useMutation({ mutationFn: () => declararNoShow(codigo), onSuccess: refrescar })
}
