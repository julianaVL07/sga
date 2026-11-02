package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.response.CalendarioOcupacionResponse;
import co.edu.uniquindio.sga.application.dto.response.NocheCalendarioResponse;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.List;

/**
 * Mapper REST del calendario de ocupación (CU-27). MapStruct genera OcupacionMapperImpl al compilar y lo registra
 * como bean (componentModel = "spring").
 */
@Mapper(componentModel = "spring")
public interface OcupacionMapper {

    /** Si la noche no está RESERVADA el código de la reserva es null y codigoReserva queda en null. */
    @Mapping(target = "codigoReserva", source = "codigoReserva.valor")
    NocheCalendarioResponse aNocheResponse(NocheCalendario noche);

    List<NocheCalendarioResponse> aNochesResponse(List<NocheCalendario> noches);

    default CalendarioOcupacionResponse aCalendario(IdentificacionApartamento apartamento, LocalDate desde,
                                                    LocalDate hasta, List<NocheCalendario> noches) {
        return new CalendarioOcupacionResponse(apartamento.codigo(), desde, hasta, aNochesResponse(noches));
    }
}
