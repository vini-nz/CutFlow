package com.cutflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma marcenaria/empresa (ADR-0005) OU o espaco pessoal de um usuario
 * (ADR-0006). E' o tenant do sistema: todo Projeto pertence a uma Organizacao,
 * e um Usuario so enxerga projetos das organizacoes das quais e' Membro.
 *
 * pessoal = true e' o espaco criado automaticamente no cadastro (1 membro, o
 * proprio dono): remove a friccao de "criar empresa" antes do primeiro
 * projeto para quem trabalha sozinho (o MEI). O frontend esconde a gestao de
 * equipe desses espacos. O "documento" (CNPJ) e' opcional - o cadastro nao
 * deve travar quem ainda nao quer informar dados fiscais.
 */
@Entity
@Table(name = "organizacoes")
@Getter
@Setter
@NoArgsConstructor
public class Organizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, length = 150)
    private String nome;

    // Espaco pessoal (criado no cadastro) x organizacao de verdade (criada
    // manualmente por quem tem equipe). Ver ADR-0006.
    @Column(nullable = false)
    private boolean pessoal = false;

    // Opcional (CNPJ ou outro identificador fiscal); nao e' chave de login.
    @Column(length = 30)
    private String documento;

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
