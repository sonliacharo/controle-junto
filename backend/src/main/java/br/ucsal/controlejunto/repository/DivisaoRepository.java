package br.ucsal.controlejunto.repository;

import br.ucsal.controlejunto.domain.Divisao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DivisaoRepository extends JpaRepository<Divisao, Long> {

    /**
     * Parte do usuário nas despesas de grupo do período (RF-014).
     * grupoId e categoriaId são filtros opcionais (null = sem filtro).
     */
    @Query("""
           select v from Divisao v
             join fetch v.despesa d
             join fetch d.grupo g
             left join fetch d.categoria c
            where v.usuario.id = :usuarioId
              and d.data between :inicio and :fim
              and (:grupoId is null or g.id = :grupoId)
              and (:categoriaId is null or c.id = :categoriaId)
            order by d.data desc, d.id desc
           """)
    List<Divisao> buscarDoUsuario(@Param("usuarioId") Long usuarioId,
                                  @Param("inicio") LocalDate inicio,
                                  @Param("fim") LocalDate fim,
                                  @Param("grupoId") Long grupoId,
                                  @Param("categoriaId") Long categoriaId);
}
