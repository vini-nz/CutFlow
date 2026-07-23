package com.cutflow.controller;

import com.cutflow.dto.colaborador.ConviteAceiteResponse;
import com.cutflow.dto.colaborador.ConviteDetalhesResponse;
import com.cutflow.service.ColaboradorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Fluxo publico de aceite de convite (ADR-0006) - /convite/{token} no frontend.
 * O GET e' permitAll (SecurityConfig) para mostrar "Fulano te convidou para o
 * projeto X" ANTES de pedir login; o POST /aceitar exige sessao.
 */
@RestController
@RequestMapping("/api/v1/convites/{token}")
@RequiredArgsConstructor
public class ConviteController {

    private final ColaboradorService colaboradorService;

    @GetMapping
    public ConviteDetalhesResponse detalhes(@PathVariable UUID token) {
        return colaboradorService.detalhes(token);
    }

    @PostMapping("/aceitar")
    public ConviteAceiteResponse aceitar(@PathVariable UUID token) {
        return new ConviteAceiteResponse(colaboradorService.aceitar(token));
    }
}
