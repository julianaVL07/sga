import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { cliente } from './cliente'
import type { CanalOrigen, MedioPago } from './reservas'
import type { FechaISO } from './tipos'

/*
 * Reportes del administrador: ocupación, ingresos y reservas por canal de un periodo.
 * desde y hasta van incluidas. Rol: ADMINISTRADOR.
 */

// ---------- Tipos (docs/api/openapi.yaml) ----------

export interface PeriodoReporte {
  desde: FechaISO
  /** Incluida */
  hasta: FechaISO
}

export interface ReporteOcupacionResponse extends PeriodoReporte {
  nochesPeriodo: number
  apartamentosActivos: number
  /** apartamentosActivos × nochesPeriodo */
  nochesDisponibles: number
  nochesVendidas: number
  /** De 0 a 100, con dos decimales */
  porcentajeOcupacion: number
  apartamentos: {
    apartamento: string
    nochesVendidas: number
    /** nochesVendidas / nochesPeriodo × 100 */
    porcentajeOcupacion: number
    /** false si hoy no está a la venta */
    activo: boolean
  }[]
}

export interface ReporteIngresosResponse extends PeriodoReporte {
  /** En pesos */
  total: number
  cantidadPagos: number
  medios: {
    medio: MedioPago
    cantidadPagos: number
    total: number
    porcentaje: number
  }[]
}

export interface ReporteReservasPorCanalResponse extends PeriodoReporte {
  totalReservas: number
  canales: {
    canal: CanalOrigen
    total: number
    /** PENDIENTE, CONFIRMADA o EN_CURSO */
    activas: number
    finalizadas: number
    canceladas: number
    noShow: number
    porcentaje: number
  }[]
}

// ---------- Funciones ----------

export async function reporteOcupacion(periodo: PeriodoReporte): Promise<ReporteOcupacionResponse> {
  const { data } = await cliente.get<ReporteOcupacionResponse>('/api/reportes/ocupacion', { params: periodo })
  return data
}

export async function reporteIngresos(periodo: PeriodoReporte): Promise<ReporteIngresosResponse> {
  const { data } = await cliente.get<ReporteIngresosResponse>('/api/reportes/ingresos', { params: periodo })
  return data
}

export async function reporteReservasPorCanal(periodo: PeriodoReporte): Promise<ReporteReservasPorCanalResponse> {
  const { data } = await cliente.get<ReporteReservasPorCanalResponse>('/api/reportes/reservas-por-canal', {
    params: periodo,
  })
  return data
}

// ---------- Llaves de React Query ----------

/* Empiezan por 'reservas' para que cualquier cambio en reservas o folios también refresque los reportes */
export const llavesReportes = {
  ocupacion: (p: PeriodoReporte) => ['reservas', 'reportes', 'ocupacion', p.desde, p.hasta] as const,
  ingresos: (p: PeriodoReporte) => ['reservas', 'reportes', 'ingresos', p.desde, p.hasta] as const,
  canales: (p: PeriodoReporte) => ['reservas', 'reportes', 'canales', p.desde, p.hasta] as const,
}

// ---------- Consultas ----------

export function useReporteOcupacion(periodo: PeriodoReporte, habilitada = true) {
  return useQuery({
    queryKey: llavesReportes.ocupacion(periodo),
    queryFn: () => reporteOcupacion(periodo),
    enabled: habilitada,
    placeholderData: keepPreviousData,
  })
}

export function useReporteIngresos(periodo: PeriodoReporte, habilitada = true) {
  return useQuery({
    queryKey: llavesReportes.ingresos(periodo),
    queryFn: () => reporteIngresos(periodo),
    enabled: habilitada,
    placeholderData: keepPreviousData,
  })
}

export function useReporteReservasPorCanal(periodo: PeriodoReporte, habilitada = true) {
  return useQuery({
    queryKey: llavesReportes.canales(periodo),
    queryFn: () => reporteReservasPorCanal(periodo),
    enabled: habilitada,
    placeholderData: keepPreviousData,
  })
}
