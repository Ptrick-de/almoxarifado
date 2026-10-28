package com.almoxarifado.repository;

import com.almoxarifado.model.Material;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findAllByEmpresaIdOrderByNome(Long empresaId);

    List<Material> findAllByCodigoAndEmpresaId(String codigo, Long empresaId);

    boolean existsByCodigoAndEmpresaId(String codigo, Long empresaId);

    /** Lê o material com bloqueio de escrita: impede que duas operações simultâneas gastem o mesmo saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Material m WHERE m.id = :id AND m.empresa.id = :empresaId")
    Optional<Material> buscarParaAtualizar(@Param("id") Long id, @Param("empresaId") Long empresaId);

    @Query("SELECT m FROM Material m WHERE m.empresa.id = :empresaId AND m.quantidadeAtual <= m.quantidadeMinima")
    List<Material> findEstoqueBaixoByEmpresa(@Param("empresaId") Long empresaId);

    @Query("SELECT m.codigo FROM Material m WHERE m.empresa.id = :empresaId AND m.codigo LIKE 'MAT-%'")
    List<String> findCodigosGerados(@Param("empresaId") Long empresaId);

    @Query("SELECT m FROM Material m WHERE m.empresa.id = :empresaId AND LOWER(m.nome) = LOWER(:nome) ORDER BY m.id")
    List<Material> findPorNomeExato(@Param("empresaId") Long empresaId, @Param("nome") String nome);
}
