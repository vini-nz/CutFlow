package com.cutflow.service;

import com.cutflow.dto.peca.PecaRequest;
import com.cutflow.dto.peca.PecaResponse;
import com.cutflow.entity.Peca;
import com.cutflow.entity.Projeto;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.PecaRepository;
import com.cutflow.repository.PlanoDeCorteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Desde a ADR-0003, salvar uma Peca garante (auto-provisiona, se ainda nao
 * existir) a Chapa da combinacao espessura+acabamento correspondente - o
 * usuario nunca precisa cadastrar uma Chapa antes de adicionar peca nenhuma.
 *
 * Toda mutacao de peca (criar/editar/remover) invalida os planos de corte ja
 * gerados do projeto (ADR-0004): um plano e' uma foto derivada das pecas no
 * momento da geracao - depois que as pecas mudam, ele nao representa mais
 * nada e ainda impediria a remocao da peca (posicionamentos referenciam
 * pecas). O frontend regenera o plano automaticamente apos cada mudanca.
 */
@Service
@RequiredArgsConstructor
public class PecaService {

    private final PecaRepository pecaRepository;
    private final PlanoDeCorteRepository planoDeCorteRepository;
    private final ProjetoService projetoService;
    private final ChapaService chapaService;

    @Transactional(readOnly = true)
    public List<PecaResponse> list(UUID projetoUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        return pecaRepository.findByProjetoIdOrderByCreatedAtAsc(projeto.getId()).stream()
                .map(PecaResponse::from)
                .toList();
    }

    @Transactional
    public PecaResponse create(UUID projetoUuid, PecaRequest request) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        chapaService.garantirChapa(projeto.getId(), projeto, request.espessuraMm(), request.tipoAcabamento());

        Peca peca = new Peca();
        peca.setProjeto(projeto);
        aplicarRequest(peca, request);
        planoDeCorteRepository.deleteByProjetoId(projeto.getId());
        return PecaResponse.from(pecaRepository.save(peca));
    }

    @Transactional
    public PecaResponse update(UUID projetoUuid, UUID pecaUuid, PecaRequest request) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        Peca peca = findOrThrow(projeto.getId(), pecaUuid);
        chapaService.garantirChapa(projeto.getId(), projeto, request.espessuraMm(), request.tipoAcabamento());
        aplicarRequest(peca, request);
        planoDeCorteRepository.deleteByProjetoId(projeto.getId());
        return PecaResponse.from(pecaRepository.save(peca));
    }

    @Transactional
    public void delete(UUID projetoUuid, UUID pecaUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        Peca peca = findOrThrow(projeto.getId(), pecaUuid);
        // A invalidacao precisa vir ANTES da delecao: posicionamentos de
        // planos antigos referenciam a peca e bloqueariam o DELETE.
        planoDeCorteRepository.deleteByProjetoId(projeto.getId());
        pecaRepository.delete(peca);
    }

    private void aplicarRequest(Peca peca, PecaRequest request) {
        peca.setNome(request.nome());
        peca.setAlturaMm(request.alturaMm());
        peca.setLarguraMm(request.larguraMm());
        peca.setEspessuraMm(request.espessuraMm());
        peca.setQuantidade(request.quantidade());
        peca.setTipoAcabamento(request.tipoAcabamento());
    }

    private Peca findOrThrow(Long projetoId, UUID pecaUuid) {
        return pecaRepository.findByUuidAndProjetoId(pecaUuid, projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada"));
    }
}
