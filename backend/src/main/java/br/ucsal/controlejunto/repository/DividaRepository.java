package br.ucsal.controlejunto.repository;

import br.ucsal.controlejunto.domain.Divida;
import br.ucsal.controlejunto.domain.StatusDivida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DividaRepository extends JpaRepository<Divida, Long> {

    List<Divida> findByGrupoIdOrderByIdAsc(Long grupoId);

    long countByGrupoIdAndStatus(Long grupoId, StatusDivida status);

    /** Quanto o usuário deve, somando todos os grupos (para o dashboard). */
    @Query("""
           select coalesce(sum(d.valor), 0) from Divida d
            where d.devedor.id = :usuarioId and d.status = br.ucsal.controlejunto.domain.StatusDivida.PENDENTE
           """)
    BigDecimal totalAPagar(@Param("usuarioId") Long usuarioId);

    /** Quanto o usuário tem a receber, somando todos os grupos. */
    @Query("""
           select coalesce(sum(d.valor), 0) from Divida d
            where d.credor.id = :usuarioId and d.status = br.ucsal.controlejunto.domain.StatusDivida.PENDENTE
           """)
    BigDecimal totalAReceber(@Param("usuarioId") Long usuarioId);
}
