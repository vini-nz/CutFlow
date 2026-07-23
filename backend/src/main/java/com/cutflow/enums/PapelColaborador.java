package com.cutflow.enums;

/**
 * Nivel de acesso de um colaborador direto a UM projeto especifico (ADR-0006)
 * - diferente de PapelMembro, que e' o papel dentro de uma Organizacao
 * inteira. Um colaborador nunca vira Membro da organizacao: ele so enxerga o
 * projeto para o qual foi convidado, nada mais dela.
 */
public enum PapelColaborador {
    EDITOR,
    VISUALIZADOR;

    public boolean podeEditar() {
        return this == EDITOR;
    }
}
