package com.cutflow.optimizer;

import java.util.List;

public record ChapaEmpacotada(
        int numeroChapa,
        List<PosicionamentoCalculado> posicionamentos,
        List<SobraCalculada> sobras,
        long areaUtilizadaMm2,
        long areaUtilMm2
) {
    public long areaDesperdicadaMm2() {
        return areaUtilMm2 - areaUtilizadaMm2;
    }

    public double percentualAproveitamento() {
        return areaUtilMm2 == 0 ? 0 : (areaUtilizadaMm2 * 100.0) / areaUtilMm2;
    }
}
