package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.util.List;

/**
 * Dotación y características de un apartamento (L-06). Ej.: tina de hidromasaje, cava de vinos.
 * La lista es inmutable: cambiar la dotación es reemplazarla por otra.
 */
public record Dotacion(List<String> elementos) {

    public Dotacion {
        if (elementos == null || elementos.isEmpty()) {
            throw new ReglaDominioException("L-06: la dotación debe tener al menos un elemento");
        }
        if (elementos.stream().anyMatch(e -> e == null || e.isBlank())) {
            throw new ReglaDominioException("L-06: los elementos de la dotación no pueden estar vacíos");
        }
        elementos = elementos.stream().map(String::trim).distinct().toList();
    }

    public boolean contieneElemento(String elemento) {
        return elemento != null && elementos.stream().anyMatch(e -> e.equalsIgnoreCase(elemento.trim()));
    }
}
