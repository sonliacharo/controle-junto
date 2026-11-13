package br.ucsal.controlejunto.exception;

/** O usuário não tem permissão para a ação. Vira HTTP 403. */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
