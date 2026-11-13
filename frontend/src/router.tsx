import { lazy, Suspense, type ComponentType } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { RutaProtegida } from '@/auth/RutaProtegida'
import { ROLES_ADMIN, ROLES_HUESPED, ROLES_PANEL, ROLES_SERVICIO } from '@/auth/roles'
import { CargandoPagina } from '@/components/coral/CargandoPagina'
import { PanelLayout } from '@/layouts/PanelLayout'
import { RaizApp } from '@/layouts/RaizApp'
import { ServicioLayout } from '@/layouts/ServicioLayout'
import { SitioLayout } from '@/layouts/SitioLayout'

/**
 * Cada página se descarga con React.lazy la primera vez que se visita, y Suspense muestra el indicador mientras
 * llega. Así el paquete inicial solo trae los layouts, y Leaflet viaja con las páginas que tienen el mapa
 * (búsqueda y detalle del apartamento).
 */
function pagina(cargar: () => Promise<{ default: ComponentType }>) {
  const Pagina = lazy(cargar)
  return (
    <Suspense fallback={<CargandoPagina />}>
      <Pagina />
    </Suspense>
  )
}

const miPerfil = () => import('@/pages/perfil/MiPerfil')
const noEncontrada = () => import('@/pages/errores/NoEncontrada')
const formularioApartamento = () => import('@/pages/panel/apartamentos/FormularioApartamento')

export const router = createBrowserRouter([
  {
    element: <RaizApp />,
    children: [
      // ---------- Sitio: visitante y huésped ----------
      {
        element: <SitioLayout />,
        children: [
          { path: '/', element: pagina(() => import('@/pages/publico/Inicio')) },
          { path: '/buscar', element: pagina(() => import('@/pages/publico/Buscar')) },
          { path: '/apartamentos/:id', element: pagina(() => import('@/pages/publico/DetalleApartamento')) },
          { path: '/login', element: pagina(() => import('@/pages/auth/Login')) },
          { path: '/registro', element: pagina(() => import('@/pages/auth/Registro')) },
          { path: '/recuperar', element: pagina(() => import('@/pages/auth/RecuperarContrasena')) },
          { path: '/recuperar/codigo', element: pagina(() => import('@/pages/auth/RecuperarCodigo')) },
          {
            element: <RutaProtegida roles={ROLES_HUESPED} />,
            children: [
              { path: '/reservar/:id', element: pagina(() => import('@/pages/huesped/Reservar')) },
              { path: '/mis-reservas', element: pagina(() => import('@/pages/huesped/MisReservas')) },
              { path: '/mis-reservas/:codigo', element: pagina(() => import('@/pages/huesped/DetalleMiReserva')) },
              { path: '/perfil', element: pagina(miPerfil) },
            ],
          },
          { path: '/sin-permiso', element: pagina(() => import('@/pages/errores/SinPermiso')) },
          { path: '*', element: pagina(noEncontrada) },
        ],
      },

      // ---------- Panel: recepción y administración ----------
      {
        path: '/panel',
        element: (
          <RutaProtegida roles={ROLES_PANEL}>
            <PanelLayout />
          </RutaProtegida>
        ),
        children: [
          { index: true, element: pagina(() => import('@/pages/panel/recepcion/InicioRecepcion')) },
          { path: 'calendario', element: pagina(() => import('@/pages/panel/calendario/Calendario')) },
          { path: 'reservas', element: pagina(() => import('@/pages/panel/reservas/ListadoReservas')) },
          { path: 'reservas/nueva', element: pagina(() => import('@/pages/panel/reservas/NuevaReserva')) },
          { path: 'reservas/:codigo', element: pagina(() => import('@/pages/panel/reservas/DetalleReserva')) },
          { path: 'reservas/:codigo/folio', element: pagina(() => import('@/pages/panel/folio/Folio')) },
          { path: 'apartamentos', element: pagina(() => import('@/pages/panel/apartamentos/ListadoApartamentos')) },
          { path: 'apartamentos/:id', element: pagina(() => import('@/pages/panel/apartamentos/DetalleApartamento')) },
          { path: 'perfil', element: pagina(miPerfil) },
          {
            element: <RutaProtegida roles={ROLES_ADMIN} />,
            children: [
              { path: 'apartamentos/nuevo', element: pagina(formularioApartamento) },
              { path: 'apartamentos/:id/editar', element: pagina(formularioApartamento) },
              {
                path: 'apartamentos/:id/novedades',
                element: pagina(() => import('@/pages/panel/apartamentos/NovedadesApartamento')),
              },
              { path: 'temporadas', element: pagina(() => import('@/pages/panel/configuracion/Temporadas')) },
              { path: 'politica', element: pagina(() => import('@/pages/panel/configuracion/Politica')) },
              { path: 'alojamiento', element: pagina(() => import('@/pages/panel/configuracion/Alojamiento')) },
              { path: 'usuarios', element: pagina(() => import('@/pages/panel/usuarios/Usuarios')) },
              { path: 'canales', element: pagina(() => import('@/pages/panel/canales/Canales')) },
              { path: 'reportes', element: pagina(() => import('@/pages/panel/reportes/Reportes')) },
            ],
          },
          { path: '*', element: pagina(noEncontrada) },
        ],
      },

      // ---------- Servicio: personal de servicio y administración ----------
      {
        path: '/servicio',
        element: (
          <RutaProtegida roles={ROLES_SERVICIO}>
            <ServicioLayout />
          </RutaProtegida>
        ),
        children: [
          { index: true, element: pagina(() => import('@/pages/servicio/Pendientes')) },
          { path: 'novedad', element: pagina(() => import('@/pages/servicio/NuevaNovedad')) },
          { path: 'apartamentos/:id', element: pagina(() => import('@/pages/servicio/HistorialNovedades')) },
          { path: 'perfil', element: pagina(miPerfil) },
          { path: '*', element: pagina(noEncontrada) },
        ],
      },
    ],
  },
])
