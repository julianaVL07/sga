package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;

import java.util.List;

/**
 * Detalle de un apartamento para la administración (CU-20, 7.3) y respuesta de CU-21.
 * Las tarifas son por ocupante facturable y por noche, una por temporada (7.4); solo se muestran los bloqueos
 * vigentes.
 */
public record ApartamentoDetalleResponse(
        String identificacion,
        String nombre,
        String descripcion,
        int dormitorios,
        int capacidadMaxima,
        List<String> dotacion,
        List<ImagenResponse> imagenes,
        String imagenPrincipal,
        EstadoOperativo estadoOperativo,
        boolean activo,
        boolean eliminado,
        List<TarifaResponse> tarifas,
        List<BloqueoResponse> bloqueosVigentes
) {

    public record ImagenResponse(String url, boolean principal) {
    }

    public record TarifaResponse(String idTemporada, long valorPorOcupante) {
    }
}
