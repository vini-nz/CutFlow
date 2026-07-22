package com.cutflow.enums;

/**
 * Papel de um Usuario dentro de uma Organizacao (ADR-0005). Modelo simples de
 * tres niveis, suficiente para o cenario do marceneiro-piloto (dono + equipe):
 *
 * - OWNER: dono da organizacao. Pode tudo, inclusive gerenciar membros e a
 *   propria organizacao. Toda organizacao tem exatamente um OWNER (quem a
 *   criou); nao pode ser removido.
 * - ADMIN: pode gerenciar membros e projetos, mas nao mexe na organizacao em
 *   si nem remove o OWNER.
 * - MEMBRO: acessa e edita os projetos da organizacao, sem gerenciar membros.
 *
 * A hierarquia e' por ordinal: nivel mais alto (menor ordinal) inclui os
 * poderes dos mais baixos - ver podeGerenciarMembros().
 */
public enum PapelMembro {
    OWNER,
    ADMIN,
    MEMBRO;

    /** OWNER e ADMIN podem convidar/remover membros; MEMBRO nao. */
    public boolean podeGerenciarMembros() {
        return this == OWNER || this == ADMIN;
    }
}
