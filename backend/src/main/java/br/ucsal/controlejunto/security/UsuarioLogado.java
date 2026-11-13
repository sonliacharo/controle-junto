package br.ucsal.controlejunto.security;

import br.ucsal.controlejunto.exception.AcessoNegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Atalho para descobrir o id do usuário que fez a requisição. */
public final class UsuarioLogado {

    private UsuarioLogado() {
    }

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new AcessoNegadoException("Faça login para continuar.");
        }
        return id;
    }
}
