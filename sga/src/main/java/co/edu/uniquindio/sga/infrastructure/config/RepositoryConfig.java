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
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.AlojamientoRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ApartamentoRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.BloqueoRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ConflictoCanalRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.EventoCanalRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.FolioRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.NovedadRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ReservaRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ServicioAdicionalRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.TemporadaRepositoryInMemory;
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
    public AlojamientoRepository alojamientoRepository() {
        return new AlojamientoRepositoryInMemory();
    }

    @Bean
    public ApartamentoRepository apartamentoRepository() {
        return new ApartamentoRepositoryInMemory();
    }

    @Bean
    public BloqueoRepository bloqueoRepository() {
        return new BloqueoRepositoryInMemory();
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
    public NovedadRepository novedadRepository() {
        return new NovedadRepositoryInMemory();
    }

    @Bean
    public ReservaRepository reservaRepository() {
        return new ReservaRepositoryInMemory();
    }

    @Bean
    public ServicioAdicionalRepository servicioAdicionalRepository() {
        return new ServicioAdicionalRepositoryInMemory();
    }

    @Bean
    public TemporadaRepository temporadaRepository() {
        return new TemporadaRepositoryInMemory();
    }
}