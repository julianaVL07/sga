package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.repository.ConflictoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.RecordatorioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.AlojamientoJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.AlojamientoJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ApartamentoJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ApartamentoJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.BloqueoJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.BloqueoJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.CanalVentaJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.CanalVentaJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ConflictoCanalJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ConflictoCanalJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.EventoCanalJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.EventoCanalJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.FolioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.FolioJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.NovedadJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.NovedadJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.PoliticaCancelacionJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.PoliticaCancelacionJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.RecordatorioEnviadoJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.RecordatorioJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservaJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservaJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ServicioAdicionalJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ServicioAdicionalJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.TemporadaJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.TemporadaJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.AlojamientoPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ApartamentoPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.CanalPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.FolioPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.NovedadPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.PoliticaCancelacionPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ReservaPersistenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Único lugar donde se elige qué adaptador implementa cada puerto *Repository del dominio.
 * Al principio se usaron adaptadores en memoria; después se cambió el cuerpo de cada método por el adaptador JPA
 * y ninguna otra clase se enteró (arquitectura hexagonal). Las clases en memoria se conservan para las pruebas
 * y no llevan @Repository: si lo tuvieran, Spring encontraría dos beans del mismo tipo.
 */
@Configuration
public class RepositoryConfig {

    @Bean
    public AlojamientoRepository alojamientoRepository(AlojamientoJpaDataRepository dataRepository,
                                                       TemporadaJpaDataRepository temporadaDataRepository,
                                                       ServicioAdicionalJpaDataRepository servicioDataRepository,
                                                       AlojamientoPersistenceMapper mapper) {
        return new AlojamientoJpaRepository(dataRepository, temporadaDataRepository, servicioDataRepository, mapper);
    }

    @Bean
    public ApartamentoRepository apartamentoRepository(ApartamentoJpaDataRepository dataRepository,
                                                       BloqueoJpaDataRepository bloqueoDataRepository,
                                                       ApartamentoPersistenceMapper mapper) {
        return new ApartamentoJpaRepository(dataRepository, bloqueoDataRepository, mapper);
    }

    @Bean
    public BloqueoRepository bloqueoRepository(BloqueoJpaDataRepository dataRepository,
                                               ApartamentoPersistenceMapper mapper) {
        return new BloqueoJpaRepository(dataRepository, mapper);
    }

    /** 10.3.1: canales de venta externos y el hash de su credencial. */
    @Bean
    public CanalVentaRepository canalVentaRepository(CanalVentaJpaDataRepository dataRepository,
                                                     CanalPersistenceMapper mapper) {
        return new CanalVentaJpaRepository(dataRepository, mapper);
    }

    @Bean
    public ConflictoCanalRepository conflictoCanalRepository(ConflictoCanalJpaDataRepository dataRepository,
                                                             CanalPersistenceMapper mapper) {
        return new ConflictoCanalJpaRepository(dataRepository, mapper);
    }

    @Bean
    public EventoCanalRepository eventoCanalRepository(EventoCanalJpaDataRepository dataRepository,
                                                       CanalPersistenceMapper mapper) {
        return new EventoCanalJpaRepository(dataRepository, mapper);
    }

    @Bean
    public FolioRepository folioRepository(FolioJpaDataRepository dataRepository, FolioPersistenceMapper mapper) {
        return new FolioJpaRepository(dataRepository, mapper);
    }

    @Bean
    public NovedadRepository novedadRepository(NovedadJpaDataRepository dataRepository,
                                               NovedadPersistenceMapper mapper) {
        return new NovedadJpaRepository(dataRepository, mapper);
    }

    /** CU-30: versiones de la política de cancelación (F-07, 7.4). */
    @Bean
    public PoliticaCancelacionRepository politicaCancelacionRepository(
            PoliticaCancelacionJpaDataRepository dataRepository, PoliticaCancelacionPersistenceMapper mapper) {
        return new PoliticaCancelacionJpaRepository(dataRepository, mapper);
    }

    /** 7.8: último recordatorio enviado por reserva y motivo. */
    @Bean
    public RecordatorioRepository recordatorioRepository(RecordatorioEnviadoJpaDataRepository dataRepository) {
        return new RecordatorioJpaRepository(dataRepository);
    }

    @Bean
    public ReservaRepository reservaRepository(ReservaJpaDataRepository dataRepository,
                                               ReservaPersistenceMapper mapper) {
        return new ReservaJpaRepository(dataRepository, mapper);
    }

    @Bean
    public ServicioAdicionalRepository servicioAdicionalRepository(ServicioAdicionalJpaDataRepository dataRepository,
                                                                   AlojamientoPersistenceMapper mapper) {
        return new ServicioAdicionalJpaRepository(dataRepository, mapper);
    }

    @Bean
    public TemporadaRepository temporadaRepository(TemporadaJpaDataRepository dataRepository,
                                                   AlojamientoPersistenceMapper mapper) {
        return new TemporadaJpaRepository(dataRepository, mapper);
    }
}