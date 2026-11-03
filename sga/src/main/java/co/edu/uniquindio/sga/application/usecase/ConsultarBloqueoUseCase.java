package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.BloqueoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * CU-13. GET /api/apartamentos/{id}/bloqueos/{idBloqueo}
 * Consulta un bloqueo del apartamento; es el recurso al que apunta el Location de POST .../bloqueos.
 * Incluye los bloqueos levantados (se conservan). Solo consulta: no modifica datos.
 * Actor: administrador.
 */
@Service
public class ConsultarBloqueoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final BloqueoRepository bloqueoRepository;

    public ConsultarBloqueoUseCase(ApartamentoRepository apartamentoRepository, BloqueoRepository bloqueoRepository) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.bloqueoRepository = Objects.requireNonNull(bloqueoRepository);
    }

    /** 404 si el apartamento no existe, si el bloqueo no existe o si el bloqueo es de otro apartamento. */
    @Transactional(readOnly = true)
    public Bloqueo consultar(IdentificacionApartamento apartamento, String idBloqueo) {
        if (apartamento == null || !apartamentoRepository.existe(apartamento)) {
            throw new ApartamentoNoEncontradoException(apartamento);
        }
        return bloqueoRepository.buscarPorId(idBloqueo)
                .filter(bloqueo -> bloqueo.perteneceA(apartamento))
                .orElseThrow(() -> new BloqueoNoEncontradoException(apartamento, idBloqueo));
    }
}
