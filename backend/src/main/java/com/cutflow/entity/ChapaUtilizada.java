package com.cutflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uma chapa fisica dentro de um PlanoDeCorte (o "Plano" da modelagem original,
 * doc secao 4.2 - renomeado para nao colidir com o agregado PlanoDeCorte).
 * numeroChapa e sequencial dentro do plano inteiro, na ordem em que a chapa
 * foi aberta pelo algoritmo.
 */
@Entity
@Table(name = "chapas_utilizadas")
@Getter
@Setter
@NoArgsConstructor
public class ChapaUtilizada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_de_corte_id", nullable = false)
    private PlanoDeCorte planoDeCorte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapa_id", nullable = false)
    private Chapa chapa;

    @Column(name = "numero_chapa", nullable = false)
    private Integer numeroChapa;

    @Column(name = "area_desperdicada_mm2", nullable = false)
    private Long areaDesperdicadaMm2;

    @Column(name = "percentual_aproveitamento", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualAproveitamento;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
