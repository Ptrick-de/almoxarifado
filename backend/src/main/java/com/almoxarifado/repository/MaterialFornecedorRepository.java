package com.almoxarifado.repository;

import com.almoxarifado.model.MaterialFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialFornecedorRepository extends JpaRepository<MaterialFornecedor, Long> {

    List<MaterialFornecedor> findAllByEmpresaId(Long empresaId);

    List<MaterialFornecedor> findAllByMaterialId(Long materialId);

    List<MaterialFornecedor> findAllByFornecedorIdAndEmpresaId(Long fornecedorId, Long empresaId);

    Optional<MaterialFornecedor> findByMaterialIdAndFornecedorId(Long materialId, Long fornecedorId);

    void deleteAllByMaterialId(Long materialId);
}
