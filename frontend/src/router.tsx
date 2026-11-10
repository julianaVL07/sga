import { createBrowserRouter } from 'react-router-dom'
import { RutaProtegida } from '@/auth/RutaProtegida'
import { ROLES_ADMIN, ROLES_HUESPED, ROLES_PANEL, ROLES_SERVICIO } from '@/auth/roles'
import { PanelLayout } from '@/layouts/PanelLayout'
import { RaizApp } from '@/layouts/RaizApp'
import { ServicioLayout } from '@/layouts/ServicioLayout'
import { SitioLayout } from '@/layouts/SitioLayout'

import Login from '@/pages/auth/Login'
import Registro from '@/pages/auth/Registro'
import NoEncontrada from '@/pages/errores/NoEncontrada'
import SinPermiso from '@/pages/errores/SinPermiso'

import Buscar from '@/pages/publico/Buscar'
import DetalleApartamentoPublico from '@/pages/publico/DetalleApartamento'
import Inicio from '@/pages/publico/Inicio'

import DetalleMiReserva from '@/pages/huesped/DetalleMiReserva'
import MisReservas from '@/pages/huesped/MisReservas'
import Reservar from '@/pages/huesped/Reservar'
import MiPerfil from '@/pages/perfil/MiPerfil'

import DetalleApartamentoPanel from '@/pages/panel/apartamentos/DetalleApartamento'
import FormularioApartamento from '@/pages/panel/apartamentos/FormularioApartamento'
import ListadoApartamentos from '@/pages/panel/apartamentos/ListadoApartamentos'
import NovedadesApartamento from '@/pages/panel/apartamentos/NovedadesApartamento'
import Calendario from '@/pages/panel/calendario/Calendario'
import Alojamiento from '@/pages/panel/configuracion/Alojamiento'
import Politica from '@/pages/panel/configuracion/Politica'
import Temporadas from '@/pages/panel/configuracion/Temporadas'
import Folio from '@/pages/panel/folio/Folio'
import InicioRecepcion from '@/pages/panel/recepcion/InicioRecepcion'
import DetalleReserva from '@/pages/panel/reservas/DetalleReserva'
import ListadoReservas from '@/pages/panel/reservas/ListadoReservas'
import NuevaReserva from '@/pages/panel/reservas/NuevaReserva'
import Usuarios from '@/pages/panel/usuarios/Usuarios'

import HistorialNovedades from '@/pages/servicio/HistorialNovedades'
import NuevaNovedad from '@/pages/servicio/NuevaNovedad'
import Pendientes from '@/pages/servicio/Pendientes'

export const router = createBrowserRouter([
  {
    element: <RaizApp />,
    children: [
      // ---------- Sitio: visitante y huésped ----------
      {
        element: <SitioLayout />,
        children: [
          { path: '/', element: <Inicio /> },
          { path: '/buscar', element: <Buscar /> },
          { path: '/apartamentos/:id', element: <DetalleApartamentoPublico /> },
          { path: '/login', element: <Login /> },
          { path: '/registro', element: <Registro /> },
          {
            element: <RutaProtegida roles={ROLES_HUESPED} />,
            children: [
              { path: '/reservar/:id', element: <Reservar /> },
              { path: '/mis-reservas', element: <MisReservas /> },
              { path: '/mis-reservas/:codigo', element: <DetalleMiReserva /> },
              { path: '/perfil', element: <MiPerfil /> },
            ],
          },
          { path: '/sin-permiso', element: <SinPermiso /> },
          { path: '*', element: <NoEncontrada /> },
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
          { index: true, element: <InicioRecepcion /> },
          { path: 'calendario', element: <Calendario /> },
          { path: 'reservas', element: <ListadoReservas /> },
          { path: 'reservas/nueva', element: <NuevaReserva /> },
          { path: 'reservas/:codigo', element: <DetalleReserva /> },
          { path: 'reservas/:codigo/folio', element: <Folio /> },
          { path: 'apartamentos', element: <ListadoApartamentos /> },
          { path: 'apartamentos/:id', element: <DetalleApartamentoPanel /> },
          { path: 'perfil', element: <MiPerfil /> },
          {
            element: <RutaProtegida roles={ROLES_ADMIN} />,
            children: [
              { path: 'apartamentos/nuevo', element: <FormularioApartamento /> },
              { path: 'apartamentos/:id/editar', element: <FormularioApartamento /> },
              { path: 'apartamentos/:id/novedades', element: <NovedadesApartamento /> },
              { path: 'temporadas', element: <Temporadas /> },
              { path: 'politica', element: <Politica /> },
              { path: 'alojamiento', element: <Alojamiento /> },
              { path: 'usuarios', element: <Usuarios /> },
            ],
          },
          { path: '*', element: <NoEncontrada /> },
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
          { index: true, element: <Pendientes /> },
          { path: 'novedad', element: <NuevaNovedad /> },
          { path: 'apartamentos/:id', element: <HistorialNovedades /> },
          { path: 'perfil', element: <MiPerfil /> },
          { path: '*', element: <NoEncontrada /> },
        ],
      },
    ],
  },
])
