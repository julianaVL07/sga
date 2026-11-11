import { format, parseISO, subDays, subYears } from 'date-fns'
import type { FechaISO, OcupanteRequest } from '@/api/tipos'
import { esFechaValida, esMenorNoPaga } from './reglas'

/** "3 huéspedes · 1 menor" */
export function resumenHuespedes(huespedes: string[], umbral: number | null, referencia?: string): string {
  const menores = huespedes.filter((h) => esMenorNoPaga(h, umbral, referencia)).length
  const total = `${huespedes.length} ${huespedes.length === 1 ? 'huésped' : 'huéspedes'}`
  return menores > 0 ? `${total} · ${menores} ${menores === 1 ? 'menor' : 'menores'}` : total
}

/*
 * La estancia que se busca viaja entre páginas en la URL:
 *   ?entrada=2026-12-12&salida=2026-12-14&h=1990-05-20&h=2021-03-02
 * Cada "h" es un huésped con su fecha de nacimiento (puede venir vacía: se cuenta como adulto).
 */

export interface Estancia {
  entrada: string
  salida: string
  /** Fecha de nacimiento de cada huésped; '' si no se indicó */
  huespedes: string[]
}

export function leerEstancia(params: URLSearchParams): Estancia {
  const huespedes = params.getAll('h')
  return {
    entrada: params.get('entrada') ?? '',
    salida: params.get('salida') ?? '',
    huespedes: huespedes.length > 0 ? huespedes : [''],
  }
}

export function estanciaAParams(estancia: Estancia, extra: Record<string, string | undefined> = {}): URLSearchParams {
  const params = new URLSearchParams()
  if (estancia.entrada) params.set('entrada', estancia.entrada)
  if (estancia.salida) params.set('salida', estancia.salida)
  estancia.huespedes.forEach((h) => params.append('h', h))
  Object.entries(extra).forEach(([clave, valor]) => {
    if (valor) params.set(clave, valor)
  })
  return params
}

/**
 * Fecha que representa a alguien que paga noche cuando el huésped no indicó su fecha de nacimiento:
 * cumple el umbral de edad el día anterior a la llegada. Sin umbral configurado nadie queda exento,
 * así que basta con la fecha de llegada.
 */
export function fechaQuePaga(umbral: number | null, referencia?: string): FechaISO {
  const base = esFechaValida(referencia) ? parseISO(referencia) : new Date()
  return format(umbral !== null ? subDays(subYears(base, umbral), 1) : base, 'yyyy-MM-dd')
}

/** Solo se envían las fechas de nacimiento si todos los huéspedes la tienen */
export function fechasCompletas(huespedes: string[]): FechaISO[] | undefined {
  return huespedes.length > 0 && huespedes.every(esFechaValida) ? huespedes : undefined
}

/** Ocupantes para cotizar: los que no tienen fecha cuentan como adultos (pagan noche) */
export function ocupantesParaCotizar(
  huespedes: { nombre?: string; fechaNacimiento: string }[],
  umbral: number | null,
  referencia?: string,
): OcupanteRequest[] {
  const adulto = fechaQuePaga(umbral, referencia)
  return huespedes.map((h, i) => ({
    nombre: h.nombre?.trim() || `Huésped ${i + 1}`,
    fechaNacimiento: esFechaValida(h.fechaNacimiento) ? h.fechaNacimiento : adulto,
  }))
}
