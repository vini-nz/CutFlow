package com.cutflow.controller;

import com.cutflow.dto.planodecorte.PlanoDeCorteResponse;
import com.cutflow.service.PlanoDeCortePdfService;
import com.cutflow.service.PlanoDeCorteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projetos/{projetoUuid}/plano-de-corte")
@RequiredArgsConstructor
public class PlanoDeCorteController {

    private final PlanoDeCorteService planoDeCorteService;
    private final PlanoDeCortePdfService planoDeCortePdfService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoDeCorteResponse gerar(@PathVariable UUID projetoUuid) {
        return planoDeCorteService.gerar(projetoUuid);
    }

    @GetMapping
    public PlanoDeCorteResponse obterUltimo(@PathVariable UUID projetoUuid) {
        return planoDeCorteService.obterUltimo(projetoUuid);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable UUID projetoUuid) {
        var dados = planoDeCorteService.obterUltimoParaPdf(projetoUuid);
        byte[] pdf = planoDeCortePdfService.generate(dados.projeto(), dados.plano());

        String nomeArquivo = "plano-de-corte-%s.pdf".formatted(dados.projeto().getNome()
                .toLowerCase().replaceAll("[^a-z0-9]+", "-"));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomeArquivo).build().toString())
                .body(pdf);
    }
}
