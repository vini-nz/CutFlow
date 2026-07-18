package com.cutflow.dto.planodecorte;

import com.cutflow.entity.Posicionamento;

import java.util.UUID;

public record PosicionamentoResponse(
        UUID pecaUuid,
        String nomePeca,
        Integer numeroEtiqueta,
        Integer xMm,
        Integer yMm,
        Integer larguraMm,
        Integer alturaMm,
        Boolean rotacionada
) {
    public static PosicionamentoResponse from(Posicionamento p) {
        return new PosicionamentoResponse(
                p.getPeca().getUuid(),
                p.getPeca().getNome(),
                p.getNumeroEtiqueta(),
                p.getXMm(),
                p.getYMm(),
                p.getLarguraMm(),
                p.getAlturaMm(),
                p.getRotacionada());
    }
}
