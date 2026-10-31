package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.infrastructure.rest.exception.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * Documentación que publica Swagger UI (/swagger-ui.html), alineada con docs/api/openapi.yaml (Guía 07 y 08).
 * Fija el título, la versión y el orden de los tags del contrato, y declara ErrorResponse (4.6) como cuerpo de
 * los errores: sin esto springdoc pinta los 400, 404 y 409 con el esquema de la respuesta exitosa.
 */
@Configuration
public class OpenApiConfig {

    /** Respuestas del canal externo que sí traen EventoCanalResponse (RN-18 y 7.8): no son ErrorResponse. */
    private static final String RUTA_RESERVAS_CANAL = "/api/canales/{canal}/reservas";
    private static final Set<String> CODIGOS_EVENTO_CANAL = Set.of("409", "422");

    /** Tags en el orden del contrato; springdoc los publica en el orden en que encuentra los controllers. */
    private static final List<Tag> TAGS = List.of(
            new Tag().name("Reservas").description("Ciclo de vida del agregado Reserva en el Apartahotel Coral"),
            new Tag().name("Operación en recepción").description("Registro de llegada y salida del grupo"),
            new Tag().name("Apartamentos")
                    .description("Disponibilidad y operación de los apartamentos del Apartahotel Coral"),
            new Tag().name("Cotizaciones").description("Valor de una estancia en el Apartahotel Coral antes de reservar"),
            new Tag().name("Folio").description("Cuenta de la reserva: cargos, pagos, saldo y cierre"),
            new Tag().name("Canal externo").description("Integración con plataformas de terceros (CU-08)"));

    @Bean
    public OpenAPI sgaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API SGA - Apartahotel Coral")
                        .version("1.0.0")
                        .description("API RESTful del Sistema de Gestión de Alojamiento (SGA) del Apartahotel Coral, "
                                + "Cartagena. Convención de errores (Guía 07, 4.6): 400 el mensaje está mal escrito, "
                                + "404 el identificador no existe, 409 una regla del negocio (RN/RP) lo impide."));
    }

    /**
     * Ordena los tags como el contrato y aplica 4.6: todo 4xx y 5xx responde con ErrorResponse, salvo los rechazos
     * del canal externo (EventoCanalResponse).
     */
    @Bean
    public OpenApiCustomizer contratoSga() {
        return openApi -> {
            openApi.setTags(TAGS);

            ResolvedSchema resuelto = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(new AnnotatedType(ErrorResponse.class));
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas("ErrorResponse", resuelto.schema);
            Schema<?> referencia = new Schema<>().$ref("#/components/schemas/ErrorResponse");

            openApi.getPaths().forEach((ruta, item) -> item.readOperations().forEach(operacion ->
                    operacion.getResponses().forEach((codigo, respuesta) -> {
                        boolean esError = codigo.startsWith("4") || codigo.startsWith("5");
                        boolean esEventoCanal = ruta.startsWith(RUTA_RESERVAS_CANAL)
                                && CODIGOS_EVENTO_CANAL.contains(codigo);
                        if (esError && !esEventoCanal) {
                            respuesta.setContent(new Content()
                                    .addMediaType("application/json", new MediaType().schema(referencia)));
                        }
                    })));
        };
    }
}
