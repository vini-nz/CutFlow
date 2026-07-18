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

@RestController
@RequestMapping("/api/v1/projetos/{projetoUuid}/chapas")
@RequiredArgsConstructor
public class ChapaController {

    private final ChapaService chapaService;

    @GetMapping
    public List<ChapaResponse> list(@PathVariable UUID projetoUuid) {
        return chapaService.list(projetoUuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChapaResponse create(@PathVariable UUID projetoUuid, @Valid @RequestBody ChapaRequest request) {
        return chapaService.create(projetoUuid, request);
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
