package com.cutflow.dto.projeto;

import com.cutflow.entity.Projeto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * podeEditar (ADR-0006) diz ao frontend se deve mostrar os controles de edicao
 * ou tratar o projeto como somente-leitura (colaborador VISUALIZADOR). E' nulo
 * na lista "Meus projetos" (organizacao ativa = sempre edicao); preenchido no
 * detalhe e na lista "Compartilhados comigo".
 */
public record ProjetoResponse(
        UUID uuid,
        String nome,
        String cliente,
        Boolean podeEditar,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProjetoResponse from(Projeto projeto) {
        return from(projeto, null);
    }

    public static ProjetoResponse from(Projeto projeto, Boolean podeEditar) {
        return new ProjetoResponse(
                projeto.getUuid(),
                projeto.getNome(),
                projeto.getCliente(),
                podeEditar,
                projeto.getCreatedAt(),
                projeto.getUpdatedAt());
    }
}
