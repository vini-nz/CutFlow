package com.cutflow.optimizer;

import com.cutflow.enums.TipoAcabamento;
import com.cutflow.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O algoritmo de encaixe e' a parte de maior risco do MVP (doc secao 9,
 * "Plano de Testes"): um bug aqui gera um plano fisicamente impossivel de
 * cortar. Os testes focam nas duas invariantes que nunca podem quebrar -
 * pecas nao se sobrepoem e nenhuma extrapola os limites da chapa - mais as
 * regras de negocio confirmadas na entrevista (peca comprida primeiro, veio
 * nao gira, chapas insuficientes e' erro).
 */
class GuillotineOtimizadorDePlanoTest {

    private final GuillotineOtimizadorDePlano otimizador = new GuillotineOtimizadorDePlano();

    // Caso real do doc (introducao): armario de cozinha do marceneiro-piloto.
    private static final ParametrosChapa CHAPA_PADRAO = new ParametrosChapa(1840, 2740, 4, 6);

    @Test
    void gerar_casoRealArmarioCozinha_naoSobrepoeENaoExtrapolaLimites() {
        List<PecaParaEmpacotar> pecas = List.of(
                new PecaParaEmpacotar(1L, "Lateral", 550, 2200, 2, TipoAcabamento.COM_VEIO),
                new PecaParaEmpacotar(2L, "Fundo", 550, 700, 1, TipoAcabamento.LISO),
                new PecaParaEmpacotar(3L, "Tampa", 550, 700, 1, TipoAcabamento.LISO),
                new PecaParaEmpacotar(4L, "Prateleira", 500, 668, 5, TipoAcabamento.LISO),
                new PecaParaEmpacotar(5L, "Porta", 350, 2200, 2, TipoAcabamento.COM_VEIO)
        );

        ResultadoOtimizacao resultado = otimizador.gerar(CHAPA_PADRAO, pecas);

        int totalUnidades = pecas.stream().mapToInt(PecaParaEmpacotar::quantidade).sum();
        long totalPosicionadas = resultado.chapas().stream().mapToLong(c -> c.posicionamentos().size()).sum();
        assertThat(totalPosicionadas).isEqualTo(totalUnidades);

        for (ChapaEmpacotada chapa : resultado.chapas()) {
            assertNenhumaExtrapolaLimites(chapa, CHAPA_PADRAO);
            assertNenhumaSobreposicao(chapa);
        }
        assertThat(resultado.percentualAproveitamentoMedio()).isBetween(0.0, 100.0);
    }

    @Test
    void gerar_pecaComVeio_nuncaAparecePosicionadaRotacionada() {
        List<PecaParaEmpacotar> pecas = List.of(
                new PecaParaEmpacotar(1L, "Porta", 350, 2200, 4, TipoAcabamento.COM_VEIO)
        );

        ResultadoOtimizacao resultado = otimizador.gerar(CHAPA_PADRAO, pecas);

        boolean algumaRotacionada = resultado.chapas().stream()
                .flatMap(c -> c.posicionamentos().stream())
                .anyMatch(PosicionamentoCalculado::rotacionada);
        assertThat(algumaRotacionada).isFalse();
    }

    @Test
    void gerar_pecasMaisCompridasSaoNumeradasPrimeiro() {
        List<PecaParaEmpacotar> pecas = List.of(
                new PecaParaEmpacotar(1L, "Curta", 300, 300, 1, TipoAcabamento.LISO),
                new PecaParaEmpacotar(2L, "Comprida", 400, 2000, 1, TipoAcabamento.LISO)
        );

        ResultadoOtimizacao resultado = otimizador.gerar(CHAPA_PADRAO, pecas);

        PosicionamentoCalculado primeira = resultado.chapas().get(0).posicionamentos().stream()
                .min((a, b) -> Integer.compare(a.numeroEtiqueta(), b.numeroEtiqueta()))
                .orElseThrow();
        assertThat(primeira.nomePeca()).isEqualTo("Comprida");
    }

    @Test
    void gerar_quandoPecasExcedemUmaChapa_abreQuantasChapasForemNecessarias() {
        // ADR-0003: nao ha mais teto de "chapas disponiveis" - o otimizador
        // sempre abre chapas ate encaixar tudo. Area util por chapa ~=
        // (1840-12)*(2740-12) ~= 4,97 m2; cada peca usa 1840x1360mm (quase
        // metade da chapa), entao 6 pecas exigem 3+ chapas.
        List<PecaParaEmpacotar> pecas = List.of(
                new PecaParaEmpacotar(1L, "Painel", 1800, 1300, 6, TipoAcabamento.LISO)
        );

        ResultadoOtimizacao resultado = otimizador.gerar(CHAPA_PADRAO, pecas);

        assertThat(resultado.chapas().size()).isGreaterThanOrEqualTo(3);
        assertThat(resultado.chapas().stream()
                .flatMap(c -> c.posicionamentos().stream())
                .count()).isEqualTo(6);
    }

    @Test
    void gerar_quandoPecaMaiorQueAreaUtilDaChapa_lancaBusinessRuleException() {
        List<PecaParaEmpacotar> pecas = List.of(
                new PecaParaEmpacotar(1L, "Painel gigante", 2000, 3000, 1, TipoAcabamento.LISO)
        );

        assertThatThrownBy(() -> otimizador.gerar(CHAPA_PADRAO, pecas))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não cabe");
    }

    @Test
    void gerar_semPecas_lancaBusinessRuleException() {
        assertThatThrownBy(() -> otimizador.gerar(CHAPA_PADRAO, List.of()))
                .isInstanceOf(BusinessRuleException.class);
    }

    private void assertNenhumaExtrapolaLimites(ChapaEmpacotada chapa, ParametrosChapa params) {
        int larguraUtil = params.larguraMm() - 2 * params.margemBordaMm();
        int alturaUtil = params.alturaMm() - 2 * params.margemBordaMm();
        for (PosicionamentoCalculado p : chapa.posicionamentos()) {
            assertThat(p.xMm()).isGreaterThanOrEqualTo(params.margemBordaMm());
            assertThat(p.yMm()).isGreaterThanOrEqualTo(params.margemBordaMm());
            assertThat(p.xMm() - params.margemBordaMm() + p.larguraMm()).isLessThanOrEqualTo(larguraUtil);
            assertThat(p.yMm() - params.margemBordaMm() + p.alturaMm()).isLessThanOrEqualTo(alturaUtil);
        }
    }

    private void assertNenhumaSobreposicao(ChapaEmpacotada chapa) {
        List<PosicionamentoCalculado> posicionamentos = chapa.posicionamentos();
        for (int i = 0; i < posicionamentos.size(); i++) {
            for (int j = i + 1; j < posicionamentos.size(); j++) {
                assertThat(sobrepoe(posicionamentos.get(i), posicionamentos.get(j)))
                        .as("posicionamento %s sobrepoe %s", posicionamentos.get(i), posicionamentos.get(j))
                        .isFalse();
            }
        }
    }

    private boolean sobrepoe(PosicionamentoCalculado a, PosicionamentoCalculado b) {
        boolean separadosNoEixoX = a.xMm() + a.larguraMm() <= b.xMm() || b.xMm() + b.larguraMm() <= a.xMm();
        boolean separadosNoEixoY = a.yMm() + a.alturaMm() <= b.yMm() || b.yMm() + b.alturaMm() <= a.yMm();
        return !(separadosNoEixoX || separadosNoEixoY);
    }
}
