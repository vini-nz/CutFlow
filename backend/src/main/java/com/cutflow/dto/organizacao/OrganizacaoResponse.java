package com.cutflow.dto.organizacao;

import com.cutflow.entity.Organizacao;
import com.cutflow.enums.PapelMembro;

import java.util.UUID;

/**
 * Organizacao vista por um usuario, junto com o papel DELE nela - o frontend
 * usa o papel para mostrar/esconder acoes de gestao de equipe.
 */
public record OrganizacaoResponse(
        UUID uuid,
        String nome,
        String documento,
        PapelMembro papel
) {
    public static OrganizacaoResponse from(Organizacao organizacao, PapelMembro papel) {
        return new OrganizacaoResponse(organizacao.getUuid(), organizacao.getNome(),
                organizacao.getDocumento(), papel);
    }
}
