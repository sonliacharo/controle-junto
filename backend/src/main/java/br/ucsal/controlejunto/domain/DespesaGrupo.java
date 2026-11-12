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

    @OneToMany(mappedBy = "despesa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Divisao> divisoes = new ArrayList<>();

    protected DespesaGrupo() {
    }

    public DespesaGrupo(Grupo grupo, Usuario pagador, Usuario registradoPor, Categoria categoria,
                        String descricao, BigDecimal valor, LocalDate data) {
        this.grupo = grupo;
        this.pagador = pagador;
        this.registradoPor = registradoPor;
        this.categoria = categoria;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
    }

    public void adicionarDivisao(Usuario usuario, BigDecimal valorDevido) {
        divisoes.add(new Divisao(this, usuario, valorDevido));
    }

    public Long getId() { return id; }
    public Grupo getGrupo() { return grupo; }
    public Usuario getPagador() { return pagador; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public Categoria getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getData() { return data; }
    public List<Divisao> getDivisoes() { return divisoes; }
}
