package com.cutflow.repository;

import com.cutflow.entity.Projeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    // Desde a ADR-0006, um projeto pode ser acessado por Membro da sua
    // organizacao OU por ColaboradorProjeto direto (fora da organizacao) - por
    // isso a busca por uuid nao filtra mais por organizacao aqui; quem autoriza
    // e' ProjetoService.nivelAcesso, olhando os dois caminhos.
    Optional<Projeto> findByUuid(UUID uuid);

    // Lista "Meus projetos" continua escopada pela organizacao ativa (workspace).
    Page<Projeto> findByOrganizacaoIdOrderByCreatedAtDesc(Long organizacaoId, Pageable pageable);
}
