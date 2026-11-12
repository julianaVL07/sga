package co.edu.uniquindio.sga.domain.port;

import co.edu.uniquindio.sga.domain.valueobject.SugerenciaGravedad;

import java.util.Optional;

/**
 * Puerto del modelo de IA que sugiere la gravedad de una novedad a partir de su descripción (10.2, L-21).
 * La integración no es bloqueante: si el modelo no responde, tarda o devuelve algo inválido, el adaptador
 * responde vacío y lo registra en el log. Nunca lanza excepciones ni guarda nada: la gravedad final la elige
 * siempre la persona (7.6).
 */
public interface SugeridorGravedad {

    /**
     * @param descripcion lo que pasó, tal como lo escribió quien reporta.
     * @return la sugerencia (BAJA, MEDIA o ALTA con su explicación) o vacío si no hay ninguna disponible.
     */
    Optional<SugerenciaGravedad> sugerir(String descripcion);
}
