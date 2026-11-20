package br.ucsal.controlejunto.service;

import br.ucsal.controlejunto.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula quanto cada participante deve em uma despesa de grupo.
 * Classe sem dependências do Spring, para facilitar os testes.
 */
public final class CalculadoraDivisao {

    private CalculadoraDivisao() {
    }

    /**
     * RN-001: divide o valor em partes iguais.
     * Os centavos que sobram da divisão vão para os primeiros participantes,
     * para que a soma das partes seja sempre igual ao total.
     * Ex.: R$ 100,00 / 3 = 33,34 + 33,33 + 33,33.
     */
    public static Map<Long, BigDecimal> igualitaria(BigDecimal total, List<Long> participantes) {
        validarTotal(total);
        if (participantes == null || participantes.isEmpty()) {
            throw new RegraNegocioException("Selecione ao menos um participante para dividir a despesa.");
        }
        long centavos = total.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        int n = participantes.size();
        long base = centavos / n;
        long resto = centavos % n;

        Map<Long, BigDecimal> partes = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            long parte = base + (i < resto ? 1 : 0);
            partes.put(participantes.get(i), BigDecimal.valueOf(parte, 2));
        }
        return partes;
    }

    /**
     * RN-008: na divisão personalizada, a soma das partes deve ser igual ao total
     * e cada participante selecionado precisa ter uma parte maior que zero
     * (quem não entra na divisão deve ser desmarcado, e não ficar com R$ 0,00).
     */
    public static Map<Long, BigDecimal> personalizada(BigDecimal total, Map<Long, BigDecimal> valores) {
        validarTotal(total);
        if (valores == null || valores.isEmpty()) {
            throw new RegraNegocioException("Informe o valor de cada participante.");
        }
        BigDecimal soma = BigDecimal.ZERO;
        Map<Long, BigDecimal> partes = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entrada : valores.entrySet()) {
            BigDecimal valor = entrada.getValue() == null ? BigDecimal.ZERO
                    : entrada.getValue().setScale(2, RoundingMode.HALF_UP);
            if (valor.signum() < 0) {
                throw new RegraNegocioException("O valor de um participante não pode ser negativo.");
            }
            if (valor.signum() == 0) {
                throw new RegraNegocioException("Cada participante selecionado precisa ter um valor maior que zero. "
                        + "Desmarque quem não entra na divisão.");
            }
            partes.put(entrada.getKey(), valor);
            soma = soma.add(valor);
        }
        if (soma.compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new RegraNegocioException("A soma das partes (R$ " + soma
                    + ") precisa ser igual ao valor total da despesa (R$ " + total.setScale(2, RoundingMode.HALF_UP) + ").");
        }
        return partes;
    }

    /** RN-009: não aceita valor igual ou menor que zero. */
    public static void validarTotal(BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            throw new RegraNegocioException("O valor deve ser maior que zero.");
        }
    }
}
