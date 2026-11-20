package br.ucsal.controlejunto.service;

import br.ucsal.controlejunto.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraDivisaoTest {

    @Test
    void divideIgualmenteESomaFechaComOTotal() {
        Map<Long, BigDecimal> partes = CalculadoraDivisao.igualitaria(new BigDecimal("100.00"), List.of(1L, 2L, 3L));

        assertEquals(new BigDecimal("33.34"), partes.get(1L));
        assertEquals(new BigDecimal("33.33"), partes.get(2L));
        assertEquals(new BigDecimal("33.33"), partes.get(3L));
        assertEquals(new BigDecimal("100.00"), partes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void aceitaDivisaoPersonalizadaQuandoASomaBate() {
        Map<Long, BigDecimal> partes = CalculadoraDivisao.personalizada(new BigDecimal("90"),
                Map.of(1L, new BigDecimal("50"), 2L, new BigDecimal("40")));

        assertEquals(new BigDecimal("50.00"), partes.get(1L));
        assertEquals(new BigDecimal("40.00"), partes.get(2L));
    }

    @Test
    void recusaDivisaoPersonalizadaQuandoASomaNaoBate() {
        assertThrows(RegraNegocioException.class, () -> CalculadoraDivisao.personalizada(new BigDecimal("90"),
                Map.of(1L, new BigDecimal("50"), 2L, new BigDecimal("30"))));
    }

    /** Cenário do relatório de testes: R$ 90 atribuídos a um só dos 3 selecionados. */
    @Test
    void recusaParticipanteSelecionadoComParteZero() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> CalculadoraDivisao.personalizada(new BigDecimal("90"),
                        Map.of(1L, new BigDecimal("90"), 2L, BigDecimal.ZERO, 3L, BigDecimal.ZERO)));
        assertTrue(erro.getMessage().contains("maior que zero"));
    }

    @Test
    void recusaValorZeroOuNegativo() {
        assertThrows(RegraNegocioException.class, () -> CalculadoraDivisao.validarTotal(BigDecimal.ZERO));
        assertThrows(RegraNegocioException.class, () -> CalculadoraDivisao.validarTotal(new BigDecimal("-5")));
    }
}
