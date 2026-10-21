package co.edu.uniquindio.sga.infrastructure.rest.exception;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptador de salida de errores: traduce las excepciones del SGA a códigos HTTP (Guía 07, 4.6).
 * El dominio no conoce HTTP; esta clase es la única que decide qué código recibe el cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 400: falló una anotación del DTO (@NotBlank, @NotNull, @Email, @Positive...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "La petición tiene datos inválidos", req, errores);
    }

    /** 400: JSON mal formado, fecha imposible o valor de enum que no existe. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> formato(Exception ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST,
                "Formato inválido: revise el JSON, las fechas (AAAA-MM-DD) y los valores permitidos", req, null);
    }

    /** 404: el identificador no existe (excepciones de aplicación). */
    @ExceptionHandler({ReservaNoEncontradaException.class, ApartamentoNoEncontradoException.class,
            FolioNoEncontradoException.class})
    public ResponseEntity<ErrorResponse> noEncontrado(RuntimeException ex, HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    /** 409: toda regla violada, RN-01 a RN-22 y RP-01 a RP-03. La única excepción de dominio del SGA. */
    @ExceptionHandler(ReglaDominioException.class)
    public ResponseEntity<ErrorResponse> reglaDominio(ReglaDominioException ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 500: lo inesperado. La traza va al log, nunca al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generico(Exception ex, HttpServletRequest req) {
        // Los errores propios de Spring (ruta inexistente 404, método no permitido 405) traen su propio código
        if (ex instanceof org.springframework.web.ErrorResponse deSpring) {
            HttpStatus status = HttpStatus.valueOf(deSpring.getStatusCode().value());
            return construir(status, status.getReasonPhrase(), req, null);
        }
        log.error("Error inesperado en {}", req.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje,
                                                    HttpServletRequest req, Map<String, String> errores) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(),
                status.getReasonPhrase(), mensaje, req.getRequestURI(), errores));
    }
}