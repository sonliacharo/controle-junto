package br.ucsal.controlejunto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Gastos pessoais, categorias e sugestão da IA. */
public final class GastoDTOs {

    private GastoDTOs() {
    }

    public record CategoriaResponse(Long id, String nome) {
    }

    public record GastoRequest(
            @NotNull(message = "Informe o valor.") BigDecimal valor,
            @NotNull(message = "Informe a data.") LocalDate data,
            @NotBlank(message = "Informe a descrição.") @Size(max = 200) String descricao,
            @NotNull(message = "Escolha a categoria.") Long categoriaId,
            @Size(max = 300) String comentario) {
    }

    /**
     * Item da lista de gastos (RF-014).
     * origem = "PESSOAL" (gasto pessoal, editável aqui) ou "GRUPO" (parte do usuário
     * em uma despesa de grupo; valor = parte dele, valorTotal = total da despesa).
     * categoria pode ser null em despesas de grupo sem categoria.
     */
    public record GastoResponse(Long id, BigDecimal valor, LocalDate data, String descricao,
                                CategoriaResponse categoria, String comentario,
                                String origem, Long grupoId, String grupoNome, BigDecimal valorTotal) {
    }

    public record SugestaoRequest(@NotBlank(message = "Informe a descrição.") String descricao) {
    }

    /** origem = "IA" quando veio da API de IA, ou "REGRAS" quando veio da classificação local. */
    public record SugestaoResponse(CategoriaResponse categoria, String origem) {
    }
}
