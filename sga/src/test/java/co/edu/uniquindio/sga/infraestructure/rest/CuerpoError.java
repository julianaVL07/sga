package co.edu.uniquindio.sga.infraestructure.rest;

import org.hamcrest.Matcher;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba el cuerpo ErrorResponse (4.6) campo por campo: timestamp, status, error, mensaje, path y errores.
 * Lo usan las pruebas de integración de cada módulo para el 400, 401, 403, 404 y 409.
 */
final class CuerpoError {

    static final String MENSAJE_400_VALIDACION = "La petición tiene datos inválidos";
    static final String MENSAJE_401 = "Debe iniciar sesión o el token no es válido";
    static final String MENSAJE_403 = "No tiene permiso para realizar esta operación";

    /** Instante ISO-8601 en UTC, por ejemplo 2026-11-12T15:04:05.123Z. */
    private static final String INSTANTE_ISO = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

    private CuerpoError() {
    }

    /** Error sin errores por campo y con el mensaje exacto. */
    static ResultMatcher error(HttpStatus codigo, String mensaje, String path) {
        return error(codigo, is(mensaje), path);
    }

    /** Error sin errores por campo; el mensaje se compara con el matcher (por ejemplo, el prefijo de la regla). */
    static ResultMatcher error(HttpStatus codigo, Matcher<? super String> mensaje, String path) {
        return todos(
                comunes(codigo, mensaje, path),
                jsonPath("$.errores").value(nullValue()));
    }

    /** 400 de validación: mensaje estándar y, en errores, el mensaje de cada campo indicado. */
    static ResultMatcher validacion(String path, Map<String, String> errores) {
        List<ResultMatcher> matchers = new ArrayList<>();
        matchers.add(comunes(HttpStatus.BAD_REQUEST, is(MENSAJE_400_VALIDACION), path));
        errores.forEach((campo, mensaje) -> matchers.add(jsonPath("$.errores['" + campo + "']").value(mensaje)));
        return todos(matchers.toArray(ResultMatcher[]::new));
    }

    private static ResultMatcher todos(ResultMatcher... matchers) {
        return resultado -> {
            for (ResultMatcher matcher : matchers) {
                matcher.match(resultado);
            }
        };
    }

    private static ResultMatcher comunes(HttpStatus codigo, Matcher<? super String> mensaje, String path) {
        return todos(
                status().is(codigo.value()),
                jsonPath("$.timestamp").value(matchesPattern(INSTANTE_ISO)),
                jsonPath("$.status").value(codigo.value()),
                jsonPath("$.error").value(codigo.getReasonPhrase()),
                jsonPath("$.mensaje").value(mensaje),
                jsonPath("$.path").value(path));
    }
}
