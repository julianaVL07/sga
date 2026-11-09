package co.edu.uniquindio.sga.application.security;

import co.edu.uniquindio.sga.application.dto.request.ActualizarPerfilRequest;
import co.edu.uniquindio.sga.application.dto.request.CambioContrasenaRequest;
import co.edu.uniquindio.sga.application.dto.request.CambioRolRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioResponse;

/**
 * Puerto de gestión de usuarios (Guía 12, 7.2). El usuario es de infraestructura: aquí solo hay DTOs.
 * Los roles viajan como texto porque RolUsuario no pertenece a application (3.6).
 */
public interface UsuarioService {

    /** El administrador crea un usuario de cualquier rol. 409 si el correo o el documento ya existen */
    UsuarioResponse crear(CrearUsuarioRequest request);

    /** Página de usuarios (sga.paginacion.tamano); rol y activo son opcionales */
    PaginaResponse<UsuarioResponse> listar(String rol, Boolean activo, int pagina);

    UsuarioResponse consultar(Long id);

    UsuarioResponse cambiarRol(Long id, CambioRolRequest request);

    /**
     * Eliminación lógica (activo = false). No borra al titular ni sus reservas.
     *
     * @param correoSolicitante sub del token del administrador: no puede desactivarse a sí mismo
     */
    UsuarioResponse desactivar(Long id, String correoSolicitante);

    /** Perfil propio. El correo es el sub del token: nadie consulta ni edita el perfil de otro */
    UsuarioResponse consultarPerfil(String correo);

    /** Actualiza solo nombre, teléfono y foto; el correo no es editable */
    UsuarioResponse actualizarPerfil(String correo, ActualizarPerfilRequest request);

    /** 400 si la contraseña actual no coincide */
    void cambiarContrasena(String correo, CambioContrasenaRequest request);
}
