package com.cutflow.dto.projeto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjetoRequest(
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 150) String cliente
) {}
