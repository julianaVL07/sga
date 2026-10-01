package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.util.List;

/**
 * Responsable de la reserva y de su pago (glosario). Existe aunque no tenga cuenta de usuario (3.6):
 * por eso no guarda ningún dato de acceso.
 *
 * Reglas: 3.2 (el titular es siempre un ocupante facturable de la reserva).
 * Se reconoce dentro del grupo por su documento de identidad.
 */
public record Titular(String nombre, DocumentoIdentidad documento, String email, String telefono) {

    private static final String PATRON_EMAIL = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$";

    public Titular {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("3.6: el nombre del titular es obligatorio");
        }
        if (documento == null) {
            throw new ReglaDominioException("3.6: el documento del titular es obligatorio");
        }
        if (email == null || !email.trim().matches(PATRON_EMAIL)) {
            throw new ReglaDominioException("3.6: el correo del titular no es válido");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new ReglaDominioException("3.6: el teléfono del titular es obligatorio");
        }
        nombre = nombre.trim();
        email = email.trim().toLowerCase();
        telefono = telefono.trim();
    }

    public boolean coincideConOcupante(Ocupante ocupante) {
        return ocupante != null && ocupante.tieneDocumento(documento);
    }

    /** 3.2: el titular debe estar entre los ocupantes y ser facturable a la fecha de entrada. */
    public boolean esTitularFacturable(List<Ocupante> ocupantes, Estancia estancia, UmbralEdadFacturable umbral) {
        if (ocupantes == null) {
            return false;
        }
        return ocupantes.stream()
                .filter(this::coincideConOcupante)
                .anyMatch(o -> o.esFacturableEn(estancia, umbral));
    }
}
