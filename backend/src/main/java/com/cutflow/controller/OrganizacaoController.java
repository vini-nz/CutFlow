package com.cutflow.controller;

import com.cutflow.dto.organizacao.MembroRequest;
import com.cutflow.dto.organizacao.MembroResponse;
import com.cutflow.dto.organizacao.OrganizacaoRequest;
import com.cutflow.dto.organizacao.OrganizacaoResponse;
import com.cutflow.service.OrganizacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Organizacoes (workspaces) e equipe (ADR-0005). Todas as rotas exigem
 * autenticacao; a gestao de membros ainda exige papel OWNER/ADMIN, verificado
 * no OrganizacaoService via OrganizacaoContexto.
 */
@RestController
@RequestMapping("/api/v1/organizacoes")
@RequiredArgsConstructor
public class OrganizacaoController {

    private final OrganizacaoService organizacaoService;

    @GetMapping
    public List<OrganizacaoResponse> listar() {
        return organizacaoService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacaoResponse criar(@Valid @RequestBody OrganizacaoRequest request) {
        return organizacaoService.criar(request);
    }

    /** Troca o workspace ativo da sessao. */
    @PostMapping("/{uuid}/ativar")
    public OrganizacaoResponse ativar(@PathVariable UUID uuid) {
        return organizacaoService.ativar(uuid);
    }

    @GetMapping("/{uuid}/membros")
    public List<MembroResponse> listarMembros(@PathVariable UUID uuid) {
        return organizacaoService.listarMembros(uuid);
    }

    @PostMapping("/{uuid}/membros")
    @ResponseStatus(HttpStatus.CREATED)
    public MembroResponse adicionarMembro(@PathVariable UUID uuid, @Valid @RequestBody MembroRequest request) {
        return organizacaoService.adicionarMembro(uuid, request);
    }

    @DeleteMapping("/{uuid}/membros/{membroUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerMembro(@PathVariable UUID uuid, @PathVariable UUID membroUuid) {
        organizacaoService.removerMembro(uuid, membroUuid);
    }
}
