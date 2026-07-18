package com.cutflow.dto.peca;

import com.cutflow.entity.Peca;
import com.cutflow.enums.TipoAcabamento;

import java.util.UUID;

public record PecaResponse(
        UUID uuid,
        String nome,
        Integer alturaMm,
        Integer larguraMm,
        Integer espessuraMm,
        Integer quantidade,
        TipoAcabamento tipoAcabamento
) {
    public static PecaResponse from(Peca peca) {
        return new PecaResponse(
                peca.getUuid(),
                peca.getNome(),
                peca.getAlturaMm(),
                peca.getLarguraMm(),
                peca.getEspessuraMm(),
                peca.getQuantidade(),
                peca.getTipoAcabamento());
    }
}
