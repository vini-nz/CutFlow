package com.cutflow.repository;

import com.cutflow.entity.PlanoDeCorte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanoDeCorteRepository extends JpaRepository<PlanoDeCorte, Long> {
    Optional<PlanoDeCorte> findByUuidAndProjetoId(UUID uuid, Long projetoId);
    Optional<PlanoDeCorte> findFirstByProjetoIdOrderByGeradoEmDesc(Long projetoId);
}
