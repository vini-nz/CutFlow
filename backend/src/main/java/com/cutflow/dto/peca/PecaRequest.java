package com.cutflow.dto.peca;

import com.cutflow.enums.TipoAcabamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PecaRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotNull @Positive Integer alturaMm,
        @NotNull @Positive Integer larguraMm,
        @NotNull Integer espessuraMm,
        @NotNull @Positive Integer quantidade,
        @NotNull TipoAcabamento tipoAcabamento
) {}
