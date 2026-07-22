package com.cutflow.dto.auth;

import com.cutflow.entity.Usuario;

import java.util.UUID;

public record UsuarioResponse(
        UUID uuid,
        String nome,
        String email
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getUuid(), usuario.getNome(), usuario.getEmail());
    }
}
