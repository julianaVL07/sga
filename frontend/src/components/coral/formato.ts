import { format, isValid, parseISO } from 'date-fns'
import { es } from 'date-fns/locale'

const NUMERO_COP = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 })

/** Pesos colombianos sin decimales: 1240000 → "$1.240.000" */
export function formatearDinero(valor: number): string {
  const signo = valor < 0 ? '-' : ''
  return `${signo}$${NUMERO_COP.format(Math.abs(Math.round(valor)))}`
}

/** Convierte "2026-12-12" (o un instante ISO) en fecha local */
export function aFecha(valor: string | Date): Date {
  return typeof valor === 'string' ? parseISO(valor) : valor
}

/** Fecha corta en español de Colombia: "sáb 12 dic". Acepta un patrón de date-fns. */
export function formatearFecha(valor: string | Date, patron = 'EEE d MMM'): string {
  const fecha = aFecha(valor)
  if (!isValid(fecha)) return ''
  return format(fecha, patron, { locale: es }).replace(/\./g, '')
}
