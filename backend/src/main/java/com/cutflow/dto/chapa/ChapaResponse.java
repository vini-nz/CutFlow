package com.cutflow.dto.chapa;

import com.cutflow.entity.Chapa;
import com.cutflow.enums.TipoAcabamento;

import java.util.UUID;

public record ChapaResponse(
        UUID uuid,
        Integer larguraMm,
        Integer alturaMm,
        Integer espessuraMm,
        TipoAcabamento tipoAcabamento,
        String material,
        Integer kerfMm,
        Integer margemBordaMm
) {
    public static ChapaResponse from(Chapa chapa) {
        return new ChapaResponse(
                chapa.getUuid(),
                chapa.getLarguraMm(),
                chapa.getAlturaMm(),
                chapa.getEspessuraMm(),
                chapa.getTipoAcabamento(),
                chapa.getMaterial(),
                chapa.getKerfMm(),
                chapa.getMargemBordaMm());
    }
}
