package com.cutflow.dto.colaborador;

import com.cutflow.enums.PapelColaborador;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/**
 * email e' opcional de proposito (ADR-0006): preenchido = convite direcionado
 * de uso unico para aquela pessoa; vazio = link reutilizavel que qualquer
 * pessoa logada pode aceitar (estilo Canva).
 */
public record ConviteRequest(
        @Email String email,
        @NotNull PapelColaborador papel
) {}
