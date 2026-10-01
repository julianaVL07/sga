package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Servicio que ofrece el alojamiento, indicando si genera cargo y su valor (7.3, L-17).
 * Ejemplos del Anexo A: paseo por la bahía, cena en balcón, aseo de mascota
 * y la asistencia de recepción nocturna de RP-03.
 *
 * Reglas: RP-03 (el recargo por llegada nocturna se modela como un servicio que genera cargo,
 * así su valor es configuración y no una constante en el código).
 */
public class ServicioAdicional {

    private final String id;
    private String nombre;
    private String descripcion;
    private boolean generaCargo;
    private Dinero valor;
    private boolean activo;

    private ServicioAdicional(String id, String nombre, String descripcion, boolean generaCargo, Dinero valor,
                              boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.generaCargo = generaCargo;
        this.valor = valor;
        this.activo = activo;
    }

    /** Servicio que genera cargo en el folio con el valor indicado. */
    public static ServicioAdicional crearConCargo(String id, String nombre, String descripcion, Dinero valor) {
        validarIdentidad(id, nombre);
        validarValorCobrable(valor);
        return new ServicioAdicional(id, nombre.trim(), descripcion, true, valor, true);
    }

    /** Servicio incluido, sin cargo. */
    public static ServicioAdicional crearSinCargo(String id, String nombre, String descripcion) {
        validarIdentidad(id, nombre);
        return new ServicioAdicional(id, nombre.trim(), descripcion, false, Dinero.cero(), true);
    }

    public static ServicioAdicional reconstruir(String id, String nombre, String descripcion, boolean generaCargo,
                                                Dinero valor, boolean activo) {
        return new ServicioAdicional(id, nombre, descripcion, generaCargo, valor, activo);
    }

    /**
     * Genera el cargo del servicio y lo asienta en el folio. Solo aplica si el servicio está activo
     * y genera cargo. Para RP-03 el servicio de recepción nocturna lo invoca CreacionReservaService
     * cuando Alojamiento.esLlegadaNocturna(...) es verdadero.
     */
    public Cargo generarCargoPara(Folio folio, LocalDate fecha) {
        requerido(folio, "El folio es obligatorio");
        requerido(fecha, "La fecha es obligatoria");
        if (!activo) {
            throw new ReglaDominioException("L-17: El servicio " + nombre + " no está activo");
        }
        if (!generaCargo) {
            throw new ReglaDominioException("L-17: El servicio " + nombre + " no genera cargo");
        }
        Cargo cargo = new Cargo(TipoCargo.SERVICIO_ADICIONAL, nombre, valor, fecha);
        folio.agregarCargo(cargo);
        return cargo;
    }

    /** Cambia el valor a futuro; los cargos ya asentados no se alteran (RN-16). */
    public void actualizarValor(Dinero nuevoValor) {
        validarValorCobrable(nuevoValor);
        this.generaCargo = true;
        this.valor = nuevoValor;
    }

    public void dejarDeCobrar() {
        this.generaCargo = false;
        this.valor = Dinero.cero();
    }

    public void actualizarDescripcion(String nombre, String descripcion) {
        textoRequerido(nombre, "El nombre del servicio es obligatorio");
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
    }

    public void activar() { this.activo = true; }

    public void desactivar() { this.activo = false; }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isGeneraCargo() { return generaCargo; }
    public Dinero getValor() { return valor; }
    public boolean isActivo() { return activo; }

    private static void validarIdentidad(String id, String nombre) {
        textoRequerido(id, "El id del servicio es obligatorio");
        textoRequerido(nombre, "El nombre del servicio es obligatorio");
    }

    private static void validarValorCobrable(Dinero valor) {
        if (valor == null || valor.esCero() || valor.esNegativo()) {
            throw new ReglaDominioException("Un servicio que genera cargo debe tener un valor mayor que cero");
        }
    }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServicioAdicional otro)) return false;
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
