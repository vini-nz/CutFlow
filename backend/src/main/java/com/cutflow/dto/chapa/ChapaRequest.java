package com.cutflow.dto.chapa;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Usado apenas para editar uma Chapa ja existente (auto-provisionada - ver
 * ChapaService.garantirChapa). Nao ha campo de quantidade disponivel
 * (ADR-0003) nem de tipo de acabamento: o acabamento identifica a chapa
 * junto com a espessura (ADR-0004) e nao e' editavel.
 */
public record ChapaRequest(
        @NotNull @Positive Integer larguraMm,
        @NotNull @Positive Integer alturaMm,
        @NotNull Integer espessuraMm,
        @Positive Integer kerfMm,
        @Min(0) Integer margemBordaMm
) {}
