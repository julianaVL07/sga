import type { CargoResponse, FolioResponse, MedioPago, TipoCargo } from '@/api/reservas'

export const NOMBRE_TIPO_CARGO: Record<TipoCargo, string> = {
  ALOJAMIENTO: 'Alojamiento',
  SERVICIO_ADICIONAL: 'Servicio adicional',
  AJUSTE_MODIFICACION: 'Ajuste',
  PENALIDAD_CANCELACION_NOSHOW: 'Penalidad',
}

export const NOMBRE_MEDIO: Record<MedioPago, string> = {
  TARJETA_DEBITO_CREDITO: 'Tarjeta débito o crédito',
  TRANSFERENCIA_BANCARIA: 'Transferencia bancaria',
}

export type ClaseMovimiento = 'cargo' | 'correccion' | 'pago'

export interface Movimiento {
  clave: string
  /** "C1", "C2"... para los cargos y correcciones; "P1"... para los pagos */
  numero: string
  clase: ClaseMovimiento
  fecha: string
  tipo: string
  concepto: string
  /** Efecto en el saldo: los pagos restan, las correcciones tienen el signo opuesto al cargo */
  valor: number
  /** Solo cargos y correcciones: posición en la lista cargos del folio (para POST /correcciones) */
  indiceCargo?: number
  /** Corrección: número del cargo que corrige ("C3"); vacío si no se encontró el cargo */
  corrige?: string
  /** Cargo: número de la corrección que lo revirtió ("C5") */
  corregidoPor?: string
  /** Corrección: el motivo, sin el prefijo "Reverso de '...'" */
  motivo?: string
}

const numeroCargo = (indice: number) => `C${indice + 1}`

/** Prefijo con el que el backend asienta toda corrección (Cargo.inverso) */
const PREFIJO_REVERSO = "Reverso de '"

const esReverso = (cargo: CargoResponse) => cargo.concepto.startsWith(PREFIJO_REVERSO)

/**
 * El backend asienta la corrección como un cargo del mismo tipo, valor opuesto y concepto
 * "Reverso de '<concepto>': <motivo>" (Cargo.inverso). La respuesta no trae el enlace, así que se reconstruye:
 * cada corrección se asocia al primer cargo anterior que encaja y que todavía no estaba corregido.
 */
export function enlazarCorrecciones(cargos: CargoResponse[]): Map<number, number> {
  const corregidoPor = new Map<number, number>() // cargo original → corrección
  cargos.forEach((cargo, i) => {
    if (!esReverso(cargo)) return
    for (let j = 0; j < i; j++) {
      const original = cargos[j]
      if (
        !corregidoPor.has(j) &&
        ![...corregidoPor.values()].includes(j) &&
        original.tipo === cargo.tipo &&
        original.valor === -cargo.valor &&
        cargo.concepto.startsWith(`Reverso de '${original.concepto}': `)
      ) {
        corregidoPor.set(j, i)
        return
      }
    }
  })
  return corregidoPor
}

/** Cargos, correcciones y pagos en una sola lista, por fecha (y en el orden en que se asentaron) */
export function movimientosDe(folio: FolioResponse): Movimiento[] {
  const enlaces = enlazarCorrecciones(folio.cargos)
  const corrigeA = new Map([...enlaces].map(([original, correccion]) => [correccion, original]))

  const cargos: Movimiento[] = folio.cargos.map((c, i) => {
    const original = corrigeA.get(i)
    const correccion = enlaces.get(i)
    // Una corrección cuyo cargo no se encuentra se muestra igual, con su concepto completo y sin enlace
    const esCorreccion = esReverso(c)
    const enlazada = original !== undefined
    return {
      clave: `c-${i}`,
      numero: numeroCargo(i),
      clase: esCorreccion ? 'correccion' : 'cargo',
      fecha: c.fecha,
      tipo: esCorreccion ? 'Corrección' : NOMBRE_TIPO_CARGO[c.tipo],
      concepto: enlazada ? folio.cargos[original].concepto : c.concepto,
      valor: c.valor,
      indiceCargo: i,
      corrige: enlazada ? numeroCargo(original) : undefined,
      corregidoPor: correccion !== undefined ? numeroCargo(correccion) : undefined,
      motivo: enlazada ? c.concepto.slice(`${PREFIJO_REVERSO}${folio.cargos[original].concepto}': `.length) : undefined,
    }
  })

  const pagos: Movimiento[] = folio.pagos.map((p, i) => ({
    clave: `p-${i}`,
    numero: `P${i + 1}`,
    clase: 'pago',
    fecha: p.fecha,
    tipo: 'Pago',
    concepto: NOMBRE_MEDIO[p.medio],
    valor: -p.valor,
  }))

  // sort es estable: dentro del mismo día se conserva el orden de asiento, primero cargos y luego pagos
  return [...cargos, ...pagos].sort((a, b) => a.fecha.localeCompare(b.fecha))
}

/** Cargos que se pueden corregir: ni son correcciones ni ya fueron corregidos */
export function cargosCorregibles(movimientos: Movimiento[]): Movimiento[] {
  return movimientos
    .filter((m) => m.clase === 'cargo' && !m.corregidoPor)
    .sort((a, b) => (a.indiceCargo ?? 0) - (b.indiceCargo ?? 0))
}
