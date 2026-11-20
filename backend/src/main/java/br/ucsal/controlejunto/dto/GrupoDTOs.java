package br.ucsal.controlejunto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Grupos, participantes, despesas, saldos e dívidas (RF-005 a RF-009). */
public final class GrupoDTOs {

    private GrupoDTOs() {
    }

    public record GrupoRequest(@NotBlank(message = "Informe o nome do grupo.") @Size(max = 100) String nome) {
    }

    public record ParticipanteRequest(@NotBlank(message = "Informe o e-mail do participante.") String email) {
    }

    /**
     * Nova despesa no grupo.
     * Se "valores" vier vazio, a divisão é igualitária (RN-001).
     * Se vier preenchido (id do usuário -> valor), é personalizada e a soma
     * precisa ser igual ao total (RN-008).
     */
    public record DespesaRequest(
            @NotBlank(message = "Informe a descrição.") @Size(max = 200) String descricao,
            @NotNull(message = "Informe o valor.") BigDecimal valor,
            @NotNull(message = "Informe a data.") LocalDate data,
            @NotNull(message = "Informe quem pagou.") Long pagadorId,
            Long categoriaId,
            @NotEmpty(message = "Selecione os participantes da divisão.") List<Long> participantesIds,
            Map<Long, BigDecimal> valores,
            @Size(max = 300, message = "O comentário pode ter no máximo 300 caracteres.") String comentario) {
    }

    public record ParticipanteResponse(Long usuarioId, String nome, String email, String papel) {
    }

    public record DivisaoResponse(Long usuarioId, String nome, BigDecimal valorDevido) {
    }

    /**
     * temPagamento = true quando alguma dívida gerada pela despesa já foi quitada;
     * nesse caso ela não pode mais ser editada nem excluída.
     */
    public record DespesaResponse(Long id, String descricao, BigDecimal valor, LocalDate data,
                                  Long pagadorId, String pagadorNome, Long categoriaId, String categoria,
                                  String comentario, Long registradoPorId, String registradoPorNome,
                                  boolean temPagamento, List<DivisaoResponse> divisoes) {
    }

    /** saldo > 0: tem a receber; saldo < 0: deve. */
    public record SaldoResponse(Long usuarioId, String nome, BigDecimal aReceber, BigDecimal aPagar,
                                BigDecimal saldo) {
    }

    public record DividaResponse(Long id, Long devedorId, String devedorNome, Long credorId,
                                 String credorNome, BigDecimal valor, String status,
                                 String despesa, LocalDate dataQuitacao) {
    }

    public record GrupoResumoResponse(Long id, String nome, String status, LocalDate dataCriacao,
                                      int participantes, boolean souAdministrador,
                                      BigDecimal meuSaldo) {
    }

    public record GrupoDetalheResponse(Long id, String nome, String status, LocalDate dataCriacao,
                                       boolean souAdministrador, Long meuId,
                                       List<ParticipanteResponse> participantes,
                                       List<DespesaResponse> despesas,
                                       List<SaldoResponse> saldos,
                                       List<DividaResponse> dividas,
                                       BigDecimal totalGasto) {
    }

    public record NotificacaoResponse(Long id, String mensagem, boolean lida, String dataHora) {
    }
}
