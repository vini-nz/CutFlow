package com.cutflow.controller;

import com.cutflow.dto.peca.PecaRequest;
import com.cutflow.dto.peca.PecaResponse;
import com.cutflow.service.PecaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projetos/{projetoUuid}/pecas")
@RequiredArgsConstructor
public class PecaController {

    private final PecaService pecaService;

    @GetMapping
    public List<PecaResponse> list(@PathVariable UUID projetoUuid) {
        return pecaService.list(projetoUuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PecaResponse create(@PathVariable UUID projetoUuid, @Valid @RequestBody PecaRequest request) {
        return pecaService.create(projetoUuid, request);
    }

    @PutMapping("/{uuid}")
    public PecaResponse update(@PathVariable UUID projetoUuid, @PathVariable UUID uuid,
                                @Valid @RequestBody PecaRequest request) {
        return pecaService.update(projetoUuid, uuid, request);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID projetoUuid, @PathVariable UUID uuid) {
        pecaService.delete(projetoUuid, uuid);
    }
}
