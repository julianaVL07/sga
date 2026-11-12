package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/canales/conflictos/{id}/resolver. El administrador deja el motivo con el que da por resuelto el
 * conflicto (RN-18); quién lo resuelve sale del token.
 */
public record ResolverConflictoCanalRequest(
        @NotBlank(message = "El motivo de la resolución es obligatorio")
        @Size(max = 1000, message = "El motivo no puede pasar de 1000 caracteres")
        String motivo
) {
}
