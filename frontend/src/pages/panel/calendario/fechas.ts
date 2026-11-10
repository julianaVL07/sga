import { addDays, format, getDay, parseISO, startOfWeek } from 'date-fns'
import type { NocheCalendarioResponse } from '@/api/ocupacion'
import type { EstadoReserva, FechaISO, TemporadaResponse } from '@/api/tipos'

/** Fecha de hoy en el navegador, "2026-11-10" */
export function hoyISO(): FechaISO {
  return format(new Date(), 'yyyy-MM-dd')
}

export function sumarDias(fecha: FechaISO, dias: number): FechaISO {
  return format(addDays(parseISO(fecha), dias), 'yyyy-MM-dd')
}

/** Lunes de la semana de la fecha */
export function inicioDeSemana(fecha: FechaISO): FechaISO {
  return format(startOfWeek(parseISO(fecha), { weekStartsOn: 1 }), 'yyyy-MM-dd')
}

/** Lista de noches desde la fecha dada */
export function noches(desde: FechaISO, cantidad: number): FechaISO[] {
  return Array.from({ length: cantidad }, (_, i) => sumarDias(desde, i))
}

/** Viernes y sábado: las noches de fin de semana */
export function esNocheDeFinDeSemana(fecha: FechaISO): boolean {
  const dia = getDay(parseISO(fecha))
  return dia === 5 || dia === 6
}

/**
 * Temporada que rige una noche: la que tiene un rango que la contiene (inicio incluido, fin excluido,
 * como RangoFechas del backend); si ninguna, la temporada base.
 */
export function temporadaDeFecha(fecha: FechaISO, temporadas: TemporadaResponse[]): TemporadaResponse | undefined {
  const conRango = temporadas.find((t) => t.rangos.some((r) => r.fechaInicio <= fecha && fecha < r.fechaFin))
  return conRango ?? temporadas.find((t) => t.tipo === 'BASE')
}

// ---------- Tramos del calendario ----------

export type Tramo =
  | {
      tipo: 'reserva'
      inicio: number
      longitud: number
      desde: FechaISO
      /** Primera noche fuera del tramo */
      hasta: FechaISO
      codigo: string
      estado: EstadoReserva
    }
  | { tipo: 'bloqueo'; inicio: number; longitud: number; desde: FechaISO; hasta: FechaISO; motivo: string }

/** Agrupa noches seguidas de la misma reserva o del mismo bloqueo en tramos para dibujar barras */
export function tramosDelCalendario(nochesCalendario: NocheCalendarioResponse[]): Tramo[] {
  const tramos: Tramo[] = []
  nochesCalendario.forEach((noche, i) => {
    const ultimo = tramos[tramos.length - 1]
    const continua = ultimo && ultimo.inicio + ultimo.longitud === i

    if (noche.estado === 'RESERVADA' && noche.codigoReserva) {
      if (continua && ultimo.tipo === 'reserva' && ultimo.codigo === noche.codigoReserva) {
        ultimo.longitud++
        ultimo.hasta = sumarDias(noche.fecha, 1)
        return
      }
      tramos.push({
        tipo: 'reserva',
        inicio: i,
        longitud: 1,
        desde: noche.fecha,
        hasta: sumarDias(noche.fecha, 1),
        codigo: noche.codigoReserva,
        estado: noche.estadoReserva ?? 'CONFIRMADA',
      })
    } else if (noche.estado === 'BLOQUEADA') {
      const motivo = noche.motivoBloqueo ?? 'Bloqueado'
      if (continua && ultimo.tipo === 'bloqueo' && ultimo.motivo === motivo) {
        ultimo.longitud++
        ultimo.hasta = sumarDias(noche.fecha, 1)
        return
      }
      tramos.push({ tipo: 'bloqueo', inicio: i, longitud: 1, desde: noche.fecha, hasta: sumarDias(noche.fecha, 1), motivo })
    }
  })
  return tramos
}
