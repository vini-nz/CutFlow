package com.cutflow.service;

import com.cutflow.dto.organizacao.MembroRequest;
import com.cutflow.dto.organizacao.MembroResponse;
import com.cutflow.dto.organizacao.OrganizacaoRequest;
import com.cutflow.dto.organizacao.OrganizacaoResponse;
import com.cutflow.entity.Membro;
import com.cutflow.entity.Organizacao;
import com.cutflow.entity.Usuario;
import com.cutflow.enums.PapelMembro;
import com.cutflow.exception.BusinessRuleException;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.MembroRepository;
import com.cutflow.repository.OrganizacaoRepository;
import com.cutflow.repository.UsuarioRepository;
import com.cutflow.security.OrganizacaoContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida das organizacoes e da equipe (ADR-0005). Quem cria uma
 * organizacao vira OWNER e a organizacao ja fica ativa. Gestao de equipe
 * (adicionar/remover membro) exige papel OWNER/ADMIN sobre a organizacao do
 * path, verificado pelo OrganizacaoContexto.
 */
@Service
@RequiredArgsConstructor
public class OrganizacaoService {

    private final OrganizacaoRepository organizacaoRepository;
    private final MembroRepository membroRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoContexto organizacaoContexto;

    @Transactional
    public OrganizacaoResponse criar(OrganizacaoRequest request) {
        Usuario usuario = organizacaoContexto.usuarioAtual();

        Organizacao organizacao = new Organizacao();
        organizacao.setNome(request.nome().trim());
        organizacao.setDocumento(request.documento() != null && !request.documento().isBlank()
                ? request.documento().trim() : null);
        organizacao = organizacaoRepository.save(organizacao);

        Membro membro = new Membro();
        membro.setUsuario(usuario);
        membro.setOrganizacao(organizacao);
        membro.setPapel(PapelMembro.OWNER);
        membroRepository.save(membro);

        // Nova organizacao ja vira o workspace ativo.
        organizacaoContexto.definirOrganizacaoAtiva(organizacao.getUuid());
        return OrganizacaoResponse.from(organizacao, PapelMembro.OWNER);
    }

    @Transactional(readOnly = true)
    public List<OrganizacaoResponse> listar() {
        Usuario usuario = organizacaoContexto.usuarioAtual();
        return membroRepository.findByUsuarioIdOrderByCreatedAtAsc(usuario.getId()).stream()
                .map(m -> OrganizacaoResponse.from(m.getOrganizacao(), m.getPapel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizacaoResponse ativar(UUID organizacaoUuid) {
        Organizacao organizacao = organizacaoContexto.definirOrganizacaoAtiva(organizacaoUuid);
        Membro membro = organizacaoContexto.exigirMembroDe(organizacaoUuid);
        return OrganizacaoResponse.from(organizacao, membro.getPapel());
    }

    /** Edita nome/documento da organizacao - exige papel de gestao (OWNER/ADMIN). */
    @Transactional
    public OrganizacaoResponse atualizar(UUID organizacaoUuid, OrganizacaoRequest request) {
        Membro gestor = organizacaoContexto.exigirGestaoDeMembrosDe(organizacaoUuid);
        Organizacao organizacao = gestor.getOrganizacao();
        organizacao.setNome(request.nome().trim());
        organizacao.setDocumento(request.documento() != null && !request.documento().isBlank()
                ? request.documento().trim() : null);
        return OrganizacaoResponse.from(organizacaoRepository.save(organizacao), gestor.getPapel());
    }

    @Transactional(readOnly = true)
    public List<MembroResponse> listarMembros(UUID organizacaoUuid) {
        Membro solicitante = organizacaoContexto.exigirMembroDe(organizacaoUuid);
        return membroRepository.findByOrganizacaoIdOrderByCreatedAtAsc(solicitante.getOrganizacao().getId()).stream()
                .map(MembroResponse::from)
                .toList();
    }

    @Transactional
    public MembroResponse adicionarMembro(UUID organizacaoUuid, MembroRequest request) {
        Membro gestor = organizacaoContexto.exigirGestaoDeMembrosDe(organizacaoUuid);
        Organizacao organizacao = gestor.getOrganizacao();

        // Limitacao consciente desta iteracao (ADR-0005): so da para adicionar
        // quem ja tem conta no CutFlow. Convite por e-mail (para quem ainda nao
        // tem conta) fica como melhoria futura.
        String email = AuthService.normalizarEmail(request.email());
        Usuario alvo = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException(
                        "Essa pessoa precisa criar uma conta no CutFlow antes de ser adicionada à equipe"));

        if (membroRepository.existsByUsuarioIdAndOrganizacaoId(alvo.getId(), organizacao.getId())) {
            throw new BusinessRuleException("Essa pessoa já faz parte da equipe");
        }

        // OWNER e' unico e definido na criacao - convite entra no maximo como ADMIN.
        PapelMembro papel = request.papel() == null || request.papel() == PapelMembro.OWNER
                ? PapelMembro.MEMBRO : request.papel();

        Membro membro = new Membro();
        membro.setUsuario(alvo);
        membro.setOrganizacao(organizacao);
        membro.setPapel(papel);
        return MembroResponse.from(membroRepository.save(membro));
    }

    @Transactional
    public void removerMembro(UUID organizacaoUuid, UUID membroUuid) {
        Membro gestor = organizacaoContexto.exigirGestaoDeMembrosDe(organizacaoUuid);

        Membro alvo = membroRepository.findByUuidAndOrganizacaoId(membroUuid, gestor.getOrganizacao().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));

        if (alvo.getPapel() == PapelMembro.OWNER) {
            throw new BusinessRuleException("O dono da organização não pode ser removido");
        }
        membroRepository.delete(alvo);
    }
}
