package com.cutflow.dto.colaborador;

import com.cutflow.entity.ConviteProjeto;
import com.cutflow.enums.PapelColaborador;

import java.util.UUID;

public record ConviteResponse(
        UUID uuid,
        String emailAlvo,
        PapelColaborador papel,
        boolean reutilizavel,
        String urlConvite
) {
    public static ConviteResponse from(ConviteProjeto convite, String frontendUrl) {
        return new ConviteResponse(
                convite.getUuid(),
                convite.getEmailAlvo(),
                convite.getPapel(),
                convite.reutilizavel(),
                frontendUrl + "/convite/" + convite.getUuid());
    }
}
