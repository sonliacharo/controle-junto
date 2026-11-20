package br.ucsal.controlejunto.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê o cabeçalho "Authorization: Bearer <token>" e identifica o usuário.
 * Em toda requisição válida devolve um token renovado no cabeçalho
 * {@value #CABECALHO_TOKEN_RENOVADO} (RNF-005: expiração por inatividade).
 */
@Component
public class JwtFiltro extends OncePerRequestFilter {

    public static final String CABECALHO_TOKEN_RENOVADO = "X-Token-Renovado";

    private final JwtService jwtService;

    public JwtFiltro(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            String token = cabecalho.substring(7);
            jwtService.validar(token).ifPresent(usuarioId -> {
                var autenticacao = new UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
                jwtService.renovar(token).ifPresent(novo -> response.setHeader(CABECALHO_TOKEN_RENOVADO, novo));
            });
        }
        chain.doFilter(request, response);
    }
}
