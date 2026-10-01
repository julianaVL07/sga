package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Documento de identidad de una persona (7.2). Ej.: tipo "CC", número "1094123456".
 * Dos documentos son iguales si coinciden tipo y número.
 */
public record DocumentoIdentidad(String tipo, String numero) {

    public DocumentoIdentidad {
        if (tipo == null || tipo.isBlank()) {
            throw new ReglaDominioException("7.2: el tipo de documento es obligatorio");
        }
        if (numero == null || numero.isBlank()) {
            throw new ReglaDominioException("7.2: el número de documento es obligatorio");
        }
        tipo = tipo.trim().toUpperCase();
        numero = numero.trim().toUpperCase();
        if (!numero.matches("[A-Z0-9-]+")) {
            throw new ReglaDominioException("7.2: el número de documento solo admite letras, dígitos y guiones");
        }
    }
}
