package com.cutflow.repository;

import com.cutflow.entity.Projeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    // Todas as buscas sao escopadas pela organizacao ativa (ADR-0005): nunca
    // ha lookup so por uuid, para um usuario nunca alcancar projeto de outra
    // organizacao adivinhando/vendo a URL.
    Optional<Projeto> findByUuidAndOrganizacaoId(UUID uuid, Long organizacaoId);
    Page<Projeto> findByOrganizacaoIdOrderByCreatedAtDesc(Long organizacaoId, Pageable pageable);
}
