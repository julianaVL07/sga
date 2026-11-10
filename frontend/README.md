# Coral · Frontend

Interfaz web del sistema de alojamientos Coral. Está hecha con Vite, React, TypeScript, Tailwind CSS y shadcn/ui, y consume la API del backend Spring Boot que está en `../sga`.

## Requisitos

- Node.js 20.19 o superior (probado con Node 24) y npm 10 o superior.
- El backend corriendo en `http://localhost:8080`. Las instrucciones están en el README de la raíz del repositorio. El backend ya permite el origen `http://localhost:5173` (CORS).

## Instalación

```bash
cd frontend
npm install
```

## Variables de entorno

| Variable | Para qué sirve | Valor local |
|---|---|---|
| `VITE_API_URL` | URL base de la API del backend | `http://localhost:8080` |

`.env.example` es la plantilla. Cópiala como `.env.local` (este archivo no se sube al repositorio):

```bash
cp .env.example .env.local
```

Si cambias una variable, reinicia `npm run dev`.

## Desarrollo

```bash
npm run dev
```

Abre `http://localhost:5173`. Para entrar puedes usar los usuarios de prueba que aparecen en el README de la raíz, en la sección Seguridad. Al iniciar sesión, cada rol llega a su espacio:

| Rol | Destino |
|---|---|
| Huésped | `/` |
| Recepción y administración | `/panel` |
| Personal de servicio | `/servicio` |

## Otros comandos

| Comando | Qué hace |
|---|---|
| `npm run build` | Revisa los tipos y genera la versión de producción en `dist/` |
| `npm run preview` | Sirve localmente la versión de `dist/` |
| `npm run lint` | Revisa el código con oxlint |

## Estructura

```
src/
  api/            cliente HTTP (cliente.ts), tipos del contrato y consultas públicas (publico.ts)
  auth/           sesión, AuthContext, RutaProtegida y roles
  components/
    coral/        componentes propios de Coral: Logo, EstadoReserva, Dinero, Fecha, TablaResponsive...
    ui/           componentes de shadcn/ui
  layouts/        SitioLayout, PanelLayout y ServicioLayout, con sus menús
  pages/          una carpeta por área: publico, huesped, perfil, panel, servicio, auth y errores
  styles/         tokens.css: colores, fuentes, radios y sombras de Coral
  router.tsx      todas las rutas, con el rol que pide cada una
```

## Convenciones

- Todas las consultas usan React Query. Las consultas públicas de `src/api/publico.ts` traen una función y un hook para cada operación.
- El cliente agrega el token y convierte las respuestas de error del backend en un `ErrorApi` con `status`, `message` y `errores`. Con un 401 cierra la sesión y lleva a `/login?expirado=1`; con un 403 lleva a `/sin-permiso`. Los 404 y 409 llegan a la pantalla, y el 409 se muestra con `<AvisoError>`.
- La sesión se guarda en `sessionStorage` y se cierra sola cuando vence el token.
- El modo oscuro se activa con la clase `dark` en `<html>` y queda guardado en `localStorage`.
- Todo el texto visible va en español de Colombia. Cada campo lleva su `label` y cada botón que solo tiene ícono lleva `aria-label`.
