import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { BotonTema, Logo, Onda } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { MENU_SERVICIO, tituloServicio } from './menus'

/** Personal de servicio: pensado para el celular, con encabezado ciruela y navegación inferior */
export function ServicioLayout() {
  const { pathname } = useLocation()
  const { usuario, logout } = useAuth()

  return (
    <div className="flex min-h-dvh flex-col bg-background">
      <header className="relative bg-ciruela text-ciruela-texto">
        <div className="mx-auto flex max-w-2xl items-center justify-between gap-2 px-4 pt-4">
          <Logo claro tamano="sm" />
          <div className="flex items-center">
            <BotonTema className="text-ciruela-texto hover:bg-white/10 hover:text-white" />
            <Button
              variant="ghost"
              size="icon"
              aria-label="Cerrar sesión"
              onClick={logout}
              className="text-ciruela-texto hover:bg-white/10 hover:text-white"
            >
              <LogOut aria-hidden="true" />
            </Button>
          </div>
        </div>
        <div className="mx-auto max-w-2xl px-4 pt-4 pb-10">
          {usuario && <p className="text-sm text-ciruela-texto/80">Hola, {usuario.nombre.split(' ')[0]}</p>}
          <p className="font-titulo text-3xl font-semibold text-white">{tituloServicio(pathname)}</p>
        </div>
        <Onda className="absolute inset-x-0 -bottom-px h-8 [&_path:last-child]:fill-[var(--coral-fondo)] [&_path:last-child]:[fill-opacity:1]" />
      </header>

      <main id="contenido" className="mx-auto w-full max-w-2xl flex-1 px-4 pt-6 pb-28">
        <Outlet />
      </main>

      <nav
        aria-label="Servicio"
        className="fixed inset-x-0 bottom-0 z-30 border-t border-border bg-card pb-[env(safe-area-inset-bottom)]"
      >
        <ul className="mx-auto grid max-w-2xl grid-cols-3">
          {MENU_SERVICIO.map((opcion) => {
            const Icono = opcion.icono
            return (
              <li key={opcion.ruta}>
                <NavLink
                  to={opcion.ruta}
                  end={opcion.exacta}
                  className={({ isActive }) =>
                    cn(
                      'flex min-h-16 flex-col items-center justify-center gap-1 text-xs font-medium transition-colors duration-150',
                      isActive ? 'text-primary' : 'text-muted-foreground hover:text-foreground',
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      <span
                        className={cn(
                          'flex h-8 w-14 items-center justify-center rounded-full transition-colors duration-150',
                          isActive && 'bg-coral/15',
                        )}
                      >
                        <Icono aria-hidden="true" className={cn('size-5', isActive && 'text-coral')} />
                      </span>
                      {opcion.etiqueta}
                    </>
                  )}
                </NavLink>
              </li>
            )
          })}
        </ul>
      </nav>
    </div>
  )
}
