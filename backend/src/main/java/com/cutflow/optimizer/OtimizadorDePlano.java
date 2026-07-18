package com.cutflow.optimizer;

import java.util.List;

/**
 * Fronteira entre o dominio (Chapa/Peca/PlanoDeCorte) e o algoritmo de
 * empacotamento (doc secao 5.2). Permite trocar a implementacao (Guillotine
 * hoje, MaxRects/Skyline no futuro se o aproveitamento nao bastar) sem tocar
 * em service, controller ou persistencia.
 */
public interface OtimizadorDePlano {
    ResultadoOtimizacao gerar(ParametrosChapa chapa, List<PecaParaEmpacotar> pecas);
}
