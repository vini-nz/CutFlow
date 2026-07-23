package com.cutflow.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edição dos dados básicos do próprio usuário (nome e e-mail). */
public record PerfilRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 180) String email
) {}
