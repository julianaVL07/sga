import {
  BedDouble,
  Building2,
  CalendarDays,
  CalendarRange,
  ChartBar,
  ClipboardList,
  ConciergeBell,
  FilePlus2,
  ListTodo,
  ScrollText,
  UserRound,
  Users,
  type LucideIcon,
} from 'lucide-react'
import type { Rol } from '@/api/tipos'

export interface OpcionMenu {
  etiqueta: string
  ruta: string
  icono: LucideIcon
  /** Solo se marca activa si la ruta coincide exacta (para las rutas de inicio) */
  exacta?: boolean
}

export interface GrupoMenu {
  titulo?: string
  opciones: OpcionMenu[]
}

// ---------- Panel (menú lateral) ----------

const OPCIONES_RECEPCION: OpcionMenu[] = [
  { etiqueta: 'Recepción', ruta: '/panel', icono: ConciergeBell, exacta: true },
  { etiqueta: 'Reservas', ruta: '/panel/reservas', icono: ClipboardList },
  { etiqueta: 'Calendario', ruta: '/panel/calendario', icono: CalendarDays },
  { etiqueta: 'Apartamentos', ruta: '/panel/apartamentos', icono: BedDouble },
]

const OPCIONES_ADMINISTRACION: OpcionMenu[] = [
  { etiqueta: 'Temporadas y tarifas', ruta: '/panel/temporadas', icono: CalendarRange },
  { etiqueta: 'Política', ruta: '/panel/politica', icono: ScrollText },
  { etiqueta: 'Alojamiento', ruta: '/panel/alojamiento', icono: Building2 },
  { etiqueta: 'Usuarios', ruta: '/panel/usuarios', icono: Users },
  { etiqueta: 'Reportes', ruta: '/panel/reportes', icono: ChartBar },
]

export const OPCION_PERFIL_PANEL: OpcionMenu = { etiqueta: 'Mi perfil', ruta: '/panel/perfil', icono: UserRound }

export function menuPanel(roles: Rol[]): GrupoMenu[] {
  const grupos: GrupoMenu[] = [{ opciones: OPCIONES_RECEPCION }]
  if (roles.includes('ADMINISTRADOR')) {
    grupos.push({ titulo: 'Administración', opciones: OPCIONES_ADMINISTRACION })
  }
  return grupos
}

/** Título de la barra superior del panel según la ruta */
export function tituloPanel(pathname: string): string {
  if (pathname === '/panel') return 'Recepción'
  if (pathname === '/panel/reservas/nueva') return 'Nueva reserva'
  if (/^\/panel\/reservas\/[^/]+\/folio$/.test(pathname)) return 'Folio'
  if (/^\/panel\/reservas\/[^/]+$/.test(pathname)) return 'Detalle de la reserva'
  if (pathname === '/panel/apartamentos/nuevo') return 'Nuevo apartamento'
  if (/^\/panel\/apartamentos\/[^/]+\/editar$/.test(pathname)) return 'Editar apartamento'
  if (/^\/panel\/apartamentos\/[^/]+\/novedades$/.test(pathname)) return 'Novedades del apartamento'
  if (/^\/panel\/apartamentos\/[^/]+$/.test(pathname)) return 'Detalle del apartamento'
  if (pathname === '/panel/perfil') return 'Mi perfil'
  const todas = [...OPCIONES_RECEPCION, ...OPCIONES_ADMINISTRACION]
  return todas.find((o) => o.ruta !== '/panel' && pathname.startsWith(o.ruta))?.etiqueta ?? 'Panel'
}

// ---------- Servicio (barra inferior) ----------

export const MENU_SERVICIO: OpcionMenu[] = [
  { etiqueta: 'Pendientes', ruta: '/servicio', icono: ListTodo, exacta: true },
  { etiqueta: 'Nueva novedad', ruta: '/servicio/novedad', icono: FilePlus2 },
  { etiqueta: 'Mi perfil', ruta: '/servicio/perfil', icono: UserRound },
]

/** Título del encabezado de servicio según la ruta */
export function tituloServicio(pathname: string): string {
  if (pathname === '/servicio') return 'Pendientes'
  if (pathname === '/servicio/novedad') return 'Nueva novedad'
  if (pathname === '/servicio/perfil') return 'Mi perfil'
  if (pathname.startsWith('/servicio/apartamentos/')) return 'Historial de novedades'
  return 'Servicio'
}
