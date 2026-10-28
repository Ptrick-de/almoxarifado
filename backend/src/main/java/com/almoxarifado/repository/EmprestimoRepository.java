package com.almoxarifado.repository;

import com.almoxarifado.model.Emprestimo;
import com.almoxarifado.model.StatusEmprestimo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long>, JpaSpecificationExecutor<Emprestimo> {

    Optional<Emprestimo> findByIdAndEmpresaId(Long id, Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Emprestimo e WHERE e.id = :id AND e.empresa.id = :empresaId")
    Optional<Emprestimo> buscarParaAtualizar(@Param("id") Long id, @Param("empresaId") Long empresaId);

    Optional<Emprestimo> findByChaveIdempotenciaAndEmpresaId(String chave, Long empresaId);

    List<Emprestimo> findAllByEmpresaIdAndStatus(Long empresaId, StatusEmprestimo status);

    boolean existsByMaterialId(Long materialId);

    /** Soma, por material, o que ainda está emprestado (empréstimos ativos). Linhas: [materialId, quantidade]. */
    @Query("SELECT e.material.id, SUM(e.quantidade - e.quantidadeDevolvida) FROM Emprestimo e " +
           "WHERE e.empresa.id = :empresaId AND e.status = :status GROUP BY e.material.id")
    List<Object[]> somarPendentesPorMaterial(@Param("empresaId") Long empresaId, @Param("status") StatusEmprestimo status);

    @Query("SELECT COALESCE(SUM(e.quantidade - e.quantidadeDevolvida), 0L) FROM Emprestimo e " +
           "WHERE e.material.id = :materialId AND e.status = :status")
    Long somarPendentesDoMaterial(@Param("materialId") Long materialId, @Param("status") StatusEmprestimo status);
}
