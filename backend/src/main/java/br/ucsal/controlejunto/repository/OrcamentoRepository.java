package br.ucsal.controlejunto.repository;

import br.ucsal.controlejunto.domain.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    Optional<Orcamento> findByUsuarioIdAndMesAndAno(Long usuarioId, int mes, int ano);
}
