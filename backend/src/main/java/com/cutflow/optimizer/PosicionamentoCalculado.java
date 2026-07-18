package com.cutflow.optimizer;

public record PosicionamentoCalculado(
        Long pecaId,
        String nomePeca,
        int numeroEtiqueta,
        int xMm,
        int yMm,
        int larguraMm,
        int alturaMm,
        boolean rotacionada
) {}
