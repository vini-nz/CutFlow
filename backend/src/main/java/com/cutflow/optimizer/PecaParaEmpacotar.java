package com.cutflow.optimizer;

import com.cutflow.enums.TipoAcabamento;

/**
 * Entrada do otimizador para uma Peca (ja com a quantidade agregada - o
 * otimizador expande as unidades internamente).
 */
public record PecaParaEmpacotar(
        Long pecaId,
        String nome,
        int larguraMm,
        int alturaMm,
        int quantidade,
        TipoAcabamento tipoAcabamento
) {}
