import { useState, type FormEvent } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { LogOut, Menu, Search } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { NOMBRE_ROL } from '@/auth/roles'
import { BotonTema, Logo } from '@/components/coral'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { cn } from '@/lib/utils'
import { AvatarUsuario } from './AvatarUsuario'
import { OPCION_PERFIL_PANEL, menuPanel, tituloPanel, type OpcionMenu } from './menus'

function EnlaceLateral({ opcion, alNavegar }: { opcion: OpcionMenu; alNavegar?: () => void }) {
  const Icono = opcion.icono
  return (
    <NavLink
      to={opcion.ruta}
      end={opcion.exacta}
      onClick={alNavegar}
      className={({ isActive }) =>
        cn(
          'relative flex min-h-11 items-center gap-3 rounded-r-full py-2 pr-4 pl-5 text-sm font-medium transition-colors duration-150',
          'before:absolute before:inset-y-1.5 before:left-0 before:w-1 before:rounded-full',
          isActive
            ? 'bg-sidebar-accent text-white before:bg-coral'
            : 'text-ciruela-texto/80 before:bg-transparent hover:bg-white/5 hover:text-white',
        )
      }
    >
      <Icono aria-hidden="true" className="size-5 shrink-0" />
      {opcion.etiqueta}
    </NavLink>
  )
}

function MenuLateral({ alNavegar }: { alNavegar?: () => void }) {
  const { roles, usuario, logout } = useAuth()
  const grupos = menuPanel(roles)

  return (
    <div className="flex h-full flex-col bg-ciruela text-ciruela-texto">
      <div className="px-5 py-6">
        <Link to="/panel" onClick={alNavegar} aria-label="Coral, ir a Recepción" className="rounded-full">
          <Logo claro conSubtitulo tamano="sm" />
        </Link>
      </div>

      <nav aria-label="Panel" className="flex-1 overflow-y-auto pr-3">
        {grupos.map((grupo, i) => (
          <div key={grupo.titulo ?? i} className={cn(i > 0 && 'mt-6')}>
            {grupo.titulo && (
              <p className="mb-2 px-5 text-[0.7rem] font-semibold tracking-[0.18em] text-ciruela-texto/60 uppercase">
                {grupo.titulo}
              </p>
            )}
            <ul className="flex flex-col gap-1">
              {grupo.opciones.map((opcion) => (
                <li key={opcion.ruta}>
                  <EnlaceLateral opcion={opcion} alNavegar={alNavegar} />
                </li>
              ))}
            </ul>
          </div>
        ))}
      </nav>

      <div className="border-t border-sidebar-border py-3 pr-3">
        <div className="flex items-center gap-3 px-5 py-2">
          <AvatarUsuario className="size-8" />
          <div className="min-w-0 text-xs">
            <p className="truncate font-semibold text-white">{usuario?.nombre ?? ' '}</p>
            <p className="truncate text-ciruela-texto/70">{roles.map((r) => NOMBRE_ROL[r]).join(', ')}</p>
          </div>
        </div>
        <EnlaceLateral opcion={OPCION_PERFIL_PANEL} alNavegar={alNavegar} />
        <button
          type="button"
          onClick={() => {
            alNavegar?.()
            logout()
          }}
          className="flex min-h-11 w-full items-center gap-3 rounded-r-full py-2 pr-4 pl-5 text-left text-sm font-medium text-ciruela-texto/80 transition-colors duration-150 hover:bg-white/5 hover:text-white"
        >
          <LogOut aria-hidden="true" className="size-5 shrink-0" />
          Cerrar sesión
        </button>
      </div>
    </div>
  )
}

/** Panel de recepción y administración: menú lateral y barra superior */
export function PanelLayout() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [busqueda, setBusqueda] = useState('')
  const titulo = tituloPanel(pathname)

  const buscar = (evento: FormEvent) => {
    evento.preventDefault()
    const texto = busqueda.trim()
    if (texto) navigate(`/panel/reservas?buscar=${encodeURIComponent(texto)}`)
  }

  return (
    <div className="flex min-h-dvh bg-background">
      <a
        href="#contenido"
        className="sr-only z-50 rounded-full bg-primary px-4 py-2 text-primary-foreground focus:not-sr-only focus:fixed focus:top-2 focus:left-2"
      >
        Saltar al contenido
      </a>

      <aside className="sticky top-0 hidden h-dvh w-64 shrink-0 lg:block">
        <MenuLateral />
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-card px-4 md:px-6">
          <Sheet open={menuAbierto} onOpenChange={setMenuAbierto}>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon" aria-label="Abrir menú" className="lg:hidden">
                <Menu aria-hidden="true" />
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="w-72 border-0 p-0" showCloseButton={false}>
              <SheetTitle className="sr-only">Menú del panel</SheetTitle>
              <SheetDescription className="sr-only">Opciones del panel de Coral</SheetDescription>
              <MenuLateral alNavegar={() => setMenuAbierto(false)} />
            </SheetContent>
          </Sheet>

          <p className="min-w-0 flex-1 truncate font-titulo text-xl font-semibold md:text-2xl">{titulo}</p>

          <form role="search" onSubmit={buscar} className="relative hidden w-72 md:block">
            <label htmlFor="buscador-panel" className="sr-only">
              Buscar reservas por código o titular
            </label>
            <Search
              aria-hidden="true"
              className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground"
            />
            <Input
              id="buscador-panel"
              type="search"
              placeholder="Buscar reservas…"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              className="pl-10"
            />
          </form>

          <BotonTema />
        </header>

        <main id="contenido" className="flex-1 px-4 py-6 md:px-8 md:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
