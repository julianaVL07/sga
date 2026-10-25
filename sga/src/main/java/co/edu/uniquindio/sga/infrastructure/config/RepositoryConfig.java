package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ConflictoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ConflictoCanalRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.EventoCanalRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.FolioRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.AlojamientoJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.AlojamientoJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ApartamentoJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ApartamentoJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.BloqueoJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.BloqueoJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.NovedadJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.NovedadJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservaJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservaJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ServicioAdicionalJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ServicioAdicionalJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.TemporadaJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.TemporadaJpaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.AlojamientoPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ApartamentoPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.NovedadPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ReservaPersistenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Único lugar donde se elige qué adaptador implementa cada puerto *Repository del dominio.
 * Fases 1 a 3: adaptadores en memoria. En la Fase 4 se cambia el cuerpo de cada método por el adaptador JPA
 * y ninguna otra clase se entera (arquitectura hexagonal). Las clases en memoria no llevan @Repository:
 * si lo tuvieran, Spring encontraría dos beans del mismo tipo.
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

    @Bean
    public ConflictoCanalRepository conflictoCanalRepository() {
        return new ConflictoCanalRepositoryInMemory();
    }

    @Bean
    public EventoCanalRepository eventoCanalRepository() {
        return new EventoCanalRepositoryInMemory();
    }

    @Bean
    public FolioRepository folioRepository() {
        return new FolioRepositoryInMemory();
    }

    @Bean
    public NovedadRepository novedadRepository(NovedadJpaDataRepository dataRepository,
                                               NovedadPersistenceMapper mapper) {
        return new NovedadJpaRepository(dataRepository, mapper);
    }

    /** Fase 4: Reserva ya se guarda en H2 con su adaptador JPA. */
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