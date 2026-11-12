package br.ucsal.controlejunto.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Orçamento mensal do usuário (RF-013). Um por mês. */
@Entity
@Table(name = "orcamento",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "mes_ref", "ano_ref"}))
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "mes_ref", nullable = false)
    private int mes;

    @Column(name = "ano_ref", nullable = false)
    private int ano;

    @Column(name = "valor_limite", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLimite;

    protected Orcamento() {
    }

    public Orcamento(Usuario usuario, int mes, int ano, BigDecimal valorLimite) {
        this.usuario = usuario;
        this.mes = mes;
        this.ano = ano;
        this.valorLimite = valorLimite;
    }

    public void setValorLimite(BigDecimal valorLimite) { this.valorLimite = valorLimite; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public int getMes() { return mes; }
    public int getAno() { return ano; }
    public BigDecimal getValorLimite() { return valorLimite; }
}
