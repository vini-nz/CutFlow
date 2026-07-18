package com.cutflow.entity;

import com.cutflow.enums.TipoAcabamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Especificacao de chapa usada num projeto - uma por combinacao de espessura
 * e tipo de acabamento (ver constraint uq_chapas_projeto_espessura_acabamento
 * no DDL). Puramente catalogo (medida, kerf, margem) - nao ha nocao de
 * estoque/quantidade disponivel (ver ADR-0003).
 *
 * O acabamento (LISO / COM_VEIO) e' uma caracteristica FISICA da chapa que ja
 * vem de fabrica, nao algo aplicado depois (ADR-0004): uma peca com veio so
 * pode sair de uma chapa com veio, e uma peca lisa de uma chapa lisa. Por
 * isso o plano de corte nunca mistura acabamentos na mesma chapa, assim como
 * nunca mistura espessuras.
 *
 * E' auto-provisionada com valores padrao (ver DEFAULT_* em ChapaService) na
 * primeira vez que uma Peca da combinacao espessura+acabamento e' criada - o
 * usuario nao precisa cadastrar uma Chapa antes de gerar o plano. A listagem
 * continua visivel na UI, com "Editar" (largura/altura/kerf/margem) e
 * "Excluir" (apenas quando nenhuma peca da combinacao existir mais).
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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acabamento", nullable = false, length = 20)
    private TipoAcabamento tipoAcabamento = TipoAcabamento.LISO;

    @Column(nullable = false, length = 30)
    private String material = "MDF";

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
