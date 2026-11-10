import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { LayoutDashboard, LogOut, MapPin, Menu, UserRound } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { rutaInicioPorRol } from '@/auth/roles'
import { BotonTema, Logo, Onda } from '@/components/coral'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { cn } from '@/lib/utils'
import { AvatarUsuario } from './AvatarUsuario'

const ANIO_ACTUAL = new Date().getFullYear()

const claseEnlace =({ isActive }: { isActive: boolean }) =>
  cn(
    'inline-flex min-h-11 items-center rounded-full px-4 text-sm font-medium transition-colors duration-150 hover:bg-muted',
    isActive ? 'text-primary' : 'text-foreground',
  )

/** Sitio público y del huésped: menú arriba y pie de página */
export function SitioLayout() {
  const { autenticado, roles, usuario, logout } = useAuth()
  const [menuAbierto, setMenuAbierto] = useState(false)
  const esHuesped = roles.includes('HUESPED')
  const esInterno = autenticado && !esHuesped

  const enlaces = (
    <>
      <NavLink to="/buscar" className={claseEnlace} onClick={() => setMenuAbierto(false)}>
        Apartamentos
      </NavLink>
      {esHuesped && (
        <NavLink to="/mis-reservas" className={claseEnlace} onClick={() => setMenuAbierto(false)}>
          Mis reservas
        </NavLink>
      )}
      {esInterno && (
        <NavLink to={rutaInicioPorRol(roles)} className={claseEnlace} onClick={() => setMenuAbierto(false)}>
          Ir a mi espacio de trabajo
        </NavLink>
      )}
    </>
  )

  return (
    <div className="flex min-h-dvh flex-col">
      <a
        href="#contenido"
        className="sr-only z-50 rounded-full bg-primary px-4 py-2 text-primary-foreground focus:not-sr-only focus:fixed focus:top-2 focus:left-2"
      >
        Saltar al contenido
      </a>

      <header className="sticky top-0 z-40 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4">
          <Link to="/" aria-label="Coral, ir al inicio" className="rounded-full">
            <Logo tamano="sm" />
          </Link>

          <nav aria-label="Principal" className="hidden items-center gap-1 md:flex">
            {enlaces}
          </nav>

          <div className="flex items-center gap-1">
            <BotonTema />

            {autenticado ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon" aria-label="Abrir el menú de mi cuenta" className="p-0">
                    <AvatarUsuario />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-56">
                  <DropdownMenuLabel className="truncate">{usuario?.nombre ?? 'Mi cuenta'}</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  {esHuesped ? (
                    <DropdownMenuItem asChild>
                      <Link to="/perfil">
                        <UserRound aria-hidden="true" /> Mi perfil
                      </Link>
                    </DropdownMenuItem>
                  ) : (
                    <DropdownMenuItem asChild>
                      <Link to={rutaInicioPorRol(roles)}>
                        <LayoutDashboard aria-hidden="true" /> Mi espacio de trabajo
                      </Link>
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuItem onSelect={logout}>
                    <LogOut aria-hidden="true" /> Cerrar sesión
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <div className="hidden items-center gap-2 sm:flex">
                <Button variant="ghost" asChild>
                  <Link to="/login">Iniciar sesión</Link>
                </Button>
                <Button asChild>
                  <Link to="/registro">Crear cuenta</Link>
                </Button>
              </div>
            )}

            <Sheet open={menuAbierto} onOpenChange={setMenuAbierto}>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Abrir menú" className="md:hidden">
                  <Menu aria-hidden="true" />
                </Button>
              </SheetTrigger>
              <SheetContent side="right" className="w-72 p-0">
                <SheetHeader className="border-b border-border p-4">
                  <SheetTitle>
                    <Logo tamano="sm" />
                  </SheetTitle>
                </SheetHeader>
                <nav aria-label="Principal" className="flex flex-col gap-1 px-3">
                  {enlaces}
                </nav>
                {!autenticado && (
                  <div className="mt-auto flex flex-col gap-2 border-t border-border p-4">
                    <Button variant="outline" asChild>
                      <Link to="/login" onClick={() => setMenuAbierto(false)}>
                        Iniciar sesión
                      </Link>
                    </Button>
                    <Button asChild>
                      <Link to="/registro" onClick={() => setMenuAbierto(false)}>
                        Crear cuenta
                      </Link>
                    </Button>
                  </div>
                )}
              </SheetContent>
            </Sheet>
          </div>
        </div>
      </header>

      <main id="contenido" className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>

      <footer className="relative mt-20 bg-ciruela-oscuro text-ciruela-texto">
        <Onda className="absolute inset-x-0 bottom-full h-12" />
        <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 pt-10 pb-10 md:flex-row md:items-end md:justify-between">
          <Logo claro conSubtitulo />
          <address className="flex items-start gap-2 text-sm not-italic text-ciruela-texto/85">
            <MapPin aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-coral" />
            <span>
              Avenida del Malecón, Bocagrande
              <br />
              Cartagena, Colombia
            </span>
          </address>
        </div>
        <p className="border-t border-white/10 py-4 text-center text-xs text-ciruela-texto/70">
          © {ANIO_ACTUAL} Coral · Sistema de alojamientos
        </p>
      </footer>
    </div>
  )
}
