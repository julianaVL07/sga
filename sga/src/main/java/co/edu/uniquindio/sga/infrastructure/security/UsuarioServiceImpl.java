package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.dto.request.CambioRolRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioResponse;
import co.edu.uniquindio.sga.application.exception.UsuarioNoDesactivableException;
import co.edu.uniquindio.sga.application.exception.UsuarioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.UsuarioYaRegistradoException;
import co.edu.uniquindio.sga.application.security.UsuarioService;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestión de usuarios con Spring Data y Spring Security (Guía 12, 7.2) */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    /** Estados que retienen disponibilidad: su titular no se puede desactivar (7.2) */
    private static final List<EstadoReserva> ESTADOS_VIGENTES =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    private final UsuarioJpaDataRepository usuarios;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder passwordEncoder;
    private final int tamanoPagina;

    public UsuarioServiceImpl(UsuarioJpaDataRepository usuarios, ReservaRepository reservaRepository,
                              PasswordEncoder passwordEncoder, @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.usuarios = usuarios;
        this.reservaRepository = reservaRepository;
        this.passwordEncoder = passwordEncoder;
        this.tamanoPagina = tamanoPagina;
    }

    @Override
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        if (usuarios.existsByCorreo(request.correo()) || usuarios.existsByDocumento(request.documento())) {
            throw new UsuarioYaRegistradoException();
        }
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setTelefono(request.telefono());
        usuario.setDocumento(request.documento());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setRol(RolUsuario.valueOf(request.rol()));
        try {
            return aResponse(usuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            // Dos altas simultáneas pasan la consulta de arriba: decide la restricción única de la tabla
            throw new UsuarioYaRegistradoException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(String rol, Boolean activo, int pagina) {
        RolUsuario filtroRol = rol == null ? null : RolUsuario.valueOf(rol);
        Page<UsuarioEntity> encontrados = usuarios.buscar(filtroRol, activo,
                PageRequest.of(pagina, tamanoPagina, Sort.by("id")));
        return new PaginaResponse<>(encontrados.getContent().stream().map(this::aResponse).toList(),
                encontrados.getNumber(), encontrados.getSize(), encontrados.getTotalElements(),
                encontrados.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse consultar(Long id) {
        return aResponse(buscar(id));
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarRol(Long id, CambioRolRequest request) {
        UsuarioEntity usuario = buscar(id);
        usuario.setRol(RolUsuario.valueOf(request.rol()));
        return aResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse desactivar(Long id, String correoSolicitante) {
        UsuarioEntity usuario = buscar(id);
        if (usuario.getCorreo().equalsIgnoreCase(correoSolicitante)) {
            throw UsuarioNoDesactivableException.porSerElMismo();
        }
        if (esTitularDeReservasVigentes(usuario.getDocumento())) {
            throw UsuarioNoDesactivableException.porReservasActivas();
        }
        // Eliminación lógica: el titular y sus reservas siguen en el sistema; solo deja de iniciar sesión
        usuario.setActivo(false);
        return aResponse(usuario);
    }

    /** Lee el dominio por su puerto: la reserva no sabe que existen usuarios, solo conoce el documento (3.6) */
    private boolean esTitularDeReservasVigentes(String documento) {
        return ESTADOS_VIGENTES.stream()
                .map(estado -> new FiltroReservas(estado, null, null, documento, null, null))
                .anyMatch(filtro -> reservaRepository.listar(filtro, 0, 1).totalElementos() > 0);
    }

    private UsuarioEntity buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }

    private UsuarioResponse aResponse(UsuarioEntity usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getTelefono(),
                usuario.getDocumento(), usuario.getFechaNacimiento(), usuario.getFotoUrl(), usuario.getRol().name(),
                usuario.isActivo());
    }
}
