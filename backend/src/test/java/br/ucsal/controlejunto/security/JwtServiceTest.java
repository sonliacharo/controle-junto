package br.ucsal.controlejunto.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** RNF-005: o token é renovado a cada requisição (expiração por inatividade). */
class JwtServiceTest {

    private final JwtService jwt = new JwtService("segredo-de-teste-com-mais-de-32-caracteres-123", 30);

    @Test
    void renovaTokenValidoMantendoOUsuario() {
        String token = jwt.gerarToken(7L, "teste@controlejunto.com");

        Optional<String> novo = jwt.renovar(token);

        assertTrue(novo.isPresent());
        assertEquals(Optional.of(7L), jwt.validar(novo.get()));
    }

    @Test
    void naoRenovaTokenInvalido() {
        assertTrue(jwt.renovar("token-invalido").isEmpty());
        assertTrue(jwt.validar("token-invalido").isEmpty());
    }
}
