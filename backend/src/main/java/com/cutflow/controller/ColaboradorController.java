package com.cutflow.controller;

import com.cutflow.dto.colaborador.ColaboradorResponse;
import com.cutflow.dto.colaborador.ConviteRequest;
import com.cutflow.dto.colaborador.ConviteResponse;
import com.cutflow.service.ColaboradorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestao de compartilhamento direto de UM projeto (ADR-0006) - convites/link
 * e colaboradores. Ver ConviteController para o fluxo publico de aceite. Todas
 * as rotas exigem acesso de EDICAO ao projeto (checado no ColaboradorService).
 */
@RestController
@RequestMapping("/api/v1/projetos/{projetoUuid}")
@RequiredArgsConstructor
public class ColaboradorController {

    private final ColaboradorService colaboradorService;

    @GetMapping("/colaboradores")
    public List<ColaboradorResponse> listarColaboradores(@PathVariable UUID projetoUuid) {
        return colaboradorService.listarColaboradores(projetoUuid);
    }

    @DeleteMapping("/colaboradores/{colaboradorUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerColaborador(@PathVariable UUID projetoUuid, @PathVariable UUID colaboradorUuid) {
        colaboradorService.removerColaborador(projetoUuid, colaboradorUuid);
    }

    @GetMapping("/convites")
    public List<ConviteResponse> listarConvites(@PathVariable UUID projetoUuid) {
        return colaboradorService.listarConvitesAtivos(projetoUuid);
    }

    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    public ConviteResponse criarConvite(@PathVariable UUID projetoUuid, @Valid @RequestBody ConviteRequest request) {
        return colaboradorService.criarConvite(projetoUuid, request);
    }

    @DeleteMapping("/convites/{conviteUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogarConvite(@PathVariable UUID projetoUuid, @PathVariable UUID conviteUuid) {
        colaboradorService.revogarConvite(projetoUuid, conviteUuid);
    }
}
