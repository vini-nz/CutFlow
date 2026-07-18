package com.cutflow.optimizer;

import java.util.List;

public record ResultadoOtimizacao(List<ChapaEmpacotada> chapas) {

    public int totalChapasUtilizadas() {
        return chapas.size();
    }

    public double percentualAproveitamentoMedio() {
        long areaUtil = chapas.stream().mapToLong(ChapaEmpacotada::areaUtilMm2).sum();
        long areaUtilizada = chapas.stream().mapToLong(ChapaEmpacotada::areaUtilizadaMm2).sum();
        return areaUtil == 0 ? 0 : (areaUtilizada * 100.0) / areaUtil;
    }

    public double percentualDesperdicioMedio() {
        return 100.0 - percentualAproveitamentoMedio();
    }
}
