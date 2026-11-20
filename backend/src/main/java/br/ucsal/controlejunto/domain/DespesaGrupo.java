package br.ucsal.controlejunto.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Despesa registrada dentro de um grupo (RF-007). */
@Entity
@Table(name = "despesa_grupo")
public class DespesaGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    /** Quem pagou a despesa. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "pagador_id")
    private Usuario pagador;

    /** Quem lançou a despesa no sistema (RN-002). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_despesa", nullable = false)
    private LocalDate data;

    /** Observação livre sobre a despesa (RF-016). */
    @Column(length = 300)
    private String comentario;

    @OneToMany(mappedBy = "despesa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Divisao> divisoes = new ArrayList<>();

    protected DespesaGrupo() {
    }

    public DespesaGrupo(Grupo grupo, Usuario pagador, Usuario registradoPor, Categoria categoria,
                        String descricao, BigDecimal valor, LocalDate data, String comentario) {
        this.grupo = grupo;
        this.registradoPor = registradoPor;
        atualizar(pagador, categoria, descricao, valor, data, comentario);
    }

    /** RF-004/RN-002: o autor pode corrigir a despesa; as divisões são refeitas por quem chama. */
    public void atualizar(Usuario pagador, Categoria categoria, String descricao, BigDecimal valor,
                          LocalDate data, String comentario) {
        this.pagador = pagador;
        this.categoria = categoria;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.comentario = comentario;
    }

    public void adicionarDivisao(Usuario usuario, BigDecimal valorDevido) {
        divisoes.add(new Divisao(this, usuario, valorDevido));
    }

    public void limparDivisoes() {
        divisoes.clear();
    }

    public Long getId() { return id; }
    public Grupo getGrupo() { return grupo; }
    public Usuario getPagador() { return pagador; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public Categoria getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getData() { return data; }
    public String getComentario() { return comentario; }
    public List<Divisao> getDivisoes() { return divisoes; }
}
