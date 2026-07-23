package com.cutflow.controller;

import com.cutflow.dto.projeto.ProjetoRequest;
import com.cutflow.dto.projeto.ProjetoResponse;
import com.cutflow.service.ProjetoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetoService projetoService;

    @GetMapping
    public Page<ProjetoResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projetoService.list(pageable);
    }

    /** Projetos compartilhados diretamente comigo (ADR-0006), fora do workspace ativo. */
    @GetMapping("/compartilhados")
    public List<ProjetoResponse> compartilhadosComigo() {
        return projetoService.listCompartilhadosComigo();
    }

    @GetMapping("/{uuid}")
    public ProjetoResponse get(@PathVariable UUID uuid) {
        return projetoService.get(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjetoResponse create(@Valid @RequestBody ProjetoRequest request) {
        return projetoService.create(request);
    }

    @PutMapping("/{uuid}")
    public ProjetoResponse update(@PathVariable UUID uuid, @Valid @RequestBody ProjetoRequest request) {
        return projetoService.update(uuid, request);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid) {
        projetoService.delete(uuid);
    }
}
