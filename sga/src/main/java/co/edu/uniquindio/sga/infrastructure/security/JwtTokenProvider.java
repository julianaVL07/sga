package co.edu.uniquindio.sga.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Construye y firma el JWT del SGA (Guía 12) */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtEncoder encoder;

    @Value("${sga.jwt.emisor}")
    private String emisor;

    public String generateTokenAsString(String correo, String documento, List<String> roles,
                                        Instant emitidoEn, Instant expiraEn) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .issuedAt(emitidoEn)
                .expiresAt(expiraEn)
                .subject(correo)
                .claim("roles", roles)
                .claim("documento", documento) // para verificar el propietario del recurso (7.2)
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
