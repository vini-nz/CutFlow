package com.cutflow.dto.chapa;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChapaRequest(
        @NotNull @Positive Integer larguraMm,
        @NotNull @Positive Integer alturaMm,
        @NotNull Integer espessuraMm,
        @NotNull @Min(0) Integer quantidadeDisponivel,
        @Positive Integer kerfMm,
        @Min(0) Integer margemBordaMm
) {}
