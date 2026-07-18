package com.cutflow.service;

import com.cutflow.dto.peca.PecaRequest;
import com.cutflow.dto.peca.PecaResponse;
import com.cutflow.entity.Peca;
import com.cutflow.entity.Projeto;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.PecaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PecaService {

    private final PecaRepository pecaRepository;
    private final ProjetoService projetoService;

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

        Peca peca = new Peca();
        peca.setProjeto(projeto);
        aplicarRequest(peca, request);
        return PecaResponse.from(pecaRepository.save(peca));
    }

    @Transactional
    public PecaResponse update(UUID projetoUuid, UUID pecaUuid, PecaRequest request) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        Peca peca = findOrThrow(projeto.getId(), pecaUuid);
        aplicarRequest(peca, request);
        return PecaResponse.from(pecaRepository.save(peca));
    }

    @Transactional
    public void delete(UUID projetoUuid, UUID pecaUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        pecaRepository.delete(findOrThrow(projeto.getId(), pecaUuid));
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
