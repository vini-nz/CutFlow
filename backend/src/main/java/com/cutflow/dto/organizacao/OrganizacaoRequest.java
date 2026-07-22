package com.cutflow.dto.organizacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizacaoRequest(
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 30) String documento
) {}
