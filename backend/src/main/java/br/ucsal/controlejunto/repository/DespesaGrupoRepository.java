package br.ucsal.controlejunto.repository;

import br.ucsal.controlejunto.domain.DespesaGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DespesaGrupoRepository extends JpaRepository<DespesaGrupo, Long> {

    List<DespesaGrupo> findByGrupoIdOrderByDataDescIdDesc(Long grupoId);

    long countByGrupoId(Long grupoId);
}
