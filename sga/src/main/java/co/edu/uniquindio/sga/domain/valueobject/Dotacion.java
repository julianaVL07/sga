package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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

    /**
     * 7.5: algún elemento menciona la característica buscada, sin distinguir mayúsculas ni tildes.
     * Ej.: "hidromasaje" coincide con "Balcón con tina de hidromasaje privada".
     */
    public boolean mencionaCaracteristica(String caracteristica) {
        if (caracteristica == null || caracteristica.isBlank()) {
            return false;
        }
        String buscada = normalizar(caracteristica.trim());
        return elementos.stream().anyMatch(e -> normalizar(e).contains(buscada));
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
