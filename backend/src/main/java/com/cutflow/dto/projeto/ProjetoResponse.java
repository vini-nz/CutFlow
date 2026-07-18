package com.cutflow.dto.projeto;

import com.cutflow.entity.Projeto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjetoResponse(
        UUID uuid,
        String nome,
        String cliente,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProjetoResponse from(Projeto projeto) {
        return new ProjetoResponse(
                projeto.getUuid(),
                projeto.getNome(),
                projeto.getCliente(),
                projeto.getCreatedAt(),
                projeto.getUpdatedAt());
    }
}
