package com.cutflow.repository;

import com.cutflow.entity.ConviteProjeto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteProjetoRepository extends JpaRepository<ConviteProjeto, Long> {

    Optional<ConviteProjeto> findByUuid(UUID uuid);
    Optional<ConviteProjeto> findByUuidAndProjetoId(UUID uuid, Long projetoId);

    // Convites "ativos" do projeto (para a UI de compartilhamento): links
    // reutilizaveis (aceitoEm sempre null) e convites direcionados ainda nao
    // aceitos - ambos nao revogados.
    List<ConviteProjeto> findByProjetoIdAndRevogadoFalseAndAceitoEmIsNullOrderByCreatedAtDesc(Long projetoId);
}
