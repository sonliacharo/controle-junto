package br.ucsal.controlejunto.service;

import br.ucsal.controlejunto.domain.*;
import br.ucsal.controlejunto.dto.GrupoDTOs.*;
import br.ucsal.controlejunto.exception.AcessoNegadoException;
import br.ucsal.controlejunto.exception.NaoEncontradoException;
import br.ucsal.controlejunto.exception.RegraNegocioException;
import br.ucsal.controlejunto.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

/**
 * Grupos de despesas compartilhadas:
 * criação (RF-005), participantes (RF-006), despesas (RF-007),
 * saldos (RF-008), quitação (RF-009) e encerramento (RN-003).
 */
@Service
public class GrupoService {

    private static final NumberFormat REAIS = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final GrupoRepository grupos;
    private final ParticipacaoRepository participacoes;
    private final DespesaGrupoRepository despesas;
    private final DividaRepository dividas;
    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final NotificacaoService notificacoes;

    public GrupoService(GrupoRepository grupos, ParticipacaoRepository participacoes,
                        DespesaGrupoRepository despesas, DividaRepository dividas,
                        UsuarioRepository usuarios, CategoriaRepository categorias,
                        NotificacaoService notificacoes) {
        this.grupos = grupos;
        this.participacoes = participacoes;
        this.despesas = despesas;
        this.dividas = dividas;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.notificacoes = notificacoes;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<GrupoResumoResponse> listarMeusGrupos(Long usuarioId) {
        return participacoes.findByUsuarioIdOrderByGrupoDataCriacaoDesc(usuarioId).stream()
                .map(p -> {
                    Grupo g = p.getGrupo();
                    BigDecimal meuSaldo = calcularSaldos(g.getId()).getOrDefault(usuarioId,
                            new CalculadoraSaldos.Saldo(BigDecimal.ZERO, BigDecimal.ZERO)).saldo();
                    return new GrupoResumoResponse(g.getId(), g.getNome(), g.getStatus().name(),
                            g.getDataCriacao(), (int) participacoes.countByGrupoId(g.getId()),
                            p.isAdministrador(), meuSaldo);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public GrupoDetalheResponse detalhar(Long usuarioId, Long grupoId) {
        Participacao minha = participacaoObrigatoria(grupoId, usuarioId);
        Grupo grupo = minha.getGrupo();
        List<Participacao> membros = participacoes.findByGrupoIdOrderByIdAsc(grupoId);

        List<ParticipanteResponse> participantes = membros.stream()
                .map(p -> new ParticipanteResponse(p.getUsuario().getId(), p.getUsuario().getNome(),
                        p.getUsuario().getEmail(), p.getPapel().name()))
                .toList();

        List<Divida> todasDividas = dividas.findByGrupoIdOrderByIdAsc(grupoId);
        Set<Long> despesasComPagamento = new HashSet<>();
        todasDividas.stream().filter(d -> !d.isPendente())
                .forEach(d -> despesasComPagamento.add(d.getDespesa().getId()));

        List<DespesaGrupo> listaDespesas = despesas.findByGrupoIdOrderByDataDescIdDesc(grupoId);
        List<DespesaResponse> despesasResp = listaDespesas.stream()
                .map(d -> paraResponse(d, despesasComPagamento.contains(d.getId())))
                .toList();
        BigDecimal totalGasto = listaDespesas.stream().map(DespesaGrupo::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, CalculadoraSaldos.Saldo> saldos = calcularSaldos(grupoId);
        List<SaldoResponse> saldosResp = membros.stream()
                .map(p -> {
                    var s = saldos.getOrDefault(p.getUsuario().getId(),
                            new CalculadoraSaldos.Saldo(BigDecimal.ZERO, BigDecimal.ZERO));
                    return new SaldoResponse(p.getUsuario().getId(), p.getUsuario().getNome(),
                            s.aReceber(), s.aPagar(), s.saldo());
                })
                .toList();

        List<DividaResponse> dividasResp = todasDividas.stream()
                .map(GrupoService::paraResponse).toList();

        return new GrupoDetalheResponse(grupo.getId(), grupo.getNome(), grupo.getStatus().name(),
                grupo.getDataCriacao(), minha.isAdministrador(), usuarioId, participantes,
                despesasResp, saldosResp, dividasResp, totalGasto);
    }

    // ------------------------------------------------------------------
    // Grupo e participantes
    // ------------------------------------------------------------------

    /** RF-005: quem cria o grupo vira o administrador. */
    @Transactional
    public GrupoDetalheResponse criar(Long usuarioId, GrupoRequest req) {
        Grupo grupo = grupos.save(new Grupo(req.nome().trim()));
        participacoes.save(new Participacao(grupo, usuarios.getReferenceById(usuarioId), PapelGrupo.ADMINISTRADOR));
        return detalhar(usuarioId, grupo.getId());
    }

    /** RF-006 e RN-006 (máximo de 20 participantes). */
    @Transactional
    public GrupoDetalheResponse adicionarParticipante(Long usuarioId, Long grupoId, ParticipanteRequest req) {
        Grupo grupo = administrador(grupoId, usuarioId).getGrupo();
        exigirAtivo(grupo);
        Usuario novo = usuarios.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new NaoEncontradoException(
                        "Nenhum usuário com este e-mail. A pessoa precisa criar uma conta primeiro."));
        if (participacoes.findByGrupoIdAndUsuarioId(grupoId, novo.getId()).isPresent()) {
            throw new RegraNegocioException("Esta pessoa já participa do grupo.");
        }
        if (participacoes.countByGrupoId(grupoId) >= Grupo.LIMITE_PARTICIPANTES) {
            throw new RegraNegocioException("O grupo já tem o máximo de " + Grupo.LIMITE_PARTICIPANTES + " participantes.");
        }
        participacoes.save(new Participacao(grupo, novo, PapelGrupo.MEMBRO));
        notificacoes.notificar(novo, "Você foi adicionado ao grupo \"" + grupo.getNome() + "\".");
        return detalhar(usuarioId, grupoId);
    }

    /** RF-006: remove um participante que ainda não tem dívidas pendentes no grupo. */
    @Transactional
    public GrupoDetalheResponse removerParticipante(Long usuarioId, Long grupoId, Long participanteId) {
        Grupo grupo = administrador(grupoId, usuarioId).getGrupo();
        exigirAtivo(grupo);
        if (participanteId.equals(usuarioId)) {
            throw new RegraNegocioException("O administrador não pode se remover do grupo.");
        }
        Participacao alvo = participacaoObrigatoria(grupoId, participanteId);
        var saldo = calcularSaldos(grupoId).get(participanteId);
        if (saldo != null && (saldo.aPagar().signum() != 0 || saldo.aReceber().signum() != 0)) {
            throw new RegraNegocioException("Não é possível remover: o participante ainda tem valores pendentes no grupo.");
        }
        participacoes.delete(alvo);
        return detalhar(usuarioId, grupoId);
    }

    /** RN-007: grupo sem despesas pode ser excluído pelo administrador. */
    @Transactional
    public void excluir(Long usuarioId, Long grupoId) {
        Grupo grupo = administrador(grupoId, usuarioId).getGrupo();
        if (despesas.countByGrupoId(grupoId) > 0) {
            throw new RegraNegocioException("Só é possível excluir um grupo que não tem despesas. Encerre o grupo quando tudo estiver quitado.");
        }
        participacoes.deleteByGrupoId(grupoId);
        grupos.delete(grupo);
    }

    /** RN-003: o administrador encerra o grupo quando não há dívidas pendentes. */
    @Transactional
    public GrupoDetalheResponse encerrar(Long usuarioId, Long grupoId) {
        Grupo grupo = administrador(grupoId, usuarioId).getGrupo();
        exigirAtivo(grupo);
        long pendentes = dividas.countByGrupoIdAndStatus(grupoId, StatusDivida.PENDENTE);
        if (pendentes > 0) {
            throw new RegraNegocioException("Ainda há " + pendentes + " dívida(s) pendente(s). O grupo só pode ser encerrado com tudo quitado.");
        }
        grupo.encerrar();
        return detalhar(usuarioId, grupoId);
    }

    // ------------------------------------------------------------------
    // Despesas e dívidas
    // ------------------------------------------------------------------

    /**
     * RF-007: registra a despesa, gera as divisões e as dívidas de quem não pagou,
     * e notifica os devedores (RF-012).
     */
    @Transactional
    public GrupoDetalheResponse registrarDespesa(Long usuarioId, Long grupoId, DespesaRequest req) {
        Grupo grupo = participacaoObrigatoria(grupoId, usuarioId).getGrupo();
        exigirAtivo(grupo);

        Map<Long, Usuario> membros = membros(grupoId);
        Usuario pagador = pagador(membros, req);
        Map<Long, BigDecimal> partes = calcularPartes(membros, req);

        DespesaGrupo despesa = new DespesaGrupo(grupo, pagador, membros.get(usuarioId), categoria(req),
                req.descricao().trim(), req.valor(), req.data(), comentario(req));
        partes.forEach((id, valor) -> despesa.adicionarDivisao(membros.get(id), valor));
        despesas.save(despesa);

        gerarDividas(despesa, partes, membros, "Você deve ");
        return detalhar(usuarioId, grupoId);
    }

    /**
     * RN-002: só quem registrou a despesa pode editá-la. A divisão e as dívidas
     * são refeitas; por isso a edição só é permitida enquanto nenhuma dívida
     * da despesa tiver sido quitada.
     */
    @Transactional
    public GrupoDetalheResponse editarDespesa(Long usuarioId, Long grupoId, Long despesaId, DespesaRequest req) {
        DespesaGrupo despesa = despesaDoAutor(usuarioId, grupoId, despesaId, "editada");

        Map<Long, Usuario> membros = membros(grupoId);
        Usuario pagador = pagador(membros, req);
        Map<Long, BigDecimal> partes = calcularPartes(membros, req);

        List<Divida> antigas = dividas.findByDespesaId(despesaId);
        dividas.deleteAll(antigas);
        dividas.flush();

        despesa.atualizar(pagador, categoria(req), req.descricao().trim(), req.valor(), req.data(), comentario(req));
        despesa.limparDivisoes();
        despesas.flush();
        partes.forEach((id, valor) -> despesa.adicionarDivisao(membros.get(id), valor));

        gerarDividas(despesa, partes, membros, "A despesa foi alterada. Agora você deve ");

        // Avisa quem devia antes e não deve mais
        Set<Long> devedoresAtuais = new HashSet<>(partes.keySet());
        devedoresAtuais.remove(pagador.getId());
        antigas.stream().map(Divida::getDevedor)
                .filter(u -> !devedoresAtuais.contains(u.getId()))
                .distinct()
                .forEach(u -> notificacoes.notificar(u, "A despesa \"" + despesa.getDescricao() + "\" do grupo \""
                        + despesa.getGrupo().getNome() + "\" foi alterada e você não deve mais nada por ela."));
        return detalhar(usuarioId, grupoId);
    }

    /** RN-002: só quem registrou a despesa pode excluí-la (e só se nada foi pago ainda). */
    @Transactional
    public GrupoDetalheResponse excluirDespesa(Long usuarioId, Long grupoId, Long despesaId) {
        DespesaGrupo despesa = despesaDoAutor(usuarioId, grupoId, despesaId, "excluída");
        List<Divida> antigas = dividas.findByDespesaId(despesaId);
        antigas.stream().map(Divida::getDevedor).distinct()
                .forEach(u -> notificacoes.notificar(u, "A despesa \"" + despesa.getDescricao() + "\" do grupo \""
                        + despesa.getGrupo().getNome() + "\" foi excluída e a dívida dela foi cancelada."));
        dividas.deleteAll(antigas);
        despesas.delete(despesa);
        return detalhar(usuarioId, grupoId);
    }

    /** RF-009: o participante devedor marca a dívida como paga; quem vai receber é notificado. */
    @Transactional
    public GrupoDetalheResponse quitarDivida(Long usuarioId, Long dividaId) {
        Divida divida = dividas.findById(dividaId)
                .orElseThrow(() -> new NaoEncontradoException("Dívida não encontrada."));
        if (!divida.getDevedor().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Só quem deve pode marcar esta dívida como paga.");
        }
        exigirAtivo(divida.getGrupo());
        if (!divida.isPendente()) {
            throw new RegraNegocioException("Esta dívida já está quitada.");
        }
        divida.quitar();
        notificacoes.notificar(divida.getCredor(), divida.getDevedor().getNome() + " marcou como paga a dívida de "
                + REAIS.format(divida.getValor()) + " com você no grupo \"" + divida.getGrupo().getNome() + "\".");
        return detalhar(usuarioId, divida.getGrupo().getId());
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private Map<Long, Usuario> membros(Long grupoId) {
        Map<Long, Usuario> membros = new HashMap<>();
        participacoes.findByGrupoIdOrderByIdAsc(grupoId)
                .forEach(p -> membros.put(p.getUsuario().getId(), p.getUsuario()));
        return membros;
    }

    private static Usuario pagador(Map<Long, Usuario> membros, DespesaRequest req) {
        return Optional.ofNullable(membros.get(req.pagadorId()))
                .orElseThrow(() -> new RegraNegocioException("Quem pagou precisa ser participante do grupo."));
    }

    /** Valida os participantes e calcula a parte de cada um (RN-001 ou RN-008). */
    private static Map<Long, BigDecimal> calcularPartes(Map<Long, Usuario> membros, DespesaRequest req) {
        List<Long> selecionados = req.participantesIds().stream().distinct().toList();
        for (Long id : selecionados) {
            if (!membros.containsKey(id)) {
                throw new RegraNegocioException("Todos os participantes da divisão precisam fazer parte do grupo.");
            }
        }
        boolean personalizada = req.valores() != null && !req.valores().isEmpty();
        if (personalizada) {
            if (!new HashSet<>(selecionados).equals(req.valores().keySet())) {
                throw new RegraNegocioException("Informe um valor para cada participante selecionado.");
            }
            return CalculadoraDivisao.personalizada(req.valor(), req.valores()); // RN-008
        }
        return CalculadoraDivisao.igualitaria(req.valor(), selecionados);        // RN-001
    }

    /** Cria a dívida de cada participante que não pagou e o notifica (RF-012). */
    private void gerarDividas(DespesaGrupo despesa, Map<Long, BigDecimal> partes,
                              Map<Long, Usuario> membros, String inicioMensagem) {
        Usuario pagador = despesa.getPagador();
        Grupo grupo = despesa.getGrupo();
        partes.forEach((id, valor) -> {
            if (!id.equals(pagador.getId()) && valor.signum() > 0) {
                Usuario devedor = membros.get(id);
                dividas.save(new Divida(grupo, despesa, devedor, pagador, valor));
                notificacoes.notificar(devedor, inicioMensagem + REAIS.format(valor) + " a " + pagador.getNome()
                        + " pela despesa \"" + despesa.getDescricao() + "\" no grupo \"" + grupo.getNome() + "\".");
            }
        });
    }

    private Categoria categoria(DespesaRequest req) {
        return req.categoriaId() == null ? null : categorias.findById(req.categoriaId()).orElse(null);
    }

    private static String comentario(DespesaRequest req) {
        return req.comentario() == null || req.comentario().isBlank() ? null : req.comentario().trim();
    }

    /** Busca a despesa garantindo RN-002 (só o autor) e que ela ainda pode mudar. */
    private DespesaGrupo despesaDoAutor(Long usuarioId, Long grupoId, Long despesaId, String acao) {
        Grupo grupo = participacaoObrigatoria(grupoId, usuarioId).getGrupo();
        exigirAtivo(grupo);
        DespesaGrupo despesa = despesas.findById(despesaId)
                .filter(d -> d.getGrupo().getId().equals(grupoId))
                .orElseThrow(() -> new NaoEncontradoException("Despesa não encontrada."));
        if (!despesa.getRegistradoPor().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Apenas quem registrou a despesa pode alterá-la ou excluí-la.");
        }
        boolean temPagamento = dividas.findByDespesaId(despesaId).stream().anyMatch(d -> !d.isPendente());
        if (temPagamento) {
            throw new RegraNegocioException("Esta despesa não pode ser " + acao
                    + " porque já tem dívida marcada como paga.");
        }
        return despesa;
    }

    private Map<Long, CalculadoraSaldos.Saldo> calcularSaldos(Long grupoId) {
        List<Long> ids = participacoes.findByGrupoIdOrderByIdAsc(grupoId).stream()
                .map(p -> p.getUsuario().getId()).toList();
        List<CalculadoraSaldos.DividaPendente> pendentes = dividas.findByGrupoIdOrderByIdAsc(grupoId).stream()
                .filter(Divida::isPendente)
                .map(d -> new CalculadoraSaldos.DividaPendente(d.getDevedor().getId(), d.getCredor().getId(), d.getValor()))
                .toList();
        return CalculadoraSaldos.calcular(ids, pendentes);
    }

    private Participacao participacaoObrigatoria(Long grupoId, Long usuarioId) {
        if (!grupos.existsById(grupoId)) {
            throw new NaoEncontradoException("Grupo não encontrado.");
        }
        return participacoes.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new AcessoNegadoException("Você não participa deste grupo."));
    }

    private Participacao administrador(Long grupoId, Long usuarioId) {
        Participacao p = participacaoObrigatoria(grupoId, usuarioId);
        if (!p.isAdministrador()) {
            throw new AcessoNegadoException("Apenas o administrador do grupo pode fazer isso.");
        }
        return p;
    }

    private static void exigirAtivo(Grupo grupo) {
        if (!grupo.isAtivo()) {
            throw new RegraNegocioException("Este grupo está encerrado e disponível apenas para consulta.");
        }
    }

    private static DespesaResponse paraResponse(DespesaGrupo d, boolean temPagamento) {
        List<DivisaoResponse> divisoes = d.getDivisoes().stream()
                .map(v -> new DivisaoResponse(v.getUsuario().getId(), v.getUsuario().getNome(), v.getValorDevido()))
                .toList();
        return new DespesaResponse(d.getId(), d.getDescricao(), d.getValor(), d.getData(),
                d.getPagador().getId(), d.getPagador().getNome(),
                d.getCategoria() == null ? null : d.getCategoria().getId(),
                d.getCategoria() == null ? null : d.getCategoria().getNome(),
                d.getComentario(), d.getRegistradoPor().getId(), d.getRegistradoPor().getNome(),
                temPagamento, divisoes);
    }

    private static DividaResponse paraResponse(Divida d) {
        return new DividaResponse(d.getId(), d.getDevedor().getId(), d.getDevedor().getNome(),
                d.getCredor().getId(), d.getCredor().getNome(), d.getValor(), d.getStatus().name(),
                d.getDespesa().getDescricao(), d.getDataQuitacao());
    }
}
