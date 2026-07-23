package com.cutflow.service;

import com.cutflow.dto.projeto.ProjetoRequest;
import com.cutflow.dto.projeto.ProjetoResponse;
import com.cutflow.entity.ColaboradorProjeto;
import com.cutflow.entity.Organizacao;
import com.cutflow.entity.Projeto;
import com.cutflow.entity.Usuario;
import com.cutflow.enums.PapelColaborador;
import com.cutflow.exception.AcessoNegadoException;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.ColaboradorProjetoRepository;
import com.cutflow.repository.MembroRepository;
import com.cutflow.repository.ProjetoRepository;
import com.cutflow.security.OrganizacaoContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ponto unico de autorizacao sobre Projeto (ADR-0005 + ADR-0006). Como
 * PecaService, ChapaService e PlanoDeCorteService resolvem o projeto sempre
 * por findOrThrow/exigirPodeEditar, autorizar aqui protege transitivamente
 * pecas, chapas e planos.
 *
 * Desde a ADR-0006 existem DOIS caminhos de acesso, que nao se misturam:
 * - Membro da Organizacao dona do projeto -> EDICAO sempre;
 * - ColaboradorProjeto direto (convite aceito) -> conforme o papel do convite
 *   (EDITOR = edicao, VISUALIZADOR = leitura), MESMO que o projeto seja de uma
 *   organizacao da qual o usuario nao e' membro.
 *
 * list()/create() continuam escopados pela organizacao ATIVA (workspace);
 * findOrThrow(uuid) e' independente da ativa de proposito - um projeto
 * compartilhado comigo precisa abrir mesmo com outro workspace selecionado
 * (ver listCompartilhadosComigo).
 */
@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final MembroRepository membroRepository;
    private final ColaboradorProjetoRepository colaboradorProjetoRepository;
    private final OrganizacaoContexto organizacaoContexto;

    @Transactional(readOnly = true)
    public Page<ProjetoResponse> list(Pageable pageable) {
        Organizacao organizacao = organizacaoContexto.organizacaoAtiva();
        return projetoRepository.findByOrganizacaoIdOrderByCreatedAtDesc(organizacao.getId(), pageable)
                .map(ProjetoResponse::from);
    }

    /** Projetos compartilhados DIRETAMENTE comigo (ADR-0006) - de qualquer organizacao, independente da ativa. */
    @Transactional(readOnly = true)
    public List<ProjetoResponse> listCompartilhadosComigo() {
        Usuario usuario = organizacaoContexto.usuarioAtual();
        return colaboradorProjetoRepository.findByUsuarioIdOrderByCreatedAtDesc(usuario.getId()).stream()
                .map(c -> ProjetoResponse.from(c.getProjeto(), c.getPapel().podeEditar()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjetoResponse get(UUID uuid) {
        Projeto projeto = findOrThrow(uuid);
        return ProjetoResponse.from(projeto, nivelAcesso(projeto) == NivelAcesso.EDICAO);
    }

    @Transactional
    public ProjetoResponse create(ProjetoRequest request) {
        Organizacao organizacao = organizacaoContexto.organizacaoAtiva();
        Projeto projeto = new Projeto();
        projeto.setOrganizacao(organizacao);
        projeto.setNome(request.nome());
        projeto.setCliente(request.cliente());
        return ProjetoResponse.from(projetoRepository.save(projeto), true);
    }

    @Transactional
    public ProjetoResponse update(UUID uuid, ProjetoRequest request) {
        Projeto projeto = findOrThrow(uuid);
        exigirPodeEditar(projeto);
        projeto.setNome(request.nome());
        projeto.setCliente(request.cliente());
        return ProjetoResponse.from(projetoRepository.save(projeto), true);
    }

    @Transactional
    public void delete(UUID uuid) {
        Projeto projeto = findOrThrow(uuid);
        exigirPodeEditar(projeto);
        projetoRepository.delete(projeto);
    }

    /**
     * Resolve um projeto que o usuario atual pode ao menos VISUALIZAR (Membro
     * da organizacao OU colaborador direto, qualquer papel). Um uuid sem
     * nenhum dos dois vira 404 (nao 403) de proposito - nao confirma sequer a
     * existencia do recurso para quem nao tem acesso nenhum a ele.
     */
    @Transactional(readOnly = true)
    public Projeto findOrThrow(UUID uuid) {
        Projeto projeto = projetoRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));
        if (nivelAcesso(projeto) == null) {
            throw new ResourceNotFoundException("Projeto não encontrado");
        }
        return projeto;
    }

    /**
     * Chamado pelos services filhos (Peca/Chapa/Plano) antes de qualquer
     * mutacao. Um colaborador VISUALIZADOR passa por findOrThrow (le peças/
     * plano) mas cai aqui - unico lugar que distingue leitura de edicao.
     */
    @Transactional(readOnly = true)
    public void exigirPodeEditar(Projeto projeto) {
        if (nivelAcesso(projeto) != NivelAcesso.EDICAO) {
            throw new AcessoNegadoException("Você tem acesso somente de visualização a este projeto");
        }
    }

    @Transactional(readOnly = true)
    public NivelAcesso nivelAcesso(Projeto projeto) {
        Usuario usuario = organizacaoContexto.usuarioAtual();

        if (membroRepository.existsByUsuarioIdAndOrganizacaoId(usuario.getId(), projeto.getOrganizacao().getId())) {
            return NivelAcesso.EDICAO;
        }

        return colaboradorProjetoRepository.findByProjetoIdAndUsuarioId(projeto.getId(), usuario.getId())
                .map(ColaboradorProjeto::getPapel)
                .map(papel -> papel == PapelColaborador.EDITOR ? NivelAcesso.EDICAO : NivelAcesso.LEITURA)
                .orElse(null);
    }

    public enum NivelAcesso {
        LEITURA,
        EDICAO
    }
}
