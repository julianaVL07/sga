package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.port.SugeridorGravedad;
import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.SugerenciaGravedad;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador real del puerto SugeridorGravedad (10.2): API REST de Gemini en Google AI Studio, con RestClient.
 * El prompt pide una respuesta JSON {"gravedad": "BAJA|MEDIA|ALTA", "explicacion": "..."} y aquí se valida.
 * Es no bloqueante: sin clave, con un error de red, una respuesta lenta (sga.ia.gemini.tiempo-espera) o un JSON
 * inválido, responde vacío y lo registra en el log; la persona registra la novedad igual.
 * La clave (sga.ia.gemini.clave) solo se escribe en application-secrets.properties.
 */
public class SugeridorGravedadGemini implements SugeridorGravedad {

    private static final Logger log = LoggerFactory.getLogger(SugeridorGravedadGemini.class);

    /** Lo que se le manda al modelo de la descripción; el formulario ya limita el largo, esto es un tope extra. */
    private static final int LARGO_MAXIMO_DESCRIPCION = 1000;

    private static final String INSTRUCCIONES = """
            Eres el asistente de mantenimiento de un apartahotel. Clasifica la gravedad de esta novedad \
            (daño, faltante o situación en un apartamento) según estas reglas:
            - ALTA: riesgo para las personas o para el apartamento (gas, fugas, electricidad, inundación, \
            incendio, humo, heridas, estructuras que pueden caer).
            - MEDIA: algo dañado o que no funciona, sin riesgo inmediato (electrodomésticos, plomería, \
            cerraduras, vidrios o muebles rotos).
            - BAJA: faltantes, aseo o detalles menores.
            Responde SOLO con un objeto JSON, sin texto adicional ni bloques de código, con esta forma exacta:
            {"gravedad": "BAJA" | "MEDIA" | "ALTA", "explicacion": "una frase en español de máximo 150 caracteres"}
            Novedad: \"""";

    private final RestClient cliente;
    private final JsonMapper json;
    private final String modelo;
    private final String clave;

    /**
     * @param url          base de la API, por ejemplo https://generativelanguage.googleapis.com/v1beta
     * @param modelo       modelo de Gemini, por ejemplo gemini-2.5-flash
     * @param clave        clave de Google AI Studio; vacía deja el adaptador sin sugerencias
     * @param tiempoEspera tiempo máximo para conectar y para recibir la respuesta
     */
    public SugeridorGravedadGemini(JsonMapper json, String url, String modelo, String clave, Duration tiempoEspera) {
        this.json = Objects.requireNonNull(json);
        this.modelo = Objects.requireNonNull(modelo);
        this.clave = clave == null ? "" : clave.trim();
        Objects.requireNonNull(tiempoEspera);
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(tiempoEspera).build());
        fabrica.setReadTimeout(tiempoEspera);
        this.cliente = RestClient.builder()
                .baseUrl(Objects.requireNonNull(url))
                .requestFactory(fabrica)
                .build();
    }

    @Override
    public Optional<SugerenciaGravedad> sugerir(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return Optional.empty();
        }
        if (clave.isEmpty()) {
            log.warn("IA: falta la clave de Gemini (sga.ia.gemini.clave); no se sugiere la gravedad");
            return Optional.empty();
        }
        try {
            RespuestaGemini respuesta = cliente.post()
                    .uri("/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", clave)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(peticion(descripcion))
                    .retrieve()
                    .body(RespuestaGemini.class);
            Optional<SugerenciaGravedad> sugerencia = interpretar(textoDe(respuesta));
            if (sugerencia.isEmpty()) {
                log.warn("IA: Gemini respondió algo que no es una sugerencia válida; se ignora");
            }
            return sugerencia;
        } catch (RuntimeException e) {
            log.warn("IA: no se pudo obtener la sugerencia de Gemini ({}): {}", e.getClass().getSimpleName(),
                    e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Valida el texto que devolvió el modelo: debe ser el JSON pedido, con una gravedad BAJA, MEDIA o ALTA y una
     * explicación. Acepta que venga envuelto en un bloque ```json. Cualquier otra cosa da vacío.
     */
    Optional<SugerenciaGravedad> interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String limpio = texto.trim();
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            RespuestaModelo respuesta = json.readValue(limpio, RespuestaModelo.class);
            if (respuesta == null || respuesta.gravedad() == null || respuesta.explicacion() == null
                    || respuesta.explicacion().isBlank()) {
                return Optional.empty();
            }
            GravedadNovedad gravedad = GravedadNovedad.valueOf(respuesta.gravedad().trim().toUpperCase(Locale.ROOT));
            return Optional.of(new SugerenciaGravedad(gravedad, respuesta.explicacion()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static PeticionGemini peticion(String descripcion) {
        String texto = descripcion.trim();
        if (texto.length() > LARGO_MAXIMO_DESCRIPCION) {
            texto = texto.substring(0, LARGO_MAXIMO_DESCRIPCION);
        }
        String prompt = INSTRUCCIONES + texto.replace("\"", "'") + "\"";
        return new PeticionGemini(List.of(new Contenido(List.of(new Parte(prompt)))),
                new Configuracion("application/json", 0.0));
    }

    private static String textoDe(RespuestaGemini respuesta) {
        if (respuesta == null || respuesta.candidates() == null || respuesta.candidates().isEmpty()) {
            return null;
        }
        Contenido contenido = respuesta.candidates().getFirst().content();
        if (contenido == null || contenido.parts() == null || contenido.parts().isEmpty()) {
            return null;
        }
        return contenido.parts().getFirst().text();
    }

    // ---------- Cuerpos de la API de Gemini (generateContent); los nombres son los de la API ----------

    record PeticionGemini(List<Contenido> contents, Configuracion generationConfig) {
    }

    record Configuracion(String responseMimeType, double temperature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Contenido(List<Parte> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parte(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaGemini(List<Candidato> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidato(Contenido content) {
    }

    /** El JSON que el prompt le pide al modelo. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaModelo(String gravedad, String explicacion) {
    }
}
