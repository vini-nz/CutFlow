package com.cutflow.controller;

import com.cutflow.dto.chapa.ChapaRequest;
import com.cutflow.dto.chapa.ChapaResponse;
import com.cutflow.service.ChapaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Desde a ADR-0003, Chapa nao tem criacao manual pelo usuario: ela e'
 * auto-provisionada por combinacao espessura+acabamento ao cadastrar uma
 * Peca (ver PecaService/ChapaService.garantirChapa). Os endpoints de escrita
 * sao a edicao (largura/altura/kerf/margem) e a exclusao - esta so permitida
 * quando nenhuma peca da combinacao existe mais (senao a chapa seria
 * recriada em silencio no proximo plano; ChapaService devolve 409 com
 * mensagem explicando o motivo).
 */
@RestController
@RequestMapping("/api/v1/projetos/{projetoUuid}/chapas")
@RequiredArgsConstructor
public class ChapaController {

    private final ChapaService chapaService;

    @GetMapping
    public List<ChapaResponse> list(@PathVariable UUID projetoUuid) {
        return chapaService.list(projetoUuid);
    }

    @PutMapping("/{uuid}")
    public ChapaResponse update(@PathVariable UUID projetoUuid, @PathVariable UUID uuid,
                                 @Valid @RequestBody ChapaRequest request) {
        return chapaService.update(projetoUuid, uuid, request);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID projetoUuid, @PathVariable UUID uuid) {
        chapaService.delete(projetoUuid, uuid);
    }
}
