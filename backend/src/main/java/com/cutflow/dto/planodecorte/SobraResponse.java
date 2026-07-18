package com.cutflow.dto.planodecorte;

import com.cutflow.entity.Sobra;

public record SobraResponse(
        Integer xMm,
        Integer yMm,
        Integer larguraMm,
        Integer alturaMm
) {
    public static SobraResponse from(Sobra s) {
        return new SobraResponse(s.getXMm(), s.getYMm(), s.getLarguraMm(), s.getAlturaMm());
    }
}
