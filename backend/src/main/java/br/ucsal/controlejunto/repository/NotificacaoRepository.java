package br.ucsal.controlejunto.repository;

import br.ucsal.controlejunto.domain.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findTop20ByUsuarioIdOrderByDataHoraDesc(Long usuarioId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}
