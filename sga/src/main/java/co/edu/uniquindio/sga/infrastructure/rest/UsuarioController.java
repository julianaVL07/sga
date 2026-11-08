package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.ActualizarPerfilRequest;
import co.edu.uniquindio.sga.application.dto.request.CambioContrasenaRequest;
import co.edu.uniquindio.sga.application.dto.request.CambioRolRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioResponse;
import co.edu.uniquindio.sga.application.security.UsuarioService;
import co.edu.uniquindio.sga.infrastructure.security.RolUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Gestión de usuarios (Guía 12, 7.2). Sin DELETE: la baja es lógica (PUT /{id}/desactivar).
 * Nunca responde con la contraseña.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Usuarios y roles internos del SGA")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** POST /api/usuarios */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Crear un usuario", description = "El administrador crea un usuario de cualquier rol")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "El correo o el documento ya están registrados")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioResponse creado = usuarioService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** GET /api/usuarios?rol=&activo=&pagina=0 */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Listar usuarios",
            description = "10 por página (12.4), con filtros opcionales rol y activo. La numeración empieza en 0")
    @ApiResponse(responseCode = "200", description = "Página de usuarios (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa o rol mal escrito")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    public ResponseEntity<PaginaResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(usuarioService.listar(rol == null ? null : rol.name(), activo, pagina));
    }

    /** GET /api/usuarios/me: el usuario del token (sub) */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar mi perfil")
    @ApiResponse(responseCode = "200", description = "Usuario del token")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    public ResponseEntity<UsuarioResponse> consultarPerfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioService.consultarPerfil(jwt.getSubject()));
    }

    /** PUT /api/usuarios/me: solo nombre, teléfono y foto */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar mi perfil", description = "Cambia nombre, teléfono y foto; el correo no es editable")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    public ResponseEntity<UsuarioResponse> actualizarPerfil(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(jwt.getSubject(), request));
    }

    /** PUT /api/usuarios/me/contrasena */
    @PutMapping("/me/contrasena")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar mi contraseña")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "400", description = "Contraseña nueva inválida o contraseña actual incorrecta")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    public ResponseEntity<Void> cambiarContrasena(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CambioContrasenaRequest request) {
        usuarioService.cambiarContrasena(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/usuarios/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Consultar un usuario")
    @ApiResponse(responseCode = "200", description = "Usuario")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    public ResponseEntity<UsuarioResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.consultar(id));
    }

    /** PUT /api/usuarios/{id}/rol */
    @PutMapping("/{id}/rol")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Cambiar el rol de un usuario")
    @ApiResponse(responseCode = "200", description = "Rol cambiado")
    @ApiResponse(responseCode = "400", description = "Rol inválido")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id,
                                                      @Valid @RequestBody CambioRolRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, request));
    }

    /** PUT /api/usuarios/{id}/desactivar */
    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Desactivar un usuario",
            description = "Eliminación lógica: el usuario deja de iniciar sesión; no se borran el titular ni sus reservas")
    @ApiResponse(responseCode = "200", description = "Usuario desactivado")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @ApiResponse(responseCode = "409", description = "Es titular de reservas vigentes o es el mismo administrador")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioService.desactivar(id, jwt.getSubject()));
    }
}
