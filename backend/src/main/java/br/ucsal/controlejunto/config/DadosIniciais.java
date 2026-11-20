package br.ucsal.controlejunto.config;

import br.ucsal.controlejunto.domain.Categoria;
import br.ucsal.controlejunto.domain.Usuario;
import br.ucsal.controlejunto.dto.GastoDTOs.GastoRequest;
import br.ucsal.controlejunto.dto.GrupoDTOs.*;
import br.ucsal.controlejunto.dto.OrcamentoDTOs.OrcamentoRequest;
import br.ucsal.controlejunto.repository.CategoriaRepository;
import br.ucsal.controlejunto.repository.UsuarioRepository;
import br.ucsal.controlejunto.service.GastoService;
import br.ucsal.controlejunto.service.GrupoService;
import br.ucsal.controlejunto.service.OrcamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cria as categorias e, na primeira execução, usuários e dados de exemplo
 * para a demonstração. Senha de todos os usuários de exemplo: 123456
 */
@Component
public class DadosIniciais implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    static final List<String> CATEGORIAS = List.of("Alimentação", "Transporte", "Moradia", "Saúde",
            "Educação", "Lazer", "Compras", "Contas e Serviços", "Viagem", "Outros");

    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final GastoService gastos;
    private final OrcamentoService orcamentos;
    private final GrupoService grupos;
    private final boolean criarExemplos;

    public DadosIniciais(CategoriaRepository categorias, UsuarioRepository usuarios, PasswordEncoder encoder,
                         GastoService gastos, OrcamentoService orcamentos, GrupoService grupos,
                         @Value("${app.dados-exemplo:true}") boolean criarExemplos) {
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.gastos = gastos;
        this.orcamentos = orcamentos;
        this.grupos = grupos;
        this.criarExemplos = criarExemplos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (String nome : CATEGORIAS) {
            if (categorias.findByNomeIgnoreCase(nome).isEmpty()) {
                categorias.save(new Categoria(nome));
            }
        }
        if (criarExemplos && usuarios.count() == 0) {
            criarExemplos();
            log.info("Dados de exemplo criados. Login: livia@controlejunto.com / senha 123456");
        }
    }

    private void criarExemplos() {
        Usuario livia = usuario("Lívia Alves", "livia@controlejunto.com");
        Usuario alison = usuario("Alison Rocha", "alison@controlejunto.com");
        Usuario denilson = usuario("Denilson Souza", "denilson@controlejunto.com");
        Usuario joao = usuario("João Bruno", "joao@controlejunto.com");
        Usuario lucas = usuario("Lucas Danilo", "lucas@controlejunto.com");

        LocalDate hoje = LocalDate.now();

        // Gastos pessoais da Lívia (mês atual e meses anteriores, para o gráfico)
        gasto(livia, "Mercado Atacadão", "312.40", hoje.withDayOfMonth(2), "Alimentação");
        gasto(livia, "Uber para a faculdade", "23.90", hoje.withDayOfMonth(3), "Transporte");
        gasto(livia, "Conta de luz", "148.75", hoje.withDayOfMonth(5), "Contas e Serviços");
        gasto(livia, "Farmácia", "56.30", hoje.withDayOfMonth(6), "Saúde");
        gasto(livia, "Ingresso cinema", "38.00", hoje.withDayOfMonth(Math.min(7, hoje.getDayOfMonth())), "Lazer");
        gasto(livia, "Aluguel", "850.00", hoje.withDayOfMonth(1), "Moradia");
        for (int i = 1; i <= 5; i++) {
            LocalDate mes = hoje.minusMonths(i).withDayOfMonth(10);
            gasto(livia, "Aluguel", "850.00", mes.withDayOfMonth(1), "Moradia");
            gasto(livia, "Mercado", String.valueOf(280 + i * 23), mes, "Alimentação");
            gasto(livia, "Uber", String.valueOf(60 + i * 11), mes.plusDays(3), "Transporte");
            gasto(livia, "Internet", "99.90", mes.plusDays(5), "Contas e Serviços");
        }
        orcamentos.definir(livia.getId(), new OrcamentoRequest(hoje.getMonthValue(), hoje.getYear(), new BigDecimal("1600.00")));

        // Grupo de viagem
        Long viagem = grupos.criar(livia.getId(), new GrupoRequest("Viagem Chapada 2026")).id();
        for (Usuario u : List.of(alison, denilson, joao)) {
            grupos.adicionarParticipante(livia.getId(), viagem, new ParticipanteRequest(u.getEmail()));
        }
        List<Long> todos = List.of(livia.getId(), alison.getId(), denilson.getId(), joao.getId());
        despesa(livia, viagem, "Pousada em Lençóis (2 diárias)", "960.00", livia, todos, null, hoje.minusDays(6));
        despesa(livia, viagem, "Gasolina ida e volta", "380.00", alison, todos, null, hoje.minusDays(6));
        despesa(livia, viagem, "Mercado para a viagem", "214.60", denilson, todos, null, hoje.minusDays(5));
        despesa(livia, viagem, "Passeio Poço Encantado", "300.00", joao,
                List.of(livia.getId(), alison.getId(), joao.getId()),
                Map.of(livia.getId(), new BigDecimal("100.00"), alison.getId(), new BigDecimal("100.00"),
                        joao.getId(), new BigDecimal("100.00")), hoje.minusDays(4));

        // Grupo já encerrado, para aparecer no histórico (RN-003)
        Long festa = grupos.criar(livia.getId(), new GrupoRequest("Aniversário do Alison")).id();
        grupos.adicionarParticipante(livia.getId(), festa, new ParticipanteRequest(denilson.getEmail()));
        grupos.adicionarParticipante(livia.getId(), festa, new ParticipanteRequest(joao.getEmail()));
        despesa(livia, festa, "Bolo e salgados", "180.00", livia,
                List.of(livia.getId(), denilson.getId(), joao.getId()), null, hoje.minusMonths(1));
        for (DividaResponse d : grupos.detalhar(livia.getId(), festa).dividas()) {
            grupos.quitarDivida(d.devedorId(), d.id());
        }
        grupos.encerrar(livia.getId(), festa);

        // Grupo da república
        Long ape = grupos.criar(lucas.getId(), new GrupoRequest("Apê Pituaçu")).id();
        grupos.adicionarParticipante(lucas.getId(), ape, new ParticipanteRequest(livia.getEmail()));
        despesa(lucas, ape, "Internet do mês", "120.00", lucas, List.of(lucas.getId(), livia.getId()), null, hoje.minusDays(2));
    }

    private Usuario usuario(String nome, String email) {
        return usuarios.save(new Usuario(nome, email, encoder.encode("123456")));
    }

    private void gasto(Usuario u, String descricao, String valor, LocalDate data, String categoria) {
        Long categoriaId = categorias.findByNomeIgnoreCase(categoria).orElseThrow().getId();
        gastos.criar(u.getId(), new GastoRequest(new BigDecimal(valor), data, descricao, categoriaId, null));
    }

    private void despesa(Usuario quemRegistra, Long grupoId, String descricao, String valor, Usuario pagador,
                         List<Long> participantes, Map<Long, BigDecimal> valores, LocalDate data) {
        grupos.registrarDespesa(quemRegistra.getId(), grupoId,
                new DespesaRequest(descricao, new BigDecimal(valor), data, pagador.getId(), null, participantes, valores, null));
    }
}
