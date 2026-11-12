package br.ucsal.controlejunto.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Aviso exibido dentro do aplicativo (RF-012). */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 300)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida = false;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    protected Notificacao() {
    }

    public Notificacao(Usuario usuario, String mensagem) {
        this.usuario = usuario;
        this.mensagem = mensagem;
    }

    public void marcarComoLida() {
        this.lida = true;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; }
    public LocalDateTime getDataHora() { return dataHora; }
}
