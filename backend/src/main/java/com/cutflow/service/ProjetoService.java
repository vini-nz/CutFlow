package com.cutflow.service;

import com.cutflow.dto.projeto.ProjetoRequest;
import com.cutflow.dto.projeto.ProjetoResponse;
import com.cutflow.entity.Organizacao;
import com.cutflow.entity.Projeto;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.ProjetoRepository;
import com.cutflow.security.OrganizacaoContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ponto unico de aplicacao do multi-tenant (ADR-0005): toda operacao e'
 * escopada pela organizacao ativa do usuario logado (OrganizacaoContexto).
 * Como PecaService, ChapaService e PlanoDeCorteService resolvem o projeto
 * sempre por findOrThrow, escopar aqui protege transitivamente pecas, chapas
 * e planos - um usuario nunca alcanca dados de outra organizacao pela URL.
 */
@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final OrganizacaoContexto organizacaoContexto;

    @Transactional(readOnly = true)
    public Page<ProjetoResponse> list(Pageable pageable) {
        Organizacao organizacao = organizacaoContexto.organizacaoAtiva();
        return projetoRepository.findByOrganizacaoIdOrderByCreatedAtDesc(organizacao.getId(), pageable)
                .map(ProjetoResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjetoResponse get(UUID uuid) {
        return ProjetoResponse.from(findOrThrow(uuid));
    }

    @Transactional
    public ProjetoResponse create(ProjetoRequest request) {
        Organizacao organizacao = organizacaoContexto.organizacaoAtiva();
        Projeto projeto = new Projeto();
        projeto.setOrganizacao(organizacao);
        projeto.setNome(request.nome());
        projeto.setCliente(request.cliente());
        return ProjetoResponse.from(projetoRepository.save(projeto));
    }

    @Transactional
    public ProjetoResponse update(UUID uuid, ProjetoRequest request) {
        Projeto projeto = findOrThrow(uuid);
        projeto.setNome(request.nome());
        projeto.setCliente(request.cliente());
        return ProjetoResponse.from(projetoRepository.save(projeto));
    }

    @Transactional
    public void delete(UUID uuid) {
        projetoRepository.delete(findOrThrow(uuid));
    }

    /**
     * Resolve um projeto SEMPRE dentro da organizacao ativa. Um uuid de outra
     * organizacao devolve 404 (nao 403) de proposito - nao confirma sequer a
     * existencia do recurso para quem nao pode ve-lo.
     */
    @Transactional(readOnly = true)
    public Projeto findOrThrow(UUID uuid) {
        Organizacao organizacao = organizacaoContexto.organizacaoAtiva();
        return projetoRepository.findByUuidAndOrganizacaoId(uuid, organizacao.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));
    }
}
