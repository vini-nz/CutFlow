package com.cutflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Retalho que sobrou de uma ChapaUtilizada ao final do encaixe - registrado
 * sempre, sem tamanho minimo, porque o marceneiro-piloto confirmou que
 * reaproveita sobra do mesmo projeto mesmo pequena (doc secao 3.3, ex: virar
 * frente de gaveta).
 *
 * sentidoVeio existe na modelagem para permitir, no futuro, casar uma sobra
 * com uma peca nova respeitando o veio na emenda - mas o proprio marceneiro
 * relatou que isso e raro na pratica dele, entao o MVP so registra a sobra;
 * quem decide se ela serve pra alguma peca e' o marceneiro, olhando o PDF/
 * visualizacao. Reaproveitamento automatico fica pra V2 (doc secao 3.3/6.3).
 */
@Entity
@Table(name = "sobras")
@Getter
@Setter
@NoArgsConstructor
public class Sobra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapa_utilizada_id", nullable = false)
    private ChapaUtilizada chapaUtilizada;

    @Column(name = "x_mm", nullable = false)
    private Integer xMm;

    @Column(name = "y_mm", nullable = false)
    private Integer yMm;

    @Column(name = "largura_mm", nullable = false)
    private Integer larguraMm;

    @Column(name = "altura_mm", nullable = false)
    private Integer alturaMm;

    // Nula no MVP (ver javadoc da classe) - reservado para V2.
    @Column(name = "sentido_veio", length = 20)
    private String sentidoVeio;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
