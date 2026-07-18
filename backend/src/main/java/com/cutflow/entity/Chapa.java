package com.cutflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Especificacao de chapa disponivel para um projeto (uma por espessura - ver
 * constraint uq_chapas_projeto_espessura no DDL). Mistura catalogo (medida
 * padrao) com estoque (quantidadeDisponivel) de proposito: o marceneiro-piloto
 * compra chapa por projeto, nao mantem estoque parado (doc secao 4.3,
 * "Observacao critica"). Se o uso de sobras entre projetos crescer, separar
 * Material (catalogo) de Chapa (estoque) fica candidato a V2.
 */
@Entity
@Table(name = "chapas")
@Getter
@Setter
@NoArgsConstructor
public class Chapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Column(name = "largura_mm", nullable = false)
    private Integer larguraMm;

    @Column(name = "altura_mm", nullable = false)
    private Integer alturaMm;

    @Column(name = "espessura_mm", nullable = false)
    private Integer espessuraMm;

    @Column(nullable = false, length = 30)
    private String material = "MDF";

    @Column(name = "quantidade_disponivel", nullable = false)
    private Integer quantidadeDisponivel;

    // Confirmado na entrevista (doc secao 3.2): 4mm e o padrao real perdido
    // pela esquadrejadeira entre dois cortes; fica configuravel porque o
    // proprio marceneiro pode ajustar se perceber divergencia na pratica.
    @Column(name = "kerf_mm", nullable = false)
    private Integer kerfMm = 4;

    // Confirmado na entrevista: margem de seguranca de 0,5 a 0,7cm nas bordas
    // da chapa; 6mm fica como meio-termo padrao, tambem configuravel.
    @Column(name = "margem_borda_mm", nullable = false)
    private Integer margemBordaMm = 6;

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
