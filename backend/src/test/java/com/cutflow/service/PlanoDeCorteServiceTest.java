package com.cutflow.service;

import com.cutflow.entity.Chapa;
import com.cutflow.entity.Peca;
import com.cutflow.entity.PlanoDeCorte;
import com.cutflow.entity.Projeto;
import com.cutflow.enums.TipoAcabamento;
import com.cutflow.exception.BusinessRuleException;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.optimizer.OtimizadorDePlano;
import com.cutflow.optimizer.PecaParaEmpacotar;
import com.cutflow.optimizer.ResultadoOtimizacao;
import com.cutflow.repository.ChapaUtilizadaRepository;
import com.cutflow.repository.PecaRepository;
import com.cutflow.repository.PlanoDeCorteRepository;
import com.cutflow.repository.PosicionamentoRepository;
import com.cutflow.repository.SobraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre as regras de orquestracao do PlanoDeCorteService que nao pertencem
 * ao algoritmo em si (ja coberto por GuillotineOtimizadorDePlanoTest): casar
 * peca com a Chapa da mesma combinacao espessura+acabamento (auto-
 * provisionada via ChapaService, ADR-0003/ADR-0004), nunca misturar veio e
 * liso no mesmo grupo de empacotamento, e recusar gerar plano sem peca.
 */
@ExtendWith(MockitoExtension.class)
class PlanoDeCorteServiceTest {

    @Mock private ProjetoService projetoService;
    @Mock private PecaRepository pecaRepository;
    @Mock private ChapaService chapaService;
    @Mock private PlanoDeCorteRepository planoDeCorteRepository;
    @Mock private ChapaUtilizadaRepository chapaUtilizadaRepository;
    @Mock private PosicionamentoRepository posicionamentoRepository;
    @Mock private SobraRepository sobraRepository;
    @Mock private OtimizadorDePlano otimizadorDePlano;

    private PlanoDeCorteService planoDeCorteService;

    private static final Long PROJETO_ID = 1L;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        planoDeCorteService = new PlanoDeCorteService(
                projetoService, pecaRepository, chapaService, planoDeCorteRepository,
                chapaUtilizadaRepository, posicionamentoRepository, sobraRepository, otimizadorDePlano);

        projeto = new Projeto();
        projeto.setId(PROJETO_ID);
        projeto.setUuid(UUID.randomUUID());
        projeto.setNome("Armário Cozinha João");
    }

    private static long proximoId = 10L;

    private Peca peca(String nome, int espessuraMm, TipoAcabamento tipoAcabamento) {
        Peca peca = new Peca();
        peca.setId(proximoId++);
        peca.setUuid(UUID.randomUUID());
        peca.setProjeto(projeto);
        peca.setNome(nome);
        peca.setLarguraMm(500);
        peca.setAlturaMm(700);
        peca.setEspessuraMm(espessuraMm);
        peca.setQuantidade(1);
        peca.setTipoAcabamento(tipoAcabamento);
        return peca;
    }

    private Chapa chapa(int espessuraMm, TipoAcabamento tipoAcabamento) {
        Chapa chapa = new Chapa();
        chapa.setId(proximoId++);
        chapa.setUuid(UUID.randomUUID());
        chapa.setProjeto(projeto);
        chapa.setLarguraMm(1840);
        chapa.setAlturaMm(2740);
        chapa.setEspessuraMm(espessuraMm);
        chapa.setTipoAcabamento(tipoAcabamento);
        chapa.setKerfMm(4);
        chapa.setMargemBordaMm(6);
        return chapa;
    }

    @Test
    void gerar_semPecasCadastradas_lancaBusinessRuleException() {
        when(projetoService.findOrThrow(projeto.getUuid())).thenReturn(projeto);
        when(pecaRepository.findByProjetoIdOrderByCreatedAtAsc(PROJETO_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> planoDeCorteService.gerar(projeto.getUuid()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("peças");

        verify(planoDeCorteRepository, never()).save(any());
    }

    @Test
    void gerar_usaChapaAutoProvisionadaParaCombinacaoDaPeca() {
        // ADR-0003/ADR-0004: nao existe "sem chapa cadastrada" - a Chapa da
        // combinacao espessura+acabamento e' garantida (auto-provisionada,
        // se preciso) via ChapaService antes de rodar o otimizador.
        when(projetoService.findOrThrow(projeto.getUuid())).thenReturn(projeto);
        when(pecaRepository.findByProjetoIdOrderByCreatedAtAsc(PROJETO_ID))
                .thenReturn(List.of(peca("Prateleira", 18, TipoAcabamento.LISO)));
        when(chapaService.garantirChapa(eq(PROJETO_ID), eq(projeto), eq(18), eq(TipoAcabamento.LISO)))
                .thenReturn(chapa(18, TipoAcabamento.LISO));
        when(otimizadorDePlano.gerar(any(), any())).thenReturn(new ResultadoOtimizacao(List.of()));
        when(planoDeCorteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chapaUtilizadaRepository.findByPlanoDeCorteIdOrderByNumeroChapaAsc(any())).thenReturn(List.of());

        planoDeCorteService.gerar(projeto.getUuid());

        verify(chapaService).garantirChapa(PROJETO_ID, projeto, 18, TipoAcabamento.LISO);
        verify(otimizadorDePlano).gerar(any(), any());
    }

    @Test
    void gerar_pecaComVeioEPecaLisa_mesmaEspessura_nuncaCompartilhamChapa() {
        // Bug reportado apos a apresentacao: peca com veio e peca lisa de
        // mesma espessura eram encaixadas na MESMA chapa. O acabamento vem
        // de fabrica na chapa (ADR-0004), entao cada acabamento forma um
        // grupo proprio, empacotado numa chapa propria - o otimizador deve
        // ser chamado uma vez por combinacao, nunca com os dois misturados.
        Peca lisa = peca("Prateleira", 15, TipoAcabamento.LISO);
        Peca comVeio = peca("Porta", 15, TipoAcabamento.COM_VEIO);

        when(projetoService.findOrThrow(projeto.getUuid())).thenReturn(projeto);
        when(pecaRepository.findByProjetoIdOrderByCreatedAtAsc(PROJETO_ID)).thenReturn(List.of(lisa, comVeio));
        when(chapaService.garantirChapa(eq(PROJETO_ID), eq(projeto), eq(15), eq(TipoAcabamento.LISO)))
                .thenReturn(chapa(15, TipoAcabamento.LISO));
        when(chapaService.garantirChapa(eq(PROJETO_ID), eq(projeto), eq(15), eq(TipoAcabamento.COM_VEIO)))
                .thenReturn(chapa(15, TipoAcabamento.COM_VEIO));
        when(otimizadorDePlano.gerar(any(), any())).thenReturn(new ResultadoOtimizacao(List.of()));
        when(planoDeCorteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chapaUtilizadaRepository.findByPlanoDeCorteIdOrderByNumeroChapaAsc(any())).thenReturn(List.of());

        planoDeCorteService.gerar(projeto.getUuid());

        // Cada combinacao provisiona a propria chapa...
        verify(chapaService).garantirChapa(PROJETO_ID, projeto, 15, TipoAcabamento.LISO);
        verify(chapaService).garantirChapa(PROJETO_ID, projeto, 15, TipoAcabamento.COM_VEIO);

        // ...e o otimizador roda uma vez por grupo, cada grupo homogeneo no
        // acabamento (nunca LISO e COM_VEIO na mesma chamada).
        ArgumentCaptor<List<PecaParaEmpacotar>> captor = ArgumentCaptor.forClass(List.class);
        verify(otimizadorDePlano, times(2)).gerar(any(), captor.capture());
        for (List<PecaParaEmpacotar> grupo : captor.getAllValues()) {
            long acabamentosDistintos = grupo.stream().map(PecaParaEmpacotar::tipoAcabamento).distinct().count();
            assertThat(acabamentosDistintos).isEqualTo(1);
        }
    }

    @Test
    void obterUltimo_quandoNenhumPlanoGerado_lancaResourceNotFoundException() {
        when(projetoService.findOrThrow(projeto.getUuid())).thenReturn(projeto);
        when(planoDeCorteRepository.findFirstByProjetoIdOrderByGeradoEmDesc(PROJETO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planoDeCorteService.obterUltimo(projeto.getUuid()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obterUltimo_quandoPlanoExiste_retornaComListasVazias() {
        PlanoDeCorte plano = new PlanoDeCorte();
        plano.setId(5L);
        plano.setUuid(UUID.randomUUID());
        plano.setProjeto(projeto);
        plano.setTotalChapasUtilizadas(2);
        plano.setPercentualAproveitamento(new java.math.BigDecimal("87.50"));
        plano.setPercentualDesperdicio(new java.math.BigDecimal("12.50"));

        when(projetoService.findOrThrow(projeto.getUuid())).thenReturn(projeto);
        when(planoDeCorteRepository.findFirstByProjetoIdOrderByGeradoEmDesc(PROJETO_ID)).thenReturn(Optional.of(plano));
        when(chapaUtilizadaRepository.findByPlanoDeCorteIdOrderByNumeroChapaAsc(5L)).thenReturn(List.of());

        var response = planoDeCorteService.obterUltimo(projeto.getUuid());

        assertThat(response.totalChapasUtilizadas()).isEqualTo(2);
        assertThat(response.chapas()).isEmpty();
    }
}
