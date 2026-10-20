package com.almoxarifado.repository;

import com.almoxarifado.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByEmpresaIdOrderByNome(Long empresaId);
}
