package com.almoxarifado.repository;

import com.almoxarifado.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long>, JpaSpecificationExecutor<Movimentacao> {

    List<Movimentacao> findByMaterialIdAndEmpresaIdOrderByDataMovimentacaoDesc(Long materialId, Long empresaId);

    List<Movimentacao> findAllByEmpresaIdOrderByDataMovimentacaoDesc(Long empresaId);

    List<Movimentacao> findAllByEmprestimoIdAndEmpresaIdOrderByDataMovimentacaoAsc(Long emprestimoId, Long empresaId);

    Optional<Movimentacao> findByChaveIdempotenciaAndEmpresaId(String chave, Long empresaId);

    boolean existsByMaterialId(Long materialId);
}
