package br.ucsal.controlejunto.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Liga um usuário a um grupo e guarda o papel dele (administrador ou membro). */
@Entity
@Table(name = "participacao",
       uniqueConstraints = @UniqueConstraint(columnNames = {"grupo_id", "usuario_id"}))
public class Participacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelGrupo papel;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada = LocalDate.now();

    protected Participacao() {
    }

    public Participacao(Grupo grupo, Usuario usuario, PapelGrupo papel) {
        this.grupo = grupo;
        this.usuario = usuario;
        this.papel = papel;
    }

    public boolean isAdministrador() {
        return papel == PapelGrupo.ADMINISTRADOR;
    }

    public Long getId() { return id; }
    public Grupo getGrupo() { return grupo; }
    public Usuario getUsuario() { return usuario; }
    public PapelGrupo getPapel() { return papel; }
    public LocalDate getDataEntrada() { return dataEntrada; }
}
