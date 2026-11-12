package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.infrastructure.rest.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Escribe el ErrorResponse (4.6) de los 401 y 403 que nacen en la cadena de filtros, antes de llegar al
 * GlobalExceptionHandler. La usan la cadena del JWT y el filtro de la credencial del canal.
 */
@Component
public class RespuestaErrorSeguridad {

    // Mapper JSON que Spring Boot 4 ya configura para los controladores: el mismo formato del ErrorResponse
    private final JsonMapper jsonMapper;

    public RespuestaErrorSeguridad(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                         String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), new ErrorResponse(Instant.now(), status.value(),
                status.getReasonPhrase(), mensaje, request.getRequestURI(), null));
    }
}
