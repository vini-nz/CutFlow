package com.cutflow.service;

import com.cutflow.dto.chapa.ChapaRequest;
import com.cutflow.dto.chapa.ChapaResponse;
import com.cutflow.entity.Chapa;
import com.cutflow.entity.Projeto;
import com.cutflow.enums.TipoAcabamento;
import com.cutflow.exception.BusinessRuleException;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.ChapaRepository;
import com.cutflow.repository.PecaRepository;
import com.cutflow.repository.PlanoDeCorteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Uma chapa por combinacao espessura+acabamento dentro do projeto. O
 * acabamento (LISO / COM_VEIO) ja vem de fabrica na chapa (ADR-0004): peca
 * com veio so sai de chapa com veio, peca lisa de chapa lisa - por isso a
 * combinacao, e nao so a espessura, identifica a chapa.
 *
 * Desde a ADR-0003, o usuario nao cadastra uma Chapa manualmente: ela e'
 * auto-provisionada com valores padrao (garantirChapa) assim que uma Peca
 * da combinacao correspondente e' criada (chamado por PecaService) ou, como
 * rede de seguranca, no momento de gerar o plano (PlanoDeCorteService).
 * As acoes do usuario sobre Chapa sao editar os valores (largura/altura/
 * kerf/margem) e excluir - esta ultima apenas quando nenhuma peca da
 * combinacao existir mais, senao a chapa seria recriada com padroes no
 * proximo plano, desfazendo a exclusao de forma silenciosa e confusa.
 */
@Service
@RequiredArgsConstructor
public class ChapaService {

    // Medidas confirmadas na entrevista (doc secao 3.1): chapa padrao unica,
    // so a espessura varia. Kerf e margem tambem confirmados (secoes 3.2/3.1
    // atualizadas) - ambos ajustaveis pelo marceneiro se perceber divergencia
    // na pratica, por isso continuam editaveis mesmo vindo com valor padrao.
    private static final int DEFAULT_LARGURA_MM = 1840;
    private static final int DEFAULT_ALTURA_MM = 2740;
    private static final int DEFAULT_KERF_MM = 4;
    private static final int DEFAULT_MARGEM_BORDA_MM = 6;

    private final ChapaRepository chapaRepository;
    private final PecaRepository pecaRepository;
    private final PlanoDeCorteRepository planoDeCorteRepository;
    private final ProjetoService projetoService;

    @Transactional(readOnly = true)
    public List<ChapaResponse> list(UUID projetoUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        return chapaRepository.findByProjetoIdOrderByEspessuraMmAscTipoAcabamentoAsc(projeto.getId()).stream()
                .map(ChapaResponse::from)
                .toList();
    }

    @Transactional
    public ChapaResponse update(UUID projetoUuid, UUID chapaUuid, ChapaRequest request) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        Chapa chapa = findOrThrow(projeto.getId(), chapaUuid);

        chapa.setLarguraMm(request.larguraMm());
        chapa.setAlturaMm(request.alturaMm());
        if (request.kerfMm() != null) {
            chapa.setKerfMm(request.kerfMm());
        }
        if (request.margemBordaMm() != null) {
            chapa.setMargemBordaMm(request.margemBordaMm());
        }
        // espessuraMm e tipoAcabamento nao sao alterados aqui de proposito:
        // ambos identificam quais Pecas a chapa representa. Trocar espessura/
        // acabamento de uma peca cria/usa a Chapa daquela outra combinacao.

        // Planos ja gerados usaram os parametros antigos (dimensoes/kerf/
        // margem) - ficariam incoerentes com a chapa atual (ADR-0004).
        planoDeCorteRepository.deleteByProjetoId(projeto.getId());

        return ChapaResponse.from(chapaRepository.save(chapa));
    }

    @Transactional
    public void delete(UUID projetoUuid, UUID chapaUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        Chapa chapa = findOrThrow(projeto.getId(), chapaUuid);

        boolean temPecasDaCombinacao = pecaRepository.existsByProjetoIdAndEspessuraMmAndTipoAcabamento(
                projeto.getId(), chapa.getEspessuraMm(), chapa.getTipoAcabamento());
        if (temPecasDaCombinacao) {
            throw new BusinessRuleException(
                    "Ainda existem peças de %dmm (%s) neste projeto — remova-as (ou mude a espessura/acabamento delas) antes de excluir esta chapa"
                            .formatted(chapa.getEspessuraMm(), rotulo(chapa.getTipoAcabamento())));
        }

        // Planos antigos podem referenciar esta chapa (chapas_utilizadas).
        planoDeCorteRepository.deleteByProjetoId(projeto.getId());
        chapaRepository.delete(chapa);
    }

    /**
     * Retorna a Chapa da combinacao espessura+acabamento, criando-a com
     * valores padrao se ainda nao existir. Chamado por PecaService ao salvar
     * uma Peca (fluxo principal) e por PlanoDeCorteService como rede de
     * seguranca (caso uma Peca exista sem a Chapa correspondente).
     */
    @Transactional
    public Chapa garantirChapa(Long projetoId, Projeto projeto, Integer espessuraMm, TipoAcabamento tipoAcabamento) {
        return chapaRepository.findByProjetoIdAndEspessuraMmAndTipoAcabamento(projetoId, espessuraMm, tipoAcabamento)
                .orElseGet(() -> {
                    Chapa chapa = new Chapa();
                    chapa.setProjeto(projeto);
                    chapa.setLarguraMm(DEFAULT_LARGURA_MM);
                    chapa.setAlturaMm(DEFAULT_ALTURA_MM);
                    chapa.setEspessuraMm(espessuraMm);
                    chapa.setTipoAcabamento(tipoAcabamento);
                    chapa.setKerfMm(DEFAULT_KERF_MM);
                    chapa.setMargemBordaMm(DEFAULT_MARGEM_BORDA_MM);
                    return chapaRepository.save(chapa);
                });
    }

    private String rotulo(TipoAcabamento tipoAcabamento) {
        return tipoAcabamento == TipoAcabamento.COM_VEIO ? "com veio" : "liso";
    }

    private Chapa findOrThrow(Long projetoId, UUID chapaUuid) {
        return chapaRepository.findByUuidAndProjetoId(chapaUuid, projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapa não encontrada"));
    }
}
