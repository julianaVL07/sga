import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { esErrorApi } from '@/api/cliente'
import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'
import { TemaProvider } from '@/lib/tema'
import { router } from './router'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,
      refetchOnWindowFocus: false,
      // Reintenta solo fallas de red o del servidor, no los 4xx
      retry: (intentos, error) => intentos < 2 && !(esErrorApi(error) && error.status >= 400 && error.status < 500),
    },
  },
})

export default function App() {
  return (
    <TemaProvider>
      <QueryClientProvider client={queryClient}>
        <TooltipProvider>
          <RouterProvider router={router} />
          <Toaster position="top-center" richColors closeButton />
        </TooltipProvider>
      </QueryClientProvider>
    </TemaProvider>
  )
}
