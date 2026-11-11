import { addDays, differenceInCalendarDays, differenceInYears, format, getDay, isValid, parseISO } from 'date-fns'
import type {
  AlojamientoResponse,
  FechaISO,
  PoliticaCancelacionResponse,
  TemporadaResponse,
  TipoTemporada,
} from '@/api/tipos'

/*
 * Reglas del alojamiento que el frontend muestra antes de llamar al backend.
 * Todos los valores salen de GET /api/alojamiento; aquí no hay valores del negocio escritos a mano.
 * Un null significa que el dato no vino (o aún no carga): el aviso se muestra sin el dato.
 * El backend siempre tiene la última palabra (409).
 */

export interface ReglasAlojamiento {
  /** RN-06: los menores de esta edad no pagan noche */
  umbralEdad: number | null
  porcentajeAnticipo: number | null
  plazoConfirmacionHoras: number | null
  /** RP-03: desde esta hora la llegada es nocturna ("HH:mm") */
  horaNocturna: string | null
  /** L-15: desde esta hora del día de llegada se puede declarar el no-show ("HH:mm") */
  horaLimiteNoShow: string | null
  /** RP-03: valor de la recepción nocturna */
  recargoNocturno: number | null
  estanciaMinima: number | null
  estanciaMinimaActiva: boolean
  aceptaMascotas: boolean | null
  maximoMascotas: number | null
  pesoMaximoMascotaKg: number | null
  /** L-16: cargo único de aseo por mascotas */
  cargoAseoMascota: number | null
  /** RP-02: depósito de garantía reembolsable */
  depositoGarantia: number | null
  horaEntrada: string | null
  horaSalida: string | null
}

const hhmm = (hora: string | null | undefined) => (hora ? hora.slice(0, 5) : null)

/** Lee los parámetros del alojamiento tal como llegan */
export function reglasDe(alojamiento: AlojamientoResponse | undefined): ReglasAlojamiento {
  const p = alojamiento?.parametros
  const nocturna = alojamiento?.serviciosAdicionales.find((s) => /nocturn/i.test(s.nombre))
  return {
    umbralEdad: p?.umbralEdadFacturable ?? null,
    porcentajeAnticipo: p?.porcentajeAnticipo ?? null,
    plazoConfirmacionHoras: p?.plazoConfirmacionHoras ?? null,
    horaNocturna: hhmm(p?.horaInicioLlegadaNocturna),
    horaLimiteNoShow: hhmm(p?.horaLimiteNoShow),
    recargoNocturno: nocturna?.valor ?? null,
    estanciaMinima: p?.estanciaMinimaNoches ?? null,
    estanciaMinimaActiva: p?.estanciaMinimaActiva ?? false,
    aceptaMascotas: p?.aceptaMascotas ?? null,
    maximoMascotas: p?.maximoMascotas ?? null,
    pesoMaximoMascotaKg: p?.pesoMaximoMascotaKg ?? null,
    cargoAseoMascota: p?.cargoAseoMascota ?? null,
    depositoGarantia: p?.depositoGarantia ?? null,
    horaEntrada: hhmm(alojamiento?.horaEntrada),
    horaSalida: hhmm(alojamiento?.horaSalida),
  }
}

// ---------- Frases con o sin el dato ----------

/** "Entrada desde las 3:00 p. m." o, sin hora, solo "Entrada" */
export function conHora(texto: string, hora: string | null, enlace = 'las'): string {
  return hora ? `${texto} ${enlace ? `${enlace} ` : ''}${formatearHora(hora)}` : texto
}

/** Cierra la frase con punto, sin duplicarlo cuando termina en "a. m." o "p. m." */
export function conPunto(texto: string): string {
  return texto.endsWith('.') ? texto : `${texto}.`
}

/** "Los menores de 6 años no pagan noche" o, sin la edad, sin el dato */
export function textoMenores(umbral: number | null): string {
  return umbral !== null
    ? `Los menores de ${umbral} años no pagan noche`
    : 'Los menores de la edad que fija el alojamiento no pagan noche'
}

/** "Hasta 2 mascotas de máximo 10 kg", con las partes que se conozcan */
export function textoMascotas(reglas: ReglasAlojamiento): string {
  const cantidad = reglas.maximoMascotas !== null ? `Hasta ${reglas.maximoMascotas} mascotas` : 'Mascotas pequeñas'
  const peso = reglas.pesoMaximoMascotaKg !== null ? ` de máximo ${reglas.pesoMaximoMascotaKg} kg` : ''
  return cantidad + peso
}

/** "el 50 %" o, sin porcentaje, "el anticipo" */
export function textoPorcentajeAnticipo(porcentaje: number | null): string {
  return porcentaje !== null ? `el ${porcentaje} %` : 'el anticipo'
}

/** "dentro de las 12 horas siguientes a la reserva" o, sin plazo, "dentro del plazo de confirmación" */
export function textoPlazo(horas: number | null): string {
  return horas !== null ? `dentro de las ${horas} horas siguientes a la reserva` : 'dentro del plazo de confirmación'
}

/** Anticipo en pesos; null si el porcentaje no vino */
export function calcularAnticipo(valorEstancia: number, porcentaje: number | null): number | null {
  return porcentaje !== null ? Math.round((valorEstancia * porcentaje) / 100) : null
}

// ---------- Fechas y horas ----------

export function hoyISO(): FechaISO {
  return format(new Date(), 'yyyy-MM-dd')
}

export function sumarDias(fecha: FechaISO, dias: number): FechaISO {
  return format(addDays(parseISO(fecha), dias), 'yyyy-MM-dd')
}

export function esFechaValida(fecha: string | undefined | null): fecha is FechaISO {
  return !!fecha && /^\d{4}-\d{2}-\d{2}$/.test(fecha) && isValid(parseISO(fecha))
}

/** Noches entre dos fechas; 0 si no forman una estancia */
export function nochesEntre(entrada: string | undefined, salida: string | undefined): number {
  if (!esFechaValida(entrada) || !esFechaValida(salida)) return 0
  return Math.max(0, differenceInCalendarDays(parseISO(salida), parseISO(entrada)))
}

/** Espacio no separable: "3:00 p. m." nunca se parte entre líneas */
const NBSP = ' '

/** "15:00:00" → "3:00 p. m." */
export function formatearHora(hora: string): string {
  const [h, m] = hora.split(':').map(Number)
  if (Number.isNaN(h)) return hora
  const sufijo = h >= 12 ? `p.${NBSP}m.` : `a.${NBSP}m.`
  const h12 = h % 12 === 0 ? 12 : h % 12
  return `${h12}:${String(m ?? 0).padStart(2, '0')}${NBSP}${sufijo}`
}

/** Compara "HH:mm": true si la hora es igual o posterior al límite; sin límite conocido, false */
export function horaDesde(hora: string, limite: string | null): boolean {
  return !!hora && !!limite && hora.slice(0, 5) >= limite.slice(0, 5)
}

// ---------- Huéspedes ----------

/** Edad cumplida en una fecha (por defecto, hoy) */
export function edadEn(fechaNacimiento: string, referencia?: string): number | null {
  if (!esFechaValida(fechaNacimiento)) return null
  const ref = esFechaValida(referencia) ? parseISO(referencia) : new Date()
  return differenceInYears(ref, parseISO(fechaNacimiento))
}

/** RN-06: los menores del umbral de edad no pagan noche; sin umbral conocido no se marca a nadie */
export function esMenorNoPaga(fechaNacimiento: string, umbral: number | null, referencia?: string): boolean {
  if (umbral === null) return false
  const edad = edadEn(fechaNacimiento, referencia)
  return edad !== null && edad < umbral
}

// ---------- Temporadas ----------

/** Temporada de una noche: la primera no base cuyo rango la incluye; si ninguna, la base */
export function temporadaDe(fecha: FechaISO, temporadas: TemporadaResponse[]): TemporadaResponse | undefined {
  const especial = temporadas.find(
    (t) => t.tipo !== 'BASE' && t.rangos.some((r) => fecha >= r.fechaInicio && fecha <= r.fechaFin),
  )
  return especial ?? temporadas.find((t) => t.tipo === 'BASE')
}

/** Tipo de temporada a partir del texto que trae la cotización (id, nombre o tipo) */
export function tipoDeTemporada(texto: string, temporadas: TemporadaResponse[] = []): TipoTemporada | null {
  const t = temporadas.find((x) => x.id === texto || x.nombre.toLowerCase() === texto.toLowerCase())
  if (t) return t.tipo
  const mayus = texto.toUpperCase()
  if (mayus.includes('ALTA')) return 'ALTA'
  if (mayus.includes('MEDIA')) return 'MEDIA'
  if (mayus.includes('BASE') || mayus.includes('BAJA')) return 'BASE'
  return null
}

/** Nombre de la temporada para mostrar (la cotización puede traer el id) */
export function nombreDeTemporada(texto: string, temporadas: TemporadaResponse[] = []): string {
  return temporadas.find((x) => x.id === texto)?.nombre ?? texto
}

/** Días de la semana de date-fns (0 = domingo) que nombra la regla RP-01 */
const VIERNES = 5
const SABADO = 6

/**
 * RP-01: si la estancia toca un viernes o un sábado en temporada media o alta, exige la estancia mínima.
 * Devuelve el mensaje del aviso, o null si cumple.
 */
export function avisoEstanciaMinima(
  entrada: string | undefined,
  salida: string | undefined,
  temporadas: TemporadaResponse[],
  reglas: ReglasAlojamiento,
): string | null {
  // Sin la estancia mínima configurada no se avisa: el backend decide
  if (!reglas.estanciaMinimaActiva || reglas.estanciaMinima === null || temporadas.length === 0) return null
  const noches = nochesEntre(entrada, salida)
  if (noches === 0 || noches >= reglas.estanciaMinima) return null
  for (let i = 0; i < noches; i++) {
    const noche = sumarDias(entrada as string, i)
    const dia = getDay(parseISO(noche))
    const tipo = temporadaDe(noche, temporadas)?.tipo
    if ((dia === VIERNES || dia === SABADO) && (tipo === 'MEDIA' || tipo === 'ALTA')) {
      return `Las estancias que incluyen un viernes o un sábado en temporada media o alta deben ser de mínimo ${reglas.estanciaMinima} noches (RP-01). Amplía la fecha de salida.`
    }
  }
  return null
}

// ---------- Cancelación ----------

export interface RetencionEstimada {
  dias: number
  porcentaje: number
  valor: number
}

/** RN-13: tramo con la mayor antelación que no supere los días que faltan */
export function retencionEstimada(
  politica: PoliticaCancelacionResponse | undefined,
  valorTotal: number,
  fechaEntrada: FechaISO,
): RetencionEstimada | null {
  if (!politica || politica.tramos.length === 0) return null
  const dias = Math.max(0, differenceInCalendarDays(parseISO(fechaEntrada), new Date()))
  const tramo = [...politica.tramos]
    .sort((a, b) => b.antelacionMinimaDias - a.antelacionMinimaDias)
    .find((t) => dias >= t.antelacionMinimaDias)
  // Ningún tramo cubre esos días: no se estima y el backend calcula la retención real
  if (!tramo) return null
  const porcentaje = tramo.porcentajeRetencion
  return { dias, porcentaje, valor: Math.round((valorTotal * porcentaje) / 100) }
}

/** Tramos de la política, del de más antelación al de menos */
export function tramosOrdenados(politica: PoliticaCancelacionResponse | undefined) {
  return [...(politica?.tramos ?? [])].sort((a, b) => b.antelacionMinimaDias - a.antelacionMinimaDias)
}
