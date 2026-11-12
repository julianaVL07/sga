package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.response.ReporteIngresosResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteOcupacionResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteReservasPorCanalResponse;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal;
import org.springframework.stereotype.Component;

/** Mapper REST de los reportes (7.9). El dinero sale en pesos enteros. */
@Component
public class ReporteMapper {

    public ReporteOcupacionResponse aResponse(ReporteOcupacion reporte) {
        return new ReporteOcupacionResponse(reporte.desde(), reporte.hasta(), reporte.nochesPeriodo(),
                reporte.apartamentosActivos(), reporte.nochesDisponibles(), reporte.nochesVendidas(),
                reporte.porcentajeOcupacion(),
                reporte.apartamentos().stream()
                        .map(a -> new ReporteOcupacionResponse.Apartamento(a.apartamento().codigo(),
                                a.nochesVendidas(), a.porcentajeOcupacion(), a.activo()))
                        .toList());
    }

    public ReporteIngresosResponse aResponse(ReporteIngresos reporte) {
        return new ReporteIngresosResponse(reporte.desde(), reporte.hasta(), pesos(reporte.total()),
                reporte.cantidadPagos(),
                reporte.medios().stream()
                        .map(m -> new ReporteIngresosResponse.Medio(m.medio(), m.cantidadPagos(), pesos(m.total()),
                                m.porcentaje()))
                        .toList());
    }

    public ReporteReservasPorCanalResponse aResponse(ReporteReservasPorCanal reporte) {
        return new ReporteReservasPorCanalResponse(reporte.desde(), reporte.hasta(), reporte.totalReservas(),
                reporte.canales().stream()
                        .map(c -> new ReporteReservasPorCanalResponse.Canal(c.canal(), c.total(), c.activas(),
                                c.finalizadas(), c.canceladas(), c.noShow(), c.porcentaje()))
                        .toList());
    }

    private static long pesos(Dinero dinero) {
        return dinero.monto().longValueExact();
    }
}
