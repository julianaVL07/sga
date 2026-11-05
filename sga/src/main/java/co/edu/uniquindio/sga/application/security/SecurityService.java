package co.edu.uniquindio.sga.application.security;

import co.edu.uniquindio.sga.application.dto.request.LoginRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.response.TokenResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioRegistradoResponse;

/** Puerto de autenticación (Guía 12) */
public interface SecurityService {

    TokenResponse login(LoginRequest request);

    UsuarioRegistradoResponse registrar(RegistroUsuarioRequest request);
}
