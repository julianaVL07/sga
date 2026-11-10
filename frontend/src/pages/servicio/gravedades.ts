import type { GravedadNovedad } from '@/api/apartamentos'

/** Gravedades de una novedad, con sus colores */
export const GRAVEDADES: { valor: GravedadNovedad; etiqueta: string; fondo: string; texto: string; borde: string }[] = [
  { valor: 'BAJA', etiqueta: 'Baja', fondo: '#DDF2EA', texto: '#1C5E45', borde: '#7FC3A6' },
  { valor: 'MEDIA', etiqueta: 'Media', fondo: '#FFE4CF', texto: '#7A3E0C', borde: '#D9792F' },
  { valor: 'ALTA', etiqueta: 'Alta', fondo: '#F5E3E3', texto: '#7D2E2E', borde: '#B3261E' },
]
