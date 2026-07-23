package com.cutflow.service;

import com.cutflow.dto.colaborador.ColaboradorResponse;
import com.cutflow.dto.colaborador.ConviteDetalhesResponse;
import com.cutflow.dto.colaborador.ConviteRequest;
import com.cutflow.dto.colaborador.ConviteResponse;
import com.cutflow.entity.ColaboradorProjeto;
import com.cutflow.entity.ConviteProjeto;
import com.cutflow.entity.Projeto;
import com.cutflow.entity.Usuario;
import com.cutflow.exception.AcessoNegadoException;
import com.cutflow.exception.BusinessRuleException;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.ColaboradorProjetoRepository;
import com.cutflow.repository.ConviteProjetoRepository;
import com.cutflow.security.OrganizacaoContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Compartilhamento direto de um projeto, sem organizacao (ADR-0006) - o
 * caminho "Joaquim chama o Carlos pra ajudar num bico". Quem tem acesso de
 * EDICAO ao projeto (Membro da organizacao, ou colaborador EDITOR) pode
 * convidar; colaborador VISUALIZADOR nao.
 *
 * Nao ha envio de e-mail automatico (sem servidor de e-mail configurado): o
 * retorno de criarConvite ja traz a URL pronta para copiar e mandar por onde
 * for mais natural (WhatsApp, e-mail pessoal etc). Ver ConviteProjeto para a
 * distincao entre link reutilizavel e convite direcionado.
 */
@Service
@RequiredArgsConstructor
public class ColaboradorService {

    private final ProjetoService projetoService;
    private final ColaboradorProjetoRepository colaboradorRepository;
    private final ConviteProjetoRepository conviteRepository;
    private final OrganizacaoContexto organizacaoContexto;

    @Value("${cutflow.frontend-url}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<ColaboradorResponse> listarColaboradores(UUID projetoUuid) {
        Projeto projeto = resolverComEdicao(projetoUuid);
        return colaboradorRepository.findByProjetoIdOrderByCreatedAtAsc(projeto.getId()).stream()
                .map(ColaboradorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConviteResponse> listarConvitesAtivos(UUID projetoUuid) {
        Projeto projeto = resolverComEdicao(projetoUuid);
        return conviteRepository.findByProjetoIdAndRevogadoFalseAndAceitoEmIsNullOrderByCreatedAtDesc(projeto.getId())
                .stream()
                .map(c -> ConviteResponse.from(c, frontendUrl))
                .toList();
    }

    @Transactional
    public ConviteResponse criarConvite(UUID projetoUuid, ConviteRequest request) {
        Projeto projeto = resolverComEdicao(projetoUuid);
        Usuario criadoPor = organizacaoContexto.usuarioAtual();

        ConviteProjeto convite = new ConviteProjeto();
        convite.setProjeto(projeto);
        convite.setPapel(request.papel());
        convite.setEmailAlvo(request.email() != null && !request.email().isBlank()
                ? AuthService.normalizarEmail(request.email()) : null);
        convite.setCriadoPor(criadoPor);

        return ConviteResponse.from(conviteRepository.save(convite), frontendUrl);
    }

    @Transactional
    public void revogarConvite(UUID projetoUuid, UUID conviteUuid) {
        Projeto projeto = resolverComEdicao(projetoUuid);
        ConviteProjeto convite = conviteRepository.findByUuidAndProjetoId(conviteUuid, projeto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));
        convite.setRevogado(true);
    }

    @Transactional
    public void removerColaborador(UUID projetoUuid, UUID colaboradorUuid) {
        Projeto projeto = resolverComEdicao(projetoUuid);
        ColaboradorProjeto colaborador = colaboradorRepository.findByUuidAndProjetoId(colaboradorUuid, projeto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
        colaboradorRepository.delete(colaborador);
    }

    // ---- Fluxo publico de aceite (nao exige acesso previo ao projeto) ----

    @Transactional(readOnly = true)
    public ConviteDetalhesResponse detalhes(UUID token) {
        ConviteProjeto convite = conviteRepository.findByUuid(token)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));
        return new ConviteDetalhesResponse(
                convite.getProjeto().getNome(),
                convite.getCriadoPor().getNome(),
                convite.getPapel(),
                convite.getEmailAlvo(),
                convite.valido());
    }

    @Transactional
    public UUID aceitar(UUID token) {
        ConviteProjeto convite = conviteRepository.findByUuid(token)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));
        if (!convite.valido()) {
            throw new BusinessRuleException("Este convite não é mais válido (usado ou revogado)");
        }

        Usuario usuario = organizacaoContexto.usuarioAtual();
        if (convite.getEmailAlvo() != null && !convite.getEmailAlvo().equalsIgnoreCase(usuario.getEmail())) {
            throw new AcessoNegadoException(
                    "Este convite foi feito para outro e-mail — entre com a conta de " + convite.getEmailAlvo());
        }

        Long projetoId = convite.getProjeto().getId();
        colaboradorRepository.findByProjetoIdAndUsuarioId(projetoId, usuario.getId())
                .ifPresentOrElse(
                        existente -> existente.setPapel(convite.getPapel()), // reaceitar/atualizar papel
                        () -> {
                            ColaboradorProjeto colaborador = new ColaboradorProjeto();
                            colaborador.setProjeto(convite.getProjeto());
                            colaborador.setUsuario(usuario);
                            colaborador.setPapel(convite.getPapel());
                            colaboradorRepository.save(colaborador);
                        });

        // Convite direcionado (com e-mail) e' de uso unico -> marca aceito.
        // Link reutilizavel (sem e-mail) NAO e' consumido: varias pessoas
        // podem usar o mesmo link ate ele ser revogado (ADR-0006).
        if (!convite.reutilizavel()) {
            convite.setAceitoPor(usuario);
            convite.setAceitoEm(OffsetDateTime.now());
        }

        return convite.getProjeto().getUuid();
    }

    /** Resolve o projeto exigindo acesso de EDICAO - so quem edita pode gerenciar compartilhamento. */
    private Projeto resolverComEdicao(UUID projetoUuid) {
        Projeto projeto = projetoService.findOrThrow(projetoUuid);
        projetoService.exigirPodeEditar(projeto);
        return projeto;
    }
}
