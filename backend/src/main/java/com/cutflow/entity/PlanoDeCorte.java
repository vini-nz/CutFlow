package com.cutflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um "rodar plano de corte" completo para o projeto: pode abranger pecas de
 * varias espessuras (cada uma casada com a Chapa de mesma espessura, ver
 * PlanoDeCorteService), agregadas aqui pelos totais que a UX prioriza -
 * numero de chapas e' o dado mais visivel do resultado (doc secao 0).
 * Cada geracao cria um novo registro (sem versionamento/historico no MVP,
 * ver docs/architecture.md).
 */
@Entity
@Table(name = "planos_de_corte")
@Getter
@Setter
@NoArgsConstructor
public class PlanoDeCorte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Column(name = "total_chapas_utilizadas", nullable = false)
    private Integer totalChapasUtilizadas;

    @Column(name = "percentual_aproveitamento", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualAproveitamento;

    @Column(name = "percentual_desperdicio", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualDesperdicio;

    @Column(name = "gerado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime geradoEm;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
