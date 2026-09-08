package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record DocumentoIdentidad(String numero) {

    public DocumentoIdentidad{
        if(numero == null || numero.isBlank() || numero.isEmpty()){
            throw new ReglaDominioException("El documento de identidad es algo obligatorio, y no puede estar vacío");
        }
        numero = numero.trim();
    }
}

