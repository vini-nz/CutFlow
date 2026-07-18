package com.cutflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Onde uma unidade de Peca foi posicionada dentro de uma ChapaUtilizada.
 * numeroEtiqueta e a numeracao simples exibida na visualizacao e no PDF -
 * o marceneiro-piloto ja numera as pecas hoje (doc secao 3.1), so nao usa
 * QR Code ainda (isso fica pra V2).
 */
@Entity
@Table(name = "posicionamentos")
@Getter
@Setter
@NoArgsConstructor
public class Posicionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapa_utilizada_id", nullable = false)
    private ChapaUtilizada chapaUtilizada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Column(name = "numero_etiqueta", nullable = false)
    private Integer numeroEtiqueta;

    @Column(name = "x_mm", nullable = false)
    private Integer xMm;

    @Column(name = "y_mm", nullable = false)
    private Integer yMm;

    @Column(name = "largura_mm", nullable = false)
    private Integer larguraMm;

    @Column(name = "altura_mm", nullable = false)
    private Integer alturaMm;

    @Column(nullable = false)
    private Boolean rotacionada = false;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
