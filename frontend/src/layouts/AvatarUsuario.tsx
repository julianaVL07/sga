import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { useAuth } from '@/auth/AuthContext'
import { cn } from '@/lib/utils'

function iniciales(nombre: string | undefined): string {
  if (!nombre) return '·'
  const partes = nombre.trim().split(/\s+/)
  return ((partes[0]?.[0] ?? '') + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase()
}

interface Props {
  className?: string
}

/** Foto o iniciales del usuario que inició sesión */
export function AvatarUsuario({ className }: Props) {
  const { usuario } = useAuth()
  return (
    <Avatar className={cn('size-9', className)}>
      {usuario?.fotoUrl && <AvatarImage src={usuario.fotoUrl} alt="" />}
      <AvatarFallback className="bg-primary text-sm font-semibold text-primary-foreground">{iniciales(usuario?.nombre)}</AvatarFallback>
    </Avatar>
  )
}
