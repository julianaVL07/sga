import { Outlet } from 'react-router-dom'
import { AuthProvider } from '@/auth/AuthProvider'

/** La sesión vive dentro del router para poder navegar ante un 401 o un 403 */
export function RaizApp() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}
