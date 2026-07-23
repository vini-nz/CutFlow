package com.cutflow.dto.organizacao;

import com.cutflow.entity.Organizacao;
import com.cutflow.enums.PapelMembro;

import java.util.UUID;

/**
 * Organizacao vista por um usuario, junto com o papel DELE nela. O frontend
 * usa o papel para mostrar/esconder gestao de equipe, e "pessoal" para
 * esconder essa gestao nos espacos pessoais (ADR-0006).
 */
public record OrganizacaoResponse(
        UUID uuid,
        String nome,
        String documento,
        boolean pessoal,
        PapelMembro papel
) {
    public static OrganizacaoResponse from(Organizacao organizacao, PapelMembro papel) {
        return new OrganizacaoResponse(organizacao.getUuid(), organizacao.getNome(),
                organizacao.getDocumento(), organizacao.isPessoal(), papel);
    }
}
