package com.almoxarifado.repository;

import com.almoxarifado.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findAllByEmpresaIdOrderByNome(Long empresaId);

    Optional<Material> findByCodigoAndEmpresaId(String codigo, Long empresaId);

    boolean existsByCodigoAndEmpresaId(String codigo, Long empresaId);

    @Query("SELECT m FROM Material m WHERE m.empresa.id = :empresaId AND m.quantidadeAtual <= m.quantidadeMinima")
    List<Material> findEstoqueBaixoByEmpresa(@Param("empresaId") Long empresaId);

    @Query("SELECT m FROM Material m WHERE m.empresa.id = :empresaId AND (" +
           "LOWER(m.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(m.codigo) LIKE LOWER(CONCAT('%', :termo, '%')))")
    List<Material> buscarPorNomeOuCodigo(@Param("empresaId") Long empresaId, @Param("termo") String termo);
}
