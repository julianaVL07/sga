import { useMutation, useQueryClient } from '@tanstack/react-query'
import { cliente } from './cliente'
import { llavesReservas, type FolioResponse, type MedioPago } from './reservas'

/*
 * Acciones sobre el folio de una reserva (recepción y administración).
 * La consulta del folio (useFolio) y sus tipos están en reservas.ts; aquí solo se reutilizan.
 * Cada acción devuelve el folio actualizado: se guarda en la caché y se refrescan las reservas.
 */

// ---------- Tipos (docs/api/openapi.yaml) ----------

export interface RegistrarPagoRequest {
  /** Pesos enteros, mayor que cero */
  valor: number
  medio: MedioPago
}

export interface RegistrarCargoServicioRequest {
  idServicio: string
}

export interface RegistrarCorreccionRequest {
  /** Posición del cargo en la lista `cargos` del folio, desde 0 */
  indiceCargo: number
  motivo: string
}

export interface AutorizarCierreFolioRequest {
  autorizadoPor: string
  motivo: string
}

// ---------- Funciones ----------

const ruta = (codigo: string) => `/api/reservas/${encodeURIComponent(codigo)}/folio`

export async function registrarPago(codigo: string, datos: RegistrarPagoRequest): Promise<FolioResponse> {
  const { data } = await cliente.post<FolioResponse>(`${ruta(codigo)}/pagos`, datos)
  return data
}

export async function registrarCargoServicio(
  codigo: string,
  datos: RegistrarCargoServicioRequest,
): Promise<FolioResponse> {
  const { data } = await cliente.post<FolioResponse>(`${ruta(codigo)}/cargos`, datos)
  return data
}

export async function registrarCorreccion(codigo: string, datos: RegistrarCorreccionRequest): Promise<FolioResponse> {
  const { data } = await cliente.post<FolioResponse>(`${ruta(codigo)}/correcciones`, datos)
  return data
}

export async function cerrarFolio(codigo: string): Promise<FolioResponse> {
  const { data } = await cliente.put<FolioResponse>(`${ruta(codigo)}/cerrar`)
  return data
}

export async function autorizarCierreFolio(codigo: string, datos: AutorizarCierreFolioRequest): Promise<FolioResponse> {
  const { data } = await cliente.post<FolioResponse>(`${ruta(codigo)}/autorizacion-cierre`, datos)
  return data
}

// ---------- Mutaciones ----------

/** Guarda el folio devuelto y refresca el folio y las reservas (el anticipo cubierto cambia lo que se puede hacer) */
function useAlActualizarFolio(codigo: string) {
  const queryClient = useQueryClient()
  return (folio: FolioResponse) => {
    queryClient.setQueryData(llavesReservas.folio(codigo), folio)
    return Promise.all([
      queryClient.invalidateQueries({ queryKey: llavesReservas.folio(codigo) }),
      queryClient.invalidateQueries({ queryKey: llavesReservas.detalle(codigo) }),
    ])
  }
}

export function useRegistrarPago(codigo: string) {
  const alActualizar = useAlActualizarFolio(codigo)
  return useMutation({
    mutationFn: (datos: RegistrarPagoRequest) => registrarPago(codigo, datos),
    onSuccess: alActualizar,
  })
}

export function useRegistrarCargoServicio(codigo: string) {
  const alActualizar = useAlActualizarFolio(codigo)
  return useMutation({
    mutationFn: (idServicio: string) => registrarCargoServicio(codigo, { idServicio }),
    onSuccess: alActualizar,
  })
}

export function useRegistrarCorreccion(codigo: string) {
  const alActualizar = useAlActualizarFolio(codigo)
  return useMutation({
    mutationFn: (datos: RegistrarCorreccionRequest) => registrarCorreccion(codigo, datos),
    onSuccess: alActualizar,
  })
}

export function useCerrarFolio(codigo: string) {
  const alActualizar = useAlActualizarFolio(codigo)
  return useMutation({ mutationFn: () => cerrarFolio(codigo), onSuccess: alActualizar })
}

export function useAutorizarCierreFolio(codigo: string) {
  const alActualizar = useAlActualizarFolio(codigo)
  return useMutation({
    mutationFn: (datos: AutorizarCierreFolioRequest) => autorizarCierreFolio(codigo, datos),
    onSuccess: alActualizar,
  })
}
