package br.ucsal.controlejunto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de entrada e saída do cadastro e do login (RF-001, RF-002). */
public final class AuthDTOs {

    private AuthDTOs() {
    }

    public record CadastroRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 100) String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe a senha.") @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.") String senha) {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.") String email,
            @NotBlank(message = "Informe a senha.") String senha) {
    }

    public record UsuarioResponse(Long id, String nome, String email) {
    }

    public record TokenResponse(String token, UsuarioResponse usuario) {
    }
}
