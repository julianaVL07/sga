import { addHours, format } from 'date-fns'
import { es } from 'date-fns/locale'
import type { CanalOrigen } from '@/api/reservas'
import { formatearHora } from '@/pages/publico/comun/reglas'

export const NOMBRE_CANAL: Record<CanalOrigen, string> = {
  PORTAL: 'Portal',
  DIRECTO: 'Directo',
  EXTERNO: 'Externo',
}

/** Hora de una fecha en el formato de la app: "7:54 p. m." */
const horaDe = (fecha: Date) => formatearHora(format(fecha, 'HH:mm'))

/** Instante ISO → "10 nov 2026, 7:54 p. m." */
export function formatearInstante(valor: string | null): string {
  if (!valor) return ''
  const fecha = new Date(valor)
  return Number.isNaN(fecha.getTime()) ? valor : `${format(fecha, 'd MMM yyyy', { locale: es })}, ${horaDe(fecha)}`
}

/** Límite para pagar el anticipo: la creación más el plazo de confirmación (RN-21); null si el plazo no vino */
export function fechaLimiteAnticipo(fechaCreacion: string, horas: number | null): Date | null {
  return horas !== null ? addHours(new Date(fechaCreacion), horas) : null
}

/** "7:54 a. m. del miércoles 11 de noviembre": la hora primero, para que la frase no termine en "m." */
export function limiteAnticipo(fechaCreacion: string, horas: number | null): string | null {
  const limite = fechaLimiteAnticipo(fechaCreacion, horas)
  return limite ? `${horaDe(limite)} del ${format(limite, "EEEE d 'de' MMMM", { locale: es })}` : null
}
