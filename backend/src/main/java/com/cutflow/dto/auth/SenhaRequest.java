package com.cutflow.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca de senha. senhaAtual é opcional: obrigatória para quem já tem senha
 * local; para conta só-Google (sem senha), permite DEFINIR a primeira senha
 * sem exigir a atual.
 */
public record SenhaRequest(
        String senhaAtual,
        @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter ao menos 8 caracteres") String novaSenha
) {}
