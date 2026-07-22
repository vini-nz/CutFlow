package com.cutflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Pessoa que faz login no CutFlow (ADR-0005). Um Usuario e' global (nao
 * pertence a uma organizacao): a relacao com organizacoes e' feita por Membro,
 * permitindo que a mesma pessoa participe de varias marcenarias e alterne
 * entre elas por um "workspace switcher".
 *
 * Autentica de duas formas, nao exclusivas:
 * - local: e-mail + senha (senhaHash preenchido, BCrypt);
 * - Google: OIDC (googleSub = "sub" do token do Google).
 * Um mesmo e-mail e' uma unica conta: quem cadastrou com senha e depois entra
 * com Google (mesmo e-mail) tem o googleSub vinculado a conta existente.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    // Nulo para contas que so entram via Google (nunca definiram senha local).
    @Column(name = "senha_hash", length = 100)
    private String senhaHash;

    // "sub" do OIDC do Google; nulo enquanto a conta nunca entrou via Google.
    @Column(name = "google_sub", unique = true, length = 100)
    private String googleSub;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
