package com.cutflow.service;

import com.cutflow.dto.planodecorte.ChapaUtilizadaResponse;
import com.cutflow.dto.planodecorte.PlanoDeCorteResponse;
import com.cutflow.dto.planodecorte.PosicionamentoResponse;
import com.cutflow.dto.planodecorte.SobraResponse;
import com.cutflow.entity.Chapa;
import com.cutflow.entity.ChapaUtilizada;
import com.cutflow.entity.Peca;
import com.cutflow.entity.PlanoDeCorte;
import com.cutflow.entity.Posicionamento;
import com.cutflow.entity.Projeto;
import com.cutflow.entity.Sobra;
import com.cutflow.exception.BusinessRuleException;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.optimizer.ChapaEmpacotada;
import com.cutflow.optimizer.OtimizadorDePlano;
import com.cutflow.optimizer.ParametrosChapa;
import com.cutflow.optimizer.PecaParaEmpacotar;
import com.cutflow.optimizer.PosicionamentoCalculado;
import com.cutflow.optimizer.ResultadoOtimizacao;
import com.cutflow.optimizer.SobraCalculada;
import com.cutflow.repository.ChapaRepository;
import com.cutflow.repository.ChapaUtilizadaRepository;
import com.cutflow.repository.PecaRepository;
import com.cutflow.repository.PlanoDeCorteRepository;
import com.cutflow.repository.PosicionamentoRepository;
import com.cutflow.repository.SobraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquestra a geracao do plano de corte: agrupa as pecas do projeto por
 * espessura (regra confirmada na entrevista - plano nunca mistura pecas de
 * espessuras diferentes numa mesma chapa, doc secao 3.1), casa cada grupo
 * com a Chapa de mesma espessura, delega o encaixe ao OtimizadorDePlano e
 * persiste o resultado.
 *
 * Cada chamada a gerar() cria um PlanoDeCorte novo (sem versionamento/
 * comparacao entre planos no MVP - ver docs/architecture.md).
 */
@Service
@RequiredArgsConstructor
public class PlanoDeCorteService {

    private final ProjetoService projetoService;
    private final PecaRepository pecaRepository;
    private final ChapaRepository chapaRepository;
    private final PlanoDeCorteRepository planoDeCorteRepository;
    private final ChapaUtilizadaRepository chapaUtilizadaRepository;
    private final PosicionamentoRepository posicionamentoRepository;
    private final SobraRepository sobraRepository;
    private final OtimizadorDePlano otimizadorDePlano;

    @Transactional
    public PlanoDeCorteResponse gerar(UUID projetoUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        List<Peca> pecas = pecaRepository.findByProjetoIdOrderByCreatedAtAsc(projeto.getId());
        if (pecas.isEmpty()) {
            throw new BusinessRuleException("Projeto não tem peças cadastradas");
        }

        Map<Long, Peca> pecaPorId = pecas.stream().collect(Collectors.toMap(Peca::getId, p -> p));
        Map<Integer, List<Peca>> pecasPorEspessura = pecas.stream()
                .collect(Collectors.groupingBy(Peca::getEspessuraMm));

        List<GrupoEmpacotado> grupos = new ArrayList<>();
        for (Map.Entry<Integer, List<Peca>> entry : pecasPorEspessura.entrySet()) {
            grupos.add(empacotarGrupo(projeto.getId(), entry.getKey(), entry.getValue()));
        }

        PlanoDeCorte plano = new PlanoDeCorte();
        plano.setProjeto(projeto);
        preencherTotais(plano, grupos);
        PlanoDeCorte planoSalvo = planoDeCorteRepository.save(plano);

        persistirGrupos(planoSalvo, grupos, pecaPorId);

        return montarResposta(planoSalvo);
    }

    @Transactional(readOnly = true)
    public PlanoDeCorteResponse obterUltimo(UUID projetoUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        PlanoDeCorte plano = planoDeCorteRepository.findFirstByProjetoIdOrderByGeradoEmDesc(projeto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum plano de corte gerado para este projeto ainda"));
        return montarResposta(plano);
    }

    @Transactional(readOnly = true)
    public PlanoObtidoParaPdf obterUltimoParaPdf(UUID projetoUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        PlanoDeCorte plano = planoDeCorteRepository.findFirstByProjetoIdOrderByGeradoEmDesc(projeto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum plano de corte gerado para este projeto ainda"));
        return new PlanoObtidoParaPdf(projeto, montarResposta(plano));
    }

    private GrupoEmpacotado empacotarGrupo(Long projetoId, Integer espessuraMm, List<Peca> pecasDoGrupo) {
        Chapa chapa = chapaRepository.findByProjetoIdAndEspessuraMm(projetoId, espessuraMm)
                .orElseThrow(() -> new BusinessRuleException(
                        "Não há chapa de %dmm cadastrada, mas o projeto tem peças dessa espessura".formatted(espessuraMm)));

        List<PecaParaEmpacotar> entrada = pecasDoGrupo.stream()
                .map(p -> new PecaParaEmpacotar(p.getId(), p.getNome(), p.getLarguraMm(), p.getAlturaMm(),
                        p.getQuantidade(), p.getTipoAcabamento()))
                .toList();

        ParametrosChapa params = new ParametrosChapa(
                chapa.getLarguraMm(), chapa.getAlturaMm(), chapa.getQuantidadeDisponivel(),
                chapa.getKerfMm(), chapa.getMargemBordaMm());

        ResultadoOtimizacao resultado = otimizadorDePlano.gerar(params, entrada);
        return new GrupoEmpacotado(chapa, resultado);
    }

    private void preencherTotais(PlanoDeCorte plano, List<GrupoEmpacotado> grupos) {
        int totalChapas = grupos.stream().mapToInt(g -> g.resultado().totalChapasUtilizadas()).sum();
        long areaUtilTotal = grupos.stream()
                .flatMap(g -> g.resultado().chapas().stream())
                .mapToLong(ChapaEmpacotada::areaUtilMm2).sum();
        long areaUtilizadaTotal = grupos.stream()
                .flatMap(g -> g.resultado().chapas().stream())
                .mapToLong(ChapaEmpacotada::areaUtilizadaMm2).sum();

        BigDecimal percentualAproveitamento = areaUtilTotal == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(areaUtilizadaTotal * 100.0 / areaUtilTotal).setScale(2, RoundingMode.HALF_UP);

        plano.setTotalChapasUtilizadas(totalChapas);
        plano.setPercentualAproveitamento(percentualAproveitamento);
        plano.setPercentualDesperdicio(BigDecimal.valueOf(100).subtract(percentualAproveitamento));
    }

    private void persistirGrupos(PlanoDeCorte planoSalvo, List<GrupoEmpacotado> grupos, Map<Long, Peca> pecaPorId) {
        int numeroChapaGlobal = 1;
        int etiquetaOffset = 0;

        for (GrupoEmpacotado grupo : grupos) {
            int maiorEtiquetaDoGrupo = 0;

            for (ChapaEmpacotada empacotada : grupo.resultado().chapas()) {
                ChapaUtilizada chapaUtilizada = new ChapaUtilizada();
                chapaUtilizada.setPlanoDeCorte(planoSalvo);
                chapaUtilizada.setChapa(grupo.chapa());
                chapaUtilizada.setNumeroChapa(numeroChapaGlobal++);
                chapaUtilizada.setAreaDesperdicadaMm2(empacotada.areaDesperdicadaMm2());
                chapaUtilizada.setPercentualAproveitamento(
                        BigDecimal.valueOf(empacotada.percentualAproveitamento()).setScale(2, RoundingMode.HALF_UP));
                ChapaUtilizada chapaUtilizadaSalva = chapaUtilizadaRepository.save(chapaUtilizada);

                for (PosicionamentoCalculado pos : empacotada.posicionamentos()) {
                    maiorEtiquetaDoGrupo = Math.max(maiorEtiquetaDoGrupo, pos.numeroEtiqueta());

                    Posicionamento posicionamento = new Posicionamento();
                    posicionamento.setChapaUtilizada(chapaUtilizadaSalva);
                    posicionamento.setPeca(pecaPorId.get(pos.pecaId()));
                    posicionamento.setNumeroEtiqueta(pos.numeroEtiqueta() + etiquetaOffset);
                    posicionamento.setXMm(pos.xMm());
                    posicionamento.setYMm(pos.yMm());
                    posicionamento.setLarguraMm(pos.larguraMm());
                    posicionamento.setAlturaMm(pos.alturaMm());
                    posicionamento.setRotacionada(pos.rotacionada());
                    posicionamentoRepository.save(posicionamento);
                }

                for (SobraCalculada sobraCalc : empacotada.sobras()) {
                    Sobra sobra = new Sobra();
                    sobra.setChapaUtilizada(chapaUtilizadaSalva);
                    sobra.setXMm(sobraCalc.xMm());
                    sobra.setYMm(sobraCalc.yMm());
                    sobra.setLarguraMm(sobraCalc.larguraMm());
                    sobra.setAlturaMm(sobraCalc.alturaMm());
                    sobraRepository.save(sobra);
                }
            }

            etiquetaOffset += maiorEtiquetaDoGrupo;
        }
    }

    private PlanoDeCorteResponse montarResposta(PlanoDeCorte plano) {
        List<ChapaUtilizadaResponse> chapasResponse = chapaUtilizadaRepository
                .findByPlanoDeCorteIdOrderByNumeroChapaAsc(plano.getId()).stream()
                .map(cu -> {
                    List<PosicionamentoResponse> posicionamentos = posicionamentoRepository
                            .findByChapaUtilizadaIdOrderByNumeroEtiquetaAsc(cu.getId()).stream()
                            .map(PosicionamentoResponse::from)
                            .toList();
                    List<SobraResponse> sobras = sobraRepository
                            .findByChapaUtilizadaIdOrderByIdAsc(cu.getId()).stream()
                            .map(SobraResponse::from)
                            .toList();
                    return ChapaUtilizadaResponse.from(cu, posicionamentos, sobras);
                })
                .toList();

        return PlanoDeCorteResponse.from(plano, chapasResponse);
    }

    private record GrupoEmpacotado(Chapa chapa, ResultadoOtimizacao resultado) {}

    public record PlanoObtidoParaPdf(Projeto projeto, PlanoDeCorteResponse plano) {}
}
