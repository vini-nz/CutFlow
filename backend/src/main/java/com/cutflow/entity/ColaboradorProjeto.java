package com.cutflow.entity;

import com.cutflow.enums.PapelColaborador;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Acesso direto de um Usuario a UM Projeto especifico (ADR-0006), independente
 * de organizacao - o caminho "Joaquim compartilha o plano com o Carlos".
 * Diferente de Membro (que da acesso a toda uma organizacao): o colaborador
 * so enxerga este projeto. O papel (EDITOR/VISUALIZADOR) define se pode editar
 * ou so visualizar. Unico por (projeto, usuario) - ver constraint no DDL.
 */
@Entity
@Table(name = "colaboradores_projeto")
@Getter
@Setter
@NoArgsConstructor
public class ColaboradorProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelColaborador papel = PapelColaborador.VISUALIZADOR;

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
