package com.cutflow.dto.auth;

import com.cutflow.dto.organizacao.OrganizacaoResponse;

import java.util.List;
import java.util.UUID;

/**
 * Estado da sessao para o frontend: quem esta logado, de quais organizacoes e'
 * membro (com o papel em cada) e qual esta ativa. organizacaoAtivaUuid e' nulo
 * quando o usuario ainda nao pertence a nenhuma organizacao (leva ao
 * onboarding de organizacao no frontend).
 */
public record SessaoResponse(
        UsuarioResponse usuario,
        List<OrganizacaoResponse> organizacoes,
        UUID organizacaoAtivaUuid
) {}
