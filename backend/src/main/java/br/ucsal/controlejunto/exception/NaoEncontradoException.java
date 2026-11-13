package br.ucsal.controlejunto.exception;

/** Recurso inexistente. Vira HTTP 404. */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
