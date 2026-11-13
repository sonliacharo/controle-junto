package br.ucsal.controlejunto.repository;

import br.ucsal.controlejunto.domain.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
}
