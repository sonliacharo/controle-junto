package br.ucsal.controlejunto.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Parte de uma despesa de grupo que cabe a um participante. */
@Entity
@Table(name = "divisao")
public class Divisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "despesa_id")
    private DespesaGrupo despesa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "valor_devido", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDevido;

    protected Divisao() {
    }

    public Divisao(DespesaGrupo despesa, Usuario usuario, BigDecimal valorDevido) {
        this.despesa = despesa;
        this.usuario = usuario;
        this.valorDevido = valorDevido;
    }

    public Long getId() { return id; }
    public DespesaGrupo getDespesa() { return despesa; }
    public Usuario getUsuario() { return usuario; }
    public BigDecimal getValorDevido() { return valorDevido; }
}
