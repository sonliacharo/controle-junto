package br.ucsal.controlejunto.service;

import br.ucsal.controlejunto.domain.Usuario;
import br.ucsal.controlejunto.dto.AuthDTOs.*;
import br.ucsal.controlejunto.exception.NaoEncontradoException;
import br.ucsal.controlejunto.exception.RegraNegocioException;
import br.ucsal.controlejunto.repository.UsuarioRepository;
import br.ucsal.controlejunto.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro (RF-001) e login (RF-002). */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public TokenResponse cadastrar(CadastroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }
        Usuario usuario = usuarios.save(new Usuario(req.nome().trim(), email, encoder.encode(req.senha())));
        return token(usuario);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new RegraNegocioException("E-mail ou senha incorretos."));
        return token(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse me(Long usuarioId) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail());
    }

    private TokenResponse token(Usuario u) {
        return new TokenResponse(jwt.gerarToken(u.getId(), u.getEmail()),
                new UsuarioResponse(u.getId(), u.getNome(), u.getEmail()));
    }
}
