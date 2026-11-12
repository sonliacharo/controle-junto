package br.ucsal.controlejunto.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Gasto pessoal registrado por um usuário (RF-003). */
@Entity
@Table(name = "gasto")
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_gasto", nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 200)
    private String descricao;

    /** Observação livre do usuário (RF-016). */
    @Column(length = 300)
    private String comentario;

    protected Gasto() {
    }

    public Gasto(Usuario usuario, Categoria categoria, BigDecimal valor, LocalDate data,
                 String descricao, String comentario) {
        this.usuario = usuario;
        atualizar(categoria, valor, data, descricao, comentario);
    }

    public void atualizar(Categoria categoria, BigDecimal valor, LocalDate data,
                          String descricao, String comentario) {
        this.categoria = categoria;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
        this.comentario = comentario;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Categoria getCategoria() { return categoria; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getData() { return data; }
    public String getDescricao() { return descricao; }
    public String getComentario() { return comentario; }
}
