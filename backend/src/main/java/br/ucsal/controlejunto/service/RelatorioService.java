package br.ucsal.controlejunto.service;

import br.ucsal.controlejunto.dto.AuthDTOs.UsuarioResponse;
import br.ucsal.controlejunto.dto.GastoDTOs.GastoResponse;
import br.ucsal.controlejunto.dto.OrcamentoDTOs.DashboardResponse;
import br.ucsal.controlejunto.dto.OrcamentoDTOs.TotalCategoria;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * RF-015: exporta o relatório mensal em PDF, com o resumo do orçamento,
 * os totais por categoria, os gastos pessoais e a parte do usuário nas
 * despesas de grupo.
 */
@Service
public class RelatorioService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final String[] MESES = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho",
            "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private static final Color VERDE = new Color(0x0F, 0x76, 0x6E);
    private static final Color CINZA_CLARO = new Color(0xF1, 0xF5, 0xF9);
    private static final Color CINZA_TEXTO = new Color(0x64, 0x74, 0x8B);
    private static final Color VERMELHO = new Color(0xB9, 0x1C, 0x1C);

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, VERDE);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, CINZA_TEXTO);
    private static final Font SECAO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font TEXTO_NEGRITO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font CABECALHO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font ALERTA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, VERMELHO);

    private final AuthService auth;
    private final OrcamentoService orcamentos;
    private final GastoService gastos;

    public RelatorioService(AuthService auth, OrcamentoService orcamentos, GastoService gastos) {
        this.auth = auth;
        this.orcamentos = orcamentos;
        this.gastos = gastos;
    }

    @Transactional(readOnly = true)
    public byte[] relatorioMensal(Long usuarioId, int mes, int ano) {
        UsuarioResponse usuario = auth.me(usuarioId);
        DashboardResponse painel = orcamentos.dashboard(usuarioId, mes, ano);
        List<GastoResponse> itens = gastos.listarParaRelatorio(usuarioId, mes, ano);
        List<GastoResponse> pessoais = itens.stream().filter(g -> "PESSOAL".equals(g.origem())).toList();
        List<GastoResponse> deGrupo = itens.stream().filter(g -> "GRUPO".equals(g.origem())).toList();

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, saida);
        doc.addTitle("Relatório mensal - " + MESES[mes - 1] + " de " + ano);
        doc.addAuthor("ControleJunto");
        doc.open();

        doc.add(new Paragraph("ControleJunto · Relatório mensal", TITULO));
        doc.add(new Paragraph(MESES[mes - 1] + " de " + ano + " · " + usuario.nome() + " (" + usuario.email() + ")",
                SUBTITULO));
        doc.add(new Paragraph("Gerado em " + LocalDateTime.now().format(DATA_HORA), SUBTITULO));

        // Resumo
        secao(doc, "Resumo do mês");
        PdfPTable resumo = tabela(new float[]{3, 2});
        linhaResumo(resumo, "Total de gastos pessoais", reais(painel.totalMes()));
        var orc = painel.orcamento();
        if (orc.valorLimite() != null) {
            linhaResumo(resumo, "Orçamento do mês", reais(orc.valorLimite()));
            linhaResumo(resumo, "Orçamento utilizado", String.format(PT_BR, "%.1f%%", orc.percentualUsado()));
        } else {
            linhaResumo(resumo, "Orçamento do mês", "não definido");
        }
        BigDecimal parteGrupos = deGrupo.stream().map(GastoResponse::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        linhaResumo(resumo, "Sua parte em despesas de grupo", reais(parteGrupos));
        linhaResumo(resumo, "Você deve nos grupos (pendente)", reais(painel.totalAPagarGrupos()));
        linhaResumo(resumo, "Você tem a receber nos grupos (pendente)", reais(painel.totalAReceberGrupos()));
        doc.add(resumo);
        if (orc.alerta()) {
            doc.add(new Paragraph("Atenção: os gastos passaram de " + (int) OrcamentoService.LIMITE_ALERTA
                    + "% do orçamento do mês.", ALERTA));
        }

        // Totais por categoria
        secao(doc, "Gastos pessoais por categoria");
        if (painel.porCategoria().isEmpty()) {
            doc.add(new Paragraph("Nenhum gasto pessoal no mês.", SUBTITULO));
        } else {
            PdfPTable categorias = tabela(new float[]{3, 2, 1});
            cabecalho(categorias, "Categoria", "Total", "%");
            for (TotalCategoria t : painel.porCategoria()) {
                celula(categorias, t.categoria(), false);
                celula(categorias, reais(t.total()), true);
                celula(categorias, percentual(t.total(), painel.totalMes()), true);
            }
            doc.add(categorias);
        }

        // Gastos pessoais
        secao(doc, "Gastos pessoais (" + pessoais.size() + ")");
        if (pessoais.isEmpty()) {
            doc.add(new Paragraph("Nenhum gasto pessoal no mês.", SUBTITULO));
        } else {
            PdfPTable t = tabela(new float[]{1.2f, 4, 1.8f, 1.5f});
            cabecalho(t, "Data", "Descrição", "Categoria", "Valor");
            for (GastoResponse g : pessoais) {
                celula(t, g.data().format(DATA), false);
                celula(t, descricao(g), false);
                celula(t, g.categoria() == null ? "-" : g.categoria().nome(), false);
                celula(t, reais(g.valor()), true);
            }
            totalTabela(t, 3, painel.totalMes());
            doc.add(t);
        }

        // Despesas de grupo
        secao(doc, "Sua parte em despesas de grupo (" + deGrupo.size() + ")");
        if (deGrupo.isEmpty()) {
            doc.add(new Paragraph("Nenhuma despesa de grupo no mês.", SUBTITULO));
        } else {
            PdfPTable t = tabela(new float[]{1.2f, 2, 3.2f, 1.5f, 1.5f});
            cabecalho(t, "Data", "Grupo", "Descrição", "Total", "Sua parte");
            for (GastoResponse g : deGrupo) {
                celula(t, g.data().format(DATA), false);
                celula(t, g.grupoNome(), false);
                celula(t, descricao(g), false);
                celula(t, reais(g.valorTotal()), true);
                celula(t, reais(g.valor()), true);
            }
            totalTabela(t, 4, parteGrupos);
            doc.add(t);
        }

        doc.close();
        return saida.toByteArray();
    }

    // ------------------------------------------------------------------
    // Auxiliares de formatação
    // ------------------------------------------------------------------

    private static String reais(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor == null ? BigDecimal.ZERO : valor);
    }

    private static String percentual(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return "-";
        }
        return String.format(PT_BR, "%.1f%%", parte.doubleValue() * 100 / total.doubleValue());
    }

    private static String descricao(GastoResponse g) {
        return g.comentario() == null || g.comentario().isBlank()
                ? g.descricao() : g.descricao() + "\n" + g.comentario();
    }

    private static void secao(Document doc, String titulo) {
        Paragraph p = new Paragraph(titulo, SECAO);
        p.setSpacingBefore(16);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private static PdfPTable tabela(float[] larguras) {
        PdfPTable t = new PdfPTable(larguras);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        return t;
    }

    private static void cabecalho(PdfPTable t, String... titulos) {
        for (int i = 0; i < titulos.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(titulos[i], CABECALHO));
            c.setBackgroundColor(VERDE);
            c.setPadding(5);
            c.setBorderColor(VERDE);
            if (i > 0 && (titulos[i].equals("Valor") || titulos[i].equals("Total")
                    || titulos[i].equals("Sua parte") || titulos[i].equals("%"))) {
                c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            t.addCell(c);
        }
    }

    private static void celula(PdfPTable t, String texto, boolean direita) {
        PdfPCell c = new PdfPCell(new Phrase(texto, TEXTO));
        c.setPadding(4);
        c.setBorderColor(CINZA_CLARO);
        if (direita) {
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        t.addCell(c);
    }

    private static void linhaResumo(PdfPTable t, String rotulo, String valor) {
        PdfPCell r = new PdfPCell(new Phrase(rotulo, TEXTO));
        r.setPadding(5);
        r.setBackgroundColor(CINZA_CLARO);
        r.setBorderColor(Color.WHITE);
        PdfPCell v = new PdfPCell(new Phrase(valor, TEXTO_NEGRITO));
        v.setPadding(5);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setBorderColor(CINZA_CLARO);
        t.addCell(r);
        t.addCell(v);
    }

    private static void totalTabela(PdfPTable t, int colunasRotulo, BigDecimal total) {
        PdfPCell r = new PdfPCell(new Phrase("Total", TEXTO_NEGRITO));
        r.setColspan(colunasRotulo);
        r.setPadding(4);
        r.setBackgroundColor(CINZA_CLARO);
        r.setBorderColor(CINZA_CLARO);
        PdfPCell v = new PdfPCell(new Phrase(reais(total), TEXTO_NEGRITO));
        v.setPadding(4);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setBackgroundColor(CINZA_CLARO);
        v.setBorderColor(CINZA_CLARO);
        t.addCell(r);
        t.addCell(v);
    }
}
