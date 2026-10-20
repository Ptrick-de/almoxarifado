package com.almoxarifado.repository;

import com.almoxarifado.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    List<Fornecedor> findAllByEmpresaIdOrderByNome(Long empresaId);
}
