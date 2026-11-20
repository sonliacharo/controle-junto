package br.ucsal.controlejunto.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Gera e valida os tokens JWT de login.
 * O token expira após o tempo configurado (RNF-005: 30 minutos) e é renovado
 * a cada requisição, então a contagem é de inatividade.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${app.jwt.segredo}") String segredo,
                      @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(Long usuarioId, String email) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("email", email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    /** Devolve o id do usuário se o token for válido e não estiver expirado. */
    public Optional<Long> validar(String token) {
        return ler(token).map(Claims::getSubject).map(Long::valueOf);
    }

    /**
     * RNF-005: se o token for válido, devolve um novo token com mais
     * {@code expiracaoMinutos} a partir de agora. Assim a sessão só expira
     * depois de 30 minutos sem nenhuma requisição (inatividade), e não
     * 30 minutos depois do login.
     */
    public Optional<String> renovar(String token) {
        return ler(token).map(c -> gerarToken(Long.valueOf(c.getSubject()), c.get("email", String.class)));
    }

    private Optional<Claims> ler(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
