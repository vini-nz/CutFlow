package com.cutflow.repository;

import com.cutflow.entity.ColaboradorProjeto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ColaboradorProjetoRepository extends JpaRepository<ColaboradorProjeto, Long> {

    Optional<ColaboradorProjeto> findByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);
    boolean existsByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);

    List<ColaboradorProjeto> findByProjetoIdOrderByCreatedAtAsc(Long projetoId);
    Optional<ColaboradorProjeto> findByUuidAndProjetoId(UUID uuid, Long projetoId);

    // Base da secao "Compartilhados comigo" (ADR-0006) - projetos que o
    // usuario acessa por convite direto, nao por ser Membro da organizacao.
    List<ColaboradorProjeto> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
}
