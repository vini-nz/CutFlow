package com.cutflow.dto.chapa;

import com.cutflow.entity.Chapa;

import java.util.UUID;

public record ChapaResponse(
        UUID uuid,
        Integer larguraMm,
        Integer alturaMm,
        Integer espessuraMm,
        String material,
        Integer quantidadeDisponivel,
        Integer kerfMm,
        Integer margemBordaMm
) {
    public static ChapaResponse from(Chapa chapa) {
        return new ChapaResponse(
                chapa.getUuid(),
                chapa.getLarguraMm(),
                chapa.getAlturaMm(),
                chapa.getEspessuraMm(),
                chapa.getMaterial(),
                chapa.getQuantidadeDisponivel(),
                chapa.getKerfMm(),
                chapa.getMargemBordaMm());
    }
}
