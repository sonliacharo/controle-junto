package br.ucsal.controlejunto.exception;

/** Lançada quando uma regra de negócio (RN) é violada. Vira HTTP 422. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
