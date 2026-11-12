package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.LoginRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.request.RestablecerContrasenaRequest;
import co.edu.uniquindio.sga.application.dto.request.SolicitarRecuperacionRequest;
import co.edu.uniquindio.sga.application.dto.response.TokenResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioRegistradoResponse;
import co.edu.uniquindio.sga.application.security.RecuperacionContrasenaService;
import co.edu.uniquindio.sga.application.security.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller REST de autenticación (Guía 12) */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión, registro, recuperación de contraseña y emisión del JWT")
public class SecurityController {

    private final SecurityService securityService;
    private final RecuperacionContrasenaService recuperacionContrasenaService;

    /** POST -> /api/auth/login */
    @PostMapping("/login")
    @SecurityRequirements // este endpoint es público: no exige token en Swagger
    @Operation(summary = "Iniciar sesión", description = "Devuelve un JWT para usar como Bearer token")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario inactivo")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(securityService.login(request));
    }

    /** POST -> /api/auth/registro */
    @PostMapping("/registro")
    @SecurityRequirements // este endpoint es público: no exige token en Swagger
    @Operation(summary = "Registrar un huésped", description = "Crea un usuario con rol HUESPED")
    @ApiResponse(responseCode = "201", description = "Usuario registrado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "409", description = "El correo o el documento ya están registrados")
    public ResponseEntity<UsuarioRegistradoResponse> registrar(@Valid @RequestBody RegistroUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.registrar(request));
    }

    /** POST -> /api/auth/recuperacion/solicitar */
    @PostMapping("/recuperacion/solicitar")
    @SecurityRequirements // este endpoint es público: no exige token en Swagger
    @Operation(summary = "Pedir un código para restablecer la contraseña",
            description = "Si el correo es de una cuenta activa, le envía un código de 6 dígitos que vence en "
                    + "15 minutos. Responde 202 exista o no la cuenta, para no revelar qué correos están registrados")
    @ApiResponse(responseCode = "202", description = "Solicitud recibida")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    public ResponseEntity<Void> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        recuperacionContrasenaService.solicitar(request);
        return ResponseEntity.accepted().build();
    }

    /** POST -> /api/auth/recuperacion/restablecer */
    @PostMapping("/recuperacion/restablecer")
    @SecurityRequirements // este endpoint es público: no exige token en Swagger
    @Operation(summary = "Restablecer la contraseña con el código",
            description = "Cambia la contraseña si el código coincide, no ha vencido y no se ha usado. "
                    + "La contraseña nueva sigue la regla del registro")
    @ApiResponse(responseCode = "204", description = "Contraseña restablecida")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, o el código no sirve (incorrecto, vencido o usado)")
    public ResponseEntity<Void> restablecerContrasena(@Valid @RequestBody RestablecerContrasenaRequest request) {
        recuperacionContrasenaService.restablecer(request);
        return ResponseEntity.noContent().build();
    }
}
