package com.cutflow.dto.planodecorte;

import com.cutflow.entity.PlanoDeCorte;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PlanoDeCorteResponse(
        UUID uuid,
        Integer totalChapasUtilizadas,
        BigDecimal percentualAproveitamento,
        BigDecimal percentualDesperdicio,
        OffsetDateTime geradoEm,
        List<ChapaUtilizadaResponse> chapas
) {
    public static PlanoDeCorteResponse from(PlanoDeCorte plano, List<ChapaUtilizadaResponse> chapas) {
        return new PlanoDeCorteResponse(
                plano.getUuid(),
                plano.getTotalChapasUtilizadas(),
                plano.getPercentualAproveitamento(),
                plano.getPercentualDesperdicio(),
                plano.getGeradoEm(),
                chapas);
    }
}
