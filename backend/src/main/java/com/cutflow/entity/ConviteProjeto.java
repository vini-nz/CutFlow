package com.cutflow.entity;

import com.cutflow.enums.PapelColaborador;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Convite para colaborar num projeto (ADR-0006). O proprio uuid e' o token da
 * URL de aceite (/convite/{uuid}). Cobre DOIS modos, distinguidos por
 * emailAlvo:
 *
 * - emailAlvo = null  -> LINK REUTILIZAVEL (estilo Canva): qualquer pessoa
 *   logada que abrir o link entra como colaborador; vale ate ser revogado
 *   (nunca e' "consumido" - varias pessoas podem usar o mesmo link);
 * - emailAlvo preenchido -> CONVITE DIRECIONADO: so aquele e-mail pode aceitar
 *   e o convite e' de uso unico (marca aceitoEm ao ser aceito).
 *
 * Essa distincao corrige a incoerencia de tratar todo convite como uso unico,
 * o que quebraria o "link que varios podem usar".
 */
@Entity
@Table(name = "convites_projeto")
@Getter
@Setter
@NoArgsConstructor
public class ConviteProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelColaborador papel = PapelColaborador.VISUALIZADOR;

    // Null = link reutilizavel; preenchido = convite de uso unico para o e-mail.
    @Column(name = "email_alvo", length = 180)
    private String emailAlvo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aceito_por_id")
    private Usuario aceitoPor;

    @Column(name = "aceito_em")
    private OffsetDateTime aceitoEm;

    @Column(nullable = false)
    private boolean revogado = false;

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

    /** Link reutilizavel (varios podem aceitar) vs convite de uso unico. */
    public boolean reutilizavel() {
        return emailAlvo == null;
    }

    /**
     * Convite ainda pode ser aceito? Revogado nunca. Link reutilizavel vale
     * enquanto nao revogado. Convite direcionado vale ate ser aceito uma vez.
     */
    public boolean valido() {
        if (revogado) {
            return false;
        }
        return reutilizavel() || aceitoEm == null;
    }
}
