const PORCENTAJE = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 })

/** 2.95 → "2,95 %" */
export function formatearPorcentaje(valor: number): string {
  return `${PORCENTAJE.format(valor)} %`
}
