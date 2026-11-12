package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.port.CredencialesCanal;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoAutenticacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Canales de venta externos y su credencial (10.3.1). Emite la credencial (solo se guarda su hash) y decide si un
 * mensaje entrante viene de un canal autorizado. Cada intento rechazado queda en la bitácora (7.8).
 */
public class CanalVentaService {

    /** Largo máximo del nombre que se copia a la bitácora cuando el canal no existe. */
    private static final int LARGO_MAXIMO_NOMBRE_EN_BITACORA = 60;

    /** Canal con la credencial en claro: solo existe en memoria y se muestra una sola vez. */
    public record CredencialEmitida(CanalVenta canal, String credencial) {
    }

    private final CanalVentaRepository canalVentaRepository;
    private final EventoCanalRepository eventoCanalRepository;
    private final CredencialesCanal credenciales;

    public CanalVentaService(CanalVentaRepository canalVentaRepository, EventoCanalRepository eventoCanalRepository,
                             CredencialesCanal credenciales) {
        this.canalVentaRepository = Objects.requireNonNull(canalVentaRepository);
        this.eventoCanalRepository = Objects.requireNonNull(eventoCanalRepository);
        this.credenciales = Objects.requireNonNull(credenciales);
    }

    /** Crea un canal con una credencial generada al azar. El nombre es único. */
    public CredencialEmitida registrar(String nombre, LocalDateTime ahora) {
        return registrar(nombre, credenciales.generar(), ahora);
    }

    /**
     * Crea un canal con una credencial ya conocida (los datos iniciales la leen de la configuración).
     * El nombre es único.
     */
    public CredencialEmitida registrar(String nombre, String credencial, LocalDateTime ahora) {
        String normalizado = CanalVenta.normalizarNombre(nombre);
        if (normalizado != null && canalVentaRepository.existePorNombre(normalizado)) {
            throw new ReglaDominioException("10.3.1: Ya existe un canal llamado " + normalizado);
        }
        if (credencial == null || credencial.isBlank()) {
            throw new ReglaDominioException("El canal debe tener una credencial");
        }
        CanalVenta canal = CanalVenta.registrar(normalizado, credenciales.cifrar(credencial), ahora);
        return new CredencialEmitida(canalVentaRepository.guardar(canal), credencial);
    }

    /** Emite una credencial nueva; la anterior deja de servir desde este momento. */
    public CredencialEmitida regenerarCredencial(CanalVenta canal) {
        Objects.requireNonNull(canal, "El canal es obligatorio");
        String credencial = credenciales.generar();
        canal.regenerarCredencial(credenciales.cifrar(credencial));
        return new CredencialEmitida(canalVentaRepository.guardar(canal), credencial);
    }

    /** Eliminación lógica del canal: sus mensajes se rechazan desde ahora. */
    public CanalVenta desactivar(CanalVenta canal) {
        Objects.requireNonNull(canal, "El canal es obligatorio");
        canal.desactivar();
        return canalVentaRepository.guardar(canal);
    }

    /**
     * 10.3.1: ¿el mensaje viene del canal que dice ser? La credencial debe corresponder a ese canal; si no llega,
     * no corresponde o el canal no existe, es CREDENCIAL_INVALIDA (no se revela cuál de las tres). Si es correcta
     * pero el canal está inactivo, CANAL_INACTIVO. Todo rechazo queda en la bitácora (7.8).
     *
     * @param operacion           operación que el canal intentó (para la bitácora).
     * @param identificadorExterno identificador de la reserva si ya se conoce (en la cancelación va en la ruta).
     */
    public ResultadoAutenticacionCanal autenticar(String nombreCanal, String credencial, TipoEventoCanal operacion,
                                                  String identificadorExterno, LocalDateTime ahora) {
        if (operacion == null || ahora == null) {
            throw new ReglaDominioException("La operación y la fecha del intento son obligatorias");
        }
        String normalizado = CanalVenta.normalizarNombre(nombreCanal);
        Optional<CanalVenta> canal = normalizado == null ? Optional.empty()
                : canalVentaRepository.buscarPorNombre(normalizado);

        ResultadoAutenticacionCanal resultado;
        if (canal.isEmpty() || credencial == null || credencial.isBlank()
                || !credenciales.coincide(credencial, canal.get().getHashCredencial())) {
            resultado = ResultadoAutenticacionCanal.CREDENCIAL_INVALIDA;
        } else if (!canal.get().admiteMensajes()) {
            resultado = ResultadoAutenticacionCanal.CANAL_INACTIVO;
        } else {
            return ResultadoAutenticacionCanal.AUTENTICADO;
        }

        ResultadoEventoCanal enBitacora = resultado == ResultadoAutenticacionCanal.CANAL_INACTIVO
                ? ResultadoEventoCanal.RECHAZADO_CANAL_INACTIVO
                : ResultadoEventoCanal.RECHAZADO_CREDENCIAL;
        eventoCanalRepository.guardar(EventoCanal.auditar("EVT-" + UUID.randomUUID(), operacion,
                CanalOrigen.EXTERNO, nombreParaBitacora(normalizado), identificadorExterno, enBitacora, ahora));
        return resultado;
    }

    /** El nombre de un canal que no existe lo escribe quien llama: se recorta para no llenar la bitácora. */
    private static String nombreParaBitacora(String normalizado) {
        if (normalizado == null) {
            return "(sin nombre)";
        }
        return normalizado.length() <= LARGO_MAXIMO_NOMBRE_EN_BITACORA
                ? normalizado : normalizado.substring(0, LARGO_MAXIMO_NOMBRE_EN_BITACORA);
    }
}
