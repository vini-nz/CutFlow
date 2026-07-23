package com.cutflow.dto.colaborador;

import com.cutflow.enums.PapelColaborador;

/**
 * Visao publica de um convite (nao exige login) - o que a tela de aceite
 * mostra ANTES de o usuario logar: "Fulano te convidou para o projeto X".
 */
public record ConviteDetalhesResponse(
        String nomeProjeto,
        String convidadoPor,
        PapelColaborador papel,
        String emailAlvo,
        boolean valido
) {}
