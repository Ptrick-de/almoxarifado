package com.almoxarifado.repository;

import com.almoxarifado.model.Empresa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /** Bloqueia a linha da empresa para serializar a geração de códigos de material. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Empresa e WHERE e.id = :id")
    Optional<Empresa> buscarParaAtualizar(@Param("id") Long id);
}
