package com.cutflow.dto.organizacao;

import com.cutflow.entity.Membro;
import com.cutflow.enums.PapelMembro;

import java.util.UUID;

public record MembroResponse(
        UUID uuid,
        UUID usuarioUuid,
        String nome,
        String email,
        PapelMembro papel
) {
    public static MembroResponse from(Membro membro) {
        return new MembroResponse(
                membro.getUuid(),
                membro.getUsuario().getUuid(),
                membro.getUsuario().getNome(),
                membro.getUsuario().getEmail(),
                membro.getPapel());
    }
}
