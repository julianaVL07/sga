import type { EstadoConflictoCanal, ResultadoEventoCanal, TipoEventoCanal } from '@/api/canales'

export type Tono = 'exito' | 'alerta' | 'peligro' | 'neutro'

export const ESTADO_CONFLICTO: Record<EstadoConflictoCanal, string> = {
  PENDIENTE: 'Pendiente',
  RESUELTO: 'Resuelto',
}

export const TIPO_EVENTO: Record<TipoEventoCanal, string> = {
  CONSULTA_DISPONIBILIDAD: 'Consulta de disponibilidad',
  CREACION_RESERVA: 'Creación de reserva',
  CANCELACION_RESERVA: 'Cancelación de reserva',
}

export const RESULTADO_EVENTO: Record<ResultadoEventoCanal, string> = {
  ACEPTADO: 'Aceptado',
  DUPLICADO_IGNORADO: 'Duplicado ignorado',
  RECHAZADO_CONFLICTO: 'Conflicto',
  RECHAZADO_INVALIDO: 'Datos inválidos',
  RECHAZADO_CREDENCIAL: 'Credencial inválida',
  RECHAZADO_CANAL_INACTIVO: 'Canal inactivo',
}

export const TONO_RESULTADO: Record<ResultadoEventoCanal, Tono> = {
  ACEPTADO: 'exito',
  DUPLICADO_IGNORADO: 'neutro',
  RECHAZADO_CONFLICTO: 'alerta',
  RECHAZADO_INVALIDO: 'alerta',
  RECHAZADO_CREDENCIAL: 'peligro',
  RECHAZADO_CANAL_INACTIVO: 'peligro',
}

/** "12 nov 2026, 10:30" */
export const PATRON_FECHA_HORA = 'd MMM yyyy, HH:mm'
