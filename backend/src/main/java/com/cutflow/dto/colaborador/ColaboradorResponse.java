package com.cutflow.dto.colaborador;

import com.cutflow.entity.ColaboradorProjeto;
import com.cutflow.enums.PapelColaborador;

import java.util.UUID;

public record ColaboradorResponse(
        UUID uuid,
        String nome,
        String email,
        PapelColaborador papel
) {
    public static ColaboradorResponse from(ColaboradorProjeto colaborador) {
        return new ColaboradorResponse(
                colaborador.getUuid(),
                colaborador.getUsuario().getNome(),
                colaborador.getUsuario().getEmail(),
                colaborador.getPapel());
    }
}
