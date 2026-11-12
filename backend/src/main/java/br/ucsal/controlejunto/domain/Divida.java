package br.ucsal.controlejunto.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dívida de um participante (devedor) com quem pagou a despesa (credor).
 * O status "quitada" é apenas informativo: o sistema não movimenta dinheiro.
 */
@Entity
@Table(name = "divida")
public class Divida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "despesa_id")
    private DespesaGrupo despesa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "devedor_id")
    private Usuario devedor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "credor_id")
    private Usuario credor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDivida status = StatusDivida.PENDENTE;

    @Column(name = "data_quitacao")
    private LocalDate dataQuitacao;

    protected Divida() {
    }

    public Divida(Grupo grupo, DespesaGrupo despesa, Usuario devedor, Usuario credor, BigDecimal valor) {
        this.grupo = grupo;
        this.despesa = despesa;
        this.devedor = devedor;
        this.credor = credor;
        this.valor = valor;
    }

    /** RF-009: marca a dívida como paga. */
    public void quitar() {
        this.status = StatusDivida.QUITADA;
        this.dataQuitacao = LocalDate.now();
    }

    public boolean isPendente() {
        return status == StatusDivida.PENDENTE;
    }

    public Long getId() { return id; }
    public Grupo getGrupo() { return grupo; }
    public DespesaGrupo getDespesa() { return despesa; }
    public Usuario getDevedor() { return devedor; }
    public Usuario getCredor() { return credor; }
    public BigDecimal getValor() { return valor; }
    public StatusDivida getStatus() { return status; }
    public LocalDate getDataQuitacao() { return dataQuitacao; }
}
