package com.cutflow.service;

import com.cutflow.entity.Chapa;
import com.cutflow.entity.Peca;
import com.cutflow.entity.PlanoDeCorte;
import com.cutflow.entity.Projeto;
import com.cutflow.enums.TipoAcabamento;
import com.cutflow.exception.BusinessRuleException;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.optimizer.OtimizadorDePlano;
import com.cutflow.repository.ChapaRepository;
import com.cutflow.repository.ChapaUtilizadaRepository;
import com.cutflow.repository.PecaRepository;
import com.cutflow.repository.PlanoDeCorteRepository;
import com.cutflow.repository.PosicionamentoRepository;
import com.cutflow.repository.SobraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre as regras de orquestracao do PlanoDeCorteService que nao pertencem
 * ao algoritmo em si (ja coberto por GuillotineOtimizadorDePlanoTest): casar
 * peca com chapa da mesma espessura, e recusar gerar plano sem peca
 * cadastrada.
 */
@ExtendWith(MockitoExtension.class)
class PlanoDeCorteServiceTest {

    @Mock private ProjetoService projetoService;
    @Mock private PecaRepository pecaRepository;
    @Mock private ChapaRepository chapaRepository;
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
                projetoService, pecaRepository, chapaRepository, planoDeCorteRepository,
                chapaUtilizadaRepository, posicionamentoRepository, sobraRepository, otimizadorDePlano);

        projeto = new Projeto();
        projeto.setId(PROJETO_ID);
        projeto.setUuid(UUID.randomUUID());
        projeto.setNome("Armário Cozinha João");
    }

    private Peca peca(String nome, int espessuraMm) {
        Peca peca = new Peca();
        peca.setId(10L);
        peca.setUuid(UUID.randomUUID());
        peca.setProjeto(projeto);
        peca.setNome(nome);
        peca.setLarguraMm(500);
        peca.setAlturaMm(700);
        peca.setEspessuraMm(espessuraMm);
        peca.setQuantidade(1);
        peca.setTipoAcabamento(TipoAcabamento.LISO);
        return peca;
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
    void gerar_semChapaCadastradaParaEspessuraDaPeca_lancaBusinessRuleException() {
        when(projetoService.findOrThrow(projeto.getUuid())).thenReturn(projeto);
        when(pecaRepository.findByProjetoIdOrderByCreatedAtAsc(PROJETO_ID)).thenReturn(List.of(peca("Prateleira", 18)));
        when(chapaRepository.findByProjetoIdAndEspessuraMm(PROJETO_ID, 18)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planoDeCorteService.gerar(projeto.getUuid()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("18mm");

        verify(otimizadorDePlano, never()).gerar(any(), any());
        verify(planoDeCorteRepository, never()).save(any());
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
