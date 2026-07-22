package com.cutflow.dto.organizacao;

import com.cutflow.enums.PapelMembro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Adiciona a equipe alguem que JA tem conta no CutFlow (identificado por
 * e-mail). papel opcional (default MEMBRO); OWNER nao e' aceito por convite -
 * so existe um dono, definido na criacao da organizacao.
 */
public record MembroRequest(
        @NotBlank @Email String email,
        PapelMembro papel
) {}
