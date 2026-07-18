package com.cutflow.dto.planodecorte;

import com.cutflow.entity.ChapaUtilizada;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ChapaUtilizadaResponse(
        UUID uuid,
        Integer numeroChapa,
        Integer larguraMm,
        Integer alturaMm,
        Integer espessuraMm,
        Long areaDesperdicadaMm2,
        BigDecimal percentualAproveitamento,
        List<PosicionamentoResponse> posicionamentos,
        List<SobraResponse> sobras
) {
    public static ChapaUtilizadaResponse from(ChapaUtilizada cu, List<PosicionamentoResponse> posicionamentos,
                                               List<SobraResponse> sobras) {
        return new ChapaUtilizadaResponse(
                cu.getUuid(),
                cu.getNumeroChapa(),
                cu.getChapa().getLarguraMm(),
                cu.getChapa().getAlturaMm(),
                cu.getChapa().getEspessuraMm(),
                cu.getAreaDesperdicadaMm2(),
                cu.getPercentualAproveitamento(),
                posicionamentos,
                sobras);
    }
}
