/** Regla de contraseña del registro: 8 a 72 caracteres, al menos una mayúscula y un número */
export function errorContrasena(contrasena: string): string | undefined {
  if (contrasena.length < 8) return 'Mínimo 8 caracteres'
  if (contrasena.length > 72) return 'Máximo 72 caracteres'
  if (!/[A-ZÁÉÍÓÚÑ]/.test(contrasena) || !/\d/.test(contrasena)) return 'Debe tener al menos una mayúscula y un número'
  return undefined
}

export const esCorreo = (texto: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(texto)

export const AYUDA_CONTRASENA = 'Mínimo 8 caracteres, con al menos una mayúscula y un número.'
