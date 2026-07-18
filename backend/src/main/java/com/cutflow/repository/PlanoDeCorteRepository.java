package com.cutflow.repository;

import com.cutflow.entity.PlanoDeCorte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanoDeCorteRepository extends JpaRepository<PlanoDeCorte, Long> {
    Optional<PlanoDeCorte> findByUuidAndProjetoId(UUID uuid, Long projetoId);
    Optional<PlanoDeCorte> findFirstByProjetoIdOrderByGeradoEmDesc(Long projetoId);

    // Invalidacao de planos obsoletos (ADR-0004): qualquer mutacao de peca ou
    // chapa torna os planos ja gerados incoerentes com o estado atual do
    // projeto, entao eles sao descartados (a delecao cascateia para
    // chapas_utilizadas/posicionamentos/sobras via FK ON DELETE CASCADE).
    void deleteByProjetoId(Long projetoId);
}
