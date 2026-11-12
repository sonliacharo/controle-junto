package br.ucsal.controlejunto.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Grupo de despesas compartilhadas (RF-005). */
@Entity
@Table(name = "grupo")
public class Grupo {

    /** RN-006: um grupo pode ter no máximo 20 participantes. */
    public static final int LIMITE_PARTICIPANTES = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusGrupo status = StatusGrupo.ATIVO;

    @Column(name = "data_criacao", nullable = false)
    private LocalDate dataCriacao = LocalDate.now();

    protected Grupo() {
    }

    public Grupo(String nome) {
        this.nome = nome;
    }

    public boolean isAtivo() {
        return status == StatusGrupo.ATIVO;
    }

    public void encerrar() {
        this.status = StatusGrupo.ENCERRADO;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public StatusGrupo getStatus() { return status; }
    public LocalDate getDataCriacao() { return dataCriacao; }
}
