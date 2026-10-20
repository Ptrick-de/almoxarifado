package com.almoxarifado.repository;

import com.almoxarifado.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findByMaterialIdAndEmpresaIdOrderByDataMovimentacaoDesc(Long materialId, Long empresaId);

    List<Movimentacao> findAllByEmpresaIdOrderByDataMovimentacaoDesc(Long empresaId);
}
