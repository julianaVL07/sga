import type { EstadoConflictoCanal, ResultadoEventoCanal } from '@/api/canales'
import { ESTADO_CONFLICTO, RESULTADO_EVENTO, TONO_RESULTADO, type Tono } from './textos'

const COLORES: Record<Tono, { backgroundColor: string; color: string }> = {
  exito: { backgroundColor: '#DDF2EA', color: '#1C5E45' },
  alerta: { backgroundColor: '#FFF1C2', color: '#6B4800' },
  peligro: { backgroundColor: '#FBE1DE', color: '#8A2A1F' },
  neutro: { backgroundColor: '#EFE7EC', color: '#57434F' },
}

/** Píldora de estado con punto, como la de usuarios activos e inactivos */
export function Etiqueta({ tono, texto }: { tono: Tono; texto: string }) {
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap"
      style={COLORES[tono]}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {texto}
    </span>
  )
}

export function EtiquetaActivo({ activo }: { activo: boolean }) {
  return <Etiqueta tono={activo ? 'exito' : 'neutro'} texto={activo ? 'Activo' : 'Inactivo'} />
}

export function EtiquetaEstadoConflicto({ estado }: { estado: EstadoConflictoCanal }) {
  return <Etiqueta tono={estado === 'PENDIENTE' ? 'alerta' : 'exito'} texto={ESTADO_CONFLICTO[estado]} />
}

export function EtiquetaResultado({ resultado }: { resultado: ResultadoEventoCanal }) {
  return <Etiqueta tono={TONO_RESULTADO[resultado]} texto={RESULTADO_EVENTO[resultado]} />
}
