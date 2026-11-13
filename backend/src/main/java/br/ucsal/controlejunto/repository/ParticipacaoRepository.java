package br.ucsal.controlejunto.repository;

import br.ucsal.controlejunto.domain.Participacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipacaoRepository extends JpaRepository<Participacao, Long> {

    List<Participacao> findByUsuarioIdOrderByGrupoDataCriacaoDesc(Long usuarioId);

    List<Participacao> findByGrupoIdOrderByIdAsc(Long grupoId);

    Optional<Participacao> findByGrupoIdAndUsuarioId(Long grupoId, Long usuarioId);

    long countByGrupoId(Long grupoId);

    void deleteByGrupoId(Long grupoId);
}
