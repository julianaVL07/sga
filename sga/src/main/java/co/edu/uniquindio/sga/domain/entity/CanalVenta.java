package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Plataforma de terceros autorizada a vender el inventario del alojamiento (10.3.1, 7.8). Para el dominio todas
 * son el canal EXTERNO (RN-01: venden sobre el mismo inventario); el nombre identifica a cada una.
 *
 * Cada canal se autentica con su propia credencial. El dominio solo guarda el hash: la credencial en claro se
 * muestra una sola vez, al crear el canal o al regenerarla. Un canal no se borra: se desactiva, y desde ese
 * momento sus mensajes se rechazan.
 */
public class CanalVenta {

    /** Minúsculas, números y guiones, de 2 a 40 caracteres: el nombre viaja en la ruta /api/canales/{canal}. */
    private static final Pattern FORMATO_NOMBRE = Pattern.compile("^[a-z0-9][a-z0-9-]{1,39}$");

    /** Palabras de las rutas de administración de /api/canales que no pueden ser el nombre de un canal. */
    private static final Set<String> NOMBRES_RESERVADOS = Set.of("conflictos", "eventos");

    private final String nombre;
    private final CanalOrigen tipo;
    private String hashCredencial;
    private boolean activo;
    private final LocalDateTime fechaCreacion;

    private CanalVenta(String nombre, CanalOrigen tipo, String hashCredencial, boolean activo,
                       LocalDateTime fechaCreacion) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.hashCredencial = hashCredencial;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
    }

    /** Registra un canal externo nuevo, activo, con el hash de su primera credencial. */
    public static CanalVenta registrar(String nombre, String hashCredencial, LocalDateTime fechaCreacion) {
        String normalizado = normalizarNombre(nombre);
        if (normalizado == null || !FORMATO_NOMBRE.matcher(normalizado).matches()) {
            throw new ReglaDominioException("10.3.1: El nombre del canal debe tener de 2 a 40 caracteres entre "
                    + "letras minúsculas, números y guiones, y empezar por letra o número");
        }
        if (NOMBRES_RESERVADOS.contains(normalizado)) {
            throw new ReglaDominioException("10.3.1: El nombre " + normalizado + " está reservado por la API");
        }
        textoRequerido(hashCredencial, "El canal debe tener una credencial");
        if (fechaCreacion == null) {
            throw new ReglaDominioException("La fecha de creación del canal es obligatoria");
        }
        return new CanalVenta(normalizado, CanalOrigen.EXTERNO, hashCredencial, true, fechaCreacion);
    }

    public static CanalVenta reconstruir(String nombre, CanalOrigen tipo, String hashCredencial, boolean activo,
                                         LocalDateTime fechaCreacion) {
        return new CanalVenta(nombre, tipo, hashCredencial, activo, fechaCreacion);
    }

    /**
     * Forma única del nombre: sin espacios a los lados y en minúsculas, para que "Booking" y "booking" sean el
     * mismo canal. Devuelve null si no hay nombre.
     */
    public static String normalizarNombre(String nombre) {
        return nombre == null || nombre.isBlank() ? null : nombre.trim().toLowerCase(Locale.ROOT);
    }

    /** Reemplaza la credencial: la anterior deja de servir. Un canal inactivo no recibe credenciales nuevas. */
    public void regenerarCredencial(String nuevoHash) {
        textoRequerido(nuevoHash, "El canal debe tener una credencial");
        if (!activo) {
            throw new ReglaDominioException("10.3.1: El canal " + nombre + " está inactivo; no se le puede "
                    + "regenerar la credencial");
        }
        this.hashCredencial = nuevoHash;
    }

    /** Eliminación lógica: el canal se conserva con su historia, pero ya no puede enviar mensajes. */
    public void desactivar() {
        if (!activo) {
            throw new ReglaDominioException("10.3.1: El canal " + nombre + " ya está inactivo");
        }
        this.activo = false;
    }

    /** 10.3.1: solo un canal activo puede consultar disponibilidad, crear o cancelar reservas. */
    public boolean admiteMensajes() {
        return activo;
    }

    public String getNombre() { return nombre; }
    public CanalOrigen getTipo() { return tipo; }
    public String getHashCredencial() { return hashCredencial; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanalVenta otro)) return false;
        return nombre.equals(otro.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }
}
