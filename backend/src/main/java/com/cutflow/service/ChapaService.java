package com.cutflow.service;

import com.cutflow.dto.chapa.ChapaRequest;
import com.cutflow.dto.chapa.ChapaResponse;
import com.cutflow.entity.Chapa;
import com.cutflow.entity.Projeto;
import com.cutflow.exception.BusinessRuleException;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.ChapaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Uma chapa por espessura dentro do projeto (doc secao 3.1: o marceneiro-
 * piloto usa uma unica medida de chapa, so a espessura varia). A unicidade
 * ja e' garantida no banco (uq_chapas_projeto_espessura); a checagem aqui
 * antecipa o erro com uma mensagem legivel em vez de esperar o 409 generico
 * do GlobalExceptionHandler.
 */
@Service
@RequiredArgsConstructor
public class ChapaService {

    private final ChapaRepository chapaRepository;
    private final ProjetoService projetoService;

    @Transactional(readOnly = true)
    public List<ChapaResponse> list(UUID projetoUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        return chapaRepository.findByProjetoIdOrderByEspessuraMmAsc(projeto.getId()).stream()
                .map(ChapaResponse::from)
                .toList();
    }

    @Transactional
    public ChapaResponse create(UUID projetoUuid, ChapaRequest request) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);

        if (chapaRepository.existsByProjetoIdAndEspessuraMm(projeto.getId(), request.espessuraMm())) {
            throw new BusinessRuleException(
                    "Já existe uma chapa de %dmm cadastrada neste projeto".formatted(request.espessuraMm()));
        }

        Chapa chapa = new Chapa();
        chapa.setProjeto(projeto);
        aplicarRequest(chapa, request);
        return ChapaResponse.from(chapaRepository.save(chapa));
    }

    @Transactional
    public ChapaResponse update(UUID projetoUuid, UUID chapaUuid, ChapaRequest request) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        Chapa chapa = findOrThrow(projeto.getId(), chapaUuid);

        if (!chapa.getEspessuraMm().equals(request.espessuraMm())
                && chapaRepository.existsByProjetoIdAndEspessuraMm(projeto.getId(), request.espessuraMm())) {
            throw new BusinessRuleException(
                    "Já existe uma chapa de %dmm cadastrada neste projeto".formatted(request.espessuraMm()));
        }

        aplicarRequest(chapa, request);
        return ChapaResponse.from(chapaRepository.save(chapa));
    }

    @Transactional
    public void delete(UUID projetoUuid, UUID chapaUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        chapaRepository.delete(findOrThrow(projeto.getId(), chapaUuid));
    }

    private void aplicarRequest(Chapa chapa, ChapaRequest request) {
        chapa.setLarguraMm(request.larguraMm());
        chapa.setAlturaMm(request.alturaMm());
        chapa.setEspessuraMm(request.espessuraMm());
        chapa.setQuantidadeDisponivel(request.quantidadeDisponivel());
        if (request.kerfMm() != null) {
            chapa.setKerfMm(request.kerfMm());
        }
        if (request.margemBordaMm() != null) {
            chapa.setMargemBordaMm(request.margemBordaMm());
        }
    }

    private Chapa findOrThrow(Long projetoId, UUID chapaUuid) {
        return chapaRepository.findByUuidAndProjetoId(chapaUuid, projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapa não encontrada"));
    }
}
