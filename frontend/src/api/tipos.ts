// Tipos del contrato de la API (docs/api/openapi.yaml del backend)

export type Rol = 'HUESPED' | 'RECEPCIONISTA' | 'ADMINISTRADOR' | 'PERSONAL_SERVICIO'

export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'NO_SHOW'

export type EstadoOperativo =
  | 'PREPARADO'
  | 'OCUPADO'
  | 'PENDIENTE_PREPARACION'
  | 'EN_PREPARACION'
  | 'FUERA_DE_SERVICIO'

export type TipoTemporada = 'BASE' | 'MEDIA' | 'ALTA'

/** Fecha sin hora, "2026-12-12" */
export type FechaISO = string
/** Hora local, "15:00:00" */
export type HoraISO = string

/** Cuerpo de error único del backend */
export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  mensaje: string
  path: string
  /** Campo → mensaje; solo en los 400 de validación */
  errores: Record<string, string> | null
}

export interface Pagina<T> {
  contenido: T[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

// ---------- Autenticación y usuarios ----------

export interface LoginRequest {
  correo: string
  contrasena: string
}

/** POST /api/auth/recuperacion/solicitar: responde 202 exista o no la cuenta */
export interface SolicitarRecuperacionRequest {
  correo: string
}

/** POST /api/auth/recuperacion/restablecer: el código de 6 dígitos que llegó al correo */
export interface RestablecerContrasenaRequest {
  correo: string
  codigo: string
  nuevaContrasena: string
}

export interface TokenResponse {
  token: string
  tipo: string
  /** Instante en que vence el token (ISO-8601) */
  expiraEn: string
  roles: Rol[]
}

export interface RegistroUsuarioRequest {
  nombre: string
  correo: string
  contrasena: string
  telefono: string
  documento: string
  fechaNacimiento: FechaISO
}

export interface UsuarioRegistradoResponse {
  id: number
  nombre: string
  correo: string
  rol: Rol
}

export interface UsuarioResponse {
  id: number
  nombre: string
  correo: string
  telefono: string
  documento: string
  fechaNacimiento: FechaISO
  fotoUrl: string | null
  rol: Rol
  activo: boolean
}

// ---------- Apartamentos ----------

export interface ApartamentoResponse {
  identificacion: string
  nombre: string
  descripcion: string
  dormitorios: number
  capacidadMaxima: number
  dotacion: string[]
  imagenes: string[]
  estadoOperativo: EstadoOperativo
}

export interface ApartamentoDisponibleResponse {
  identificacion: string
  nombre: string
  imagenPrincipal: string | null
  capacidadMaxima: number
  /** Valor estimado de la estancia, en pesos */
  valorEstimado: number
  latitud: number
  longitud: number
}

export interface ImagenResponse {
  url: string
  principal: boolean
}

export interface TarifaResponse {
  idTemporada: string
  valorPorOcupante: number
}

export interface BloqueoResponse {
  id: string
  identificacionApartamento: string
  fechaInicio: FechaISO
  fechaFin: FechaISO
  motivo: string
  vigente: boolean
}

export interface ApartamentoDetalleResponse {
  identificacion: string
  nombre: string
  descripcion: string
  dormitorios: number
  capacidadMaxima: number
  dotacion: string[]
  imagenes: ImagenResponse[]
  imagenPrincipal: string | null
  estadoOperativo: EstadoOperativo
  activo: boolean
  eliminado: boolean
  tarifas: TarifaResponse[]
  bloqueosVigentes: BloqueoResponse[]
}

export interface FiltrosDisponibles {
  entrada: FechaISO
  salida: FechaISO
  ocupantes: number
  /** Una fecha por ocupante, para no cobrar a los menores del umbral */
  fechasNacimiento?: FechaISO[]
  capacidadMinima?: number
  precioMinimo?: number
  precioMaximo?: number
  /** Texto que debe aparecer en la dotación, por ejemplo "hidromasaje" */
  caracteristica?: string
}

// ---------- Alojamiento ----------

export interface ServicioAdicionalResponse {
  id: string
  nombre: string
  descripcion: string
  generaCargo: boolean
  valor: number
  activo: boolean
}

export interface ParametrosAlojamientoResponse {
  umbralEdadFacturable: number | null
  tiempoPreparacionHoras: number | null
  plazoConfirmacionHoras: number | null
  horaLimiteNoShow: HoraISO | null
  horaInicioLlegadaNocturna: HoraISO | null
  estanciaMinimaNoches: number | null
  estanciaMinimaActiva: boolean
  exigeAnticipo: boolean
  porcentajeAnticipo: number
  aceptaMascotas: boolean
  cargoAseoMascota: number
  maximoMascotas: number
  pesoMaximoMascotaKg: number
  /** RP-02: depósito de garantía reembolsable, el mismo que se asienta en el folio */
  depositoGarantia: number | null
}

export interface AlojamientoResponse {
  id: string
  nombre: string
  descripcion: string
  ciudad: string
  direccion: string
  latitud: number
  longitud: number
  horaEntrada: HoraISO
  horaSalida: HoraISO
  normaConvivencia: string
  serviciosAdicionales: ServicioAdicionalResponse[]
  parametros: ParametrosAlojamientoResponse
}

// ---------- Temporadas y política ----------

export interface RangoTemporadaResponse {
  fechaInicio: FechaISO
  fechaFin: FechaISO
}

export interface TemporadaResponse {
  id: string
  nombre: string
  tipo: TipoTemporada
  rangos: RangoTemporadaResponse[]
}

export interface TramoCancelacionResponse {
  antelacionMinimaDias: number
  porcentajeRetencion: number
}

export interface PoliticaCancelacionResponse {
  version: number
  vigenteDesde: FechaISO
  consecuenciaNoShow: string
  tramos: TramoCancelacionResponse[]
}

// ---------- Cotización ----------

export interface OcupanteRequest {
  nombre: string
  tipoDocumento?: string
  numeroDocumento?: string
  fechaNacimiento: FechaISO
}

export interface CotizarEstanciaRequest {
  identificacionApartamento: string
  fechaEntrada: FechaISO
  fechaSalida: FechaISO
  ocupantes: OcupanteRequest[]
}

export interface DetalleNocheResponse {
  fecha: FechaISO
  temporada: string
  tarifaAplicada: number
  ocupantesFacturables: number
  subtotal: number
}

export interface CotizacionResponse {
  identificacionApartamento: string
  fechaEntrada: FechaISO
  fechaSalida: FechaISO
  noches: number
  detalles: DetalleNocheResponse[]
  valorTotal: number
}
