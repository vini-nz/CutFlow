package com.cutflow.optimizer;

/**
 * Especificacao da chapa usada para uma rodada de empacotamento - espelha
 * Chapa (entity), mas o otimizador nao depende de JPA (doc secao 5.2,
 * "isolamento do algoritmo").
 */
public record ParametrosChapa(
        int larguraMm,
        int alturaMm,
        int kerfMm,
        int margemBordaMm
) {}
