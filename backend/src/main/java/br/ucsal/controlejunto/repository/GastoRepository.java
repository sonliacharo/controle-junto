package br.ucsal.controlejunto.repository;

import br.ucsal.controlejunto.domain.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    /** Gastos do usuário no período (RF-014). */
    @Query("""
           select g from Gasto g
            where g.usuario.id = :usuarioId
              and g.data between :inicio and :fim
            order by g.data desc, g.id desc
           """)
    List<Gasto> buscar(@Param("usuarioId") Long usuarioId,
                       @Param("inicio") LocalDate inicio,
                       @Param("fim") LocalDate fim);

    /** Gastos do usuário no período, filtrados por categoria (RF-014). */
    @Query("""
           select g from Gasto g
            where g.usuario.id = :usuarioId
              and g.data between :inicio and :fim
              and g.categoria.id = :categoriaId
            order by g.data desc, g.id desc
           """)
    List<Gasto> buscarPorCategoria(@Param("usuarioId") Long usuarioId,
                                   @Param("inicio") LocalDate inicio,
                                   @Param("fim") LocalDate fim,
                                   @Param("categoriaId") Long categoriaId);

    @Query("""
           select coalesce(sum(g.valor), 0) from Gasto g
            where g.usuario.id = :usuarioId and g.data between :inicio and :fim
           """)
    BigDecimal somarNoPeriodo(@Param("usuarioId") Long usuarioId,
                              @Param("inicio") LocalDate inicio,
                              @Param("fim") LocalDate fim);

    /** Total por categoria no período, usado no dashboard (RF-011). */
    @Query("""
           select g.categoria.nome, sum(g.valor) from Gasto g
            where g.usuario.id = :usuarioId and g.data between :inicio and :fim
            group by g.categoria.nome
            order by sum(g.valor) desc
           """)
    List<Object[]> totalPorCategoria(@Param("usuarioId") Long usuarioId,
                                     @Param("inicio") LocalDate inicio,
                                     @Param("fim") LocalDate fim);
}
