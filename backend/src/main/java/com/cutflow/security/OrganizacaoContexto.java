package com.cutflow.security;

import com.cutflow.entity.Membro;
import com.cutflow.entity.Organizacao;
import com.cutflow.entity.Usuario;
import com.cutflow.exception.AcessoNegadoException;
import com.cutflow.exception.NaoAutenticadoException;
import com.cutflow.exception.SemOrganizacaoException;
import com.cutflow.repository.MembroRepository;
import com.cutflow.repository.OrganizacaoRepository;
import com.cutflow.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Resolve, a cada requisicao, QUEM e' o usuario logado e QUAL organizacao esta
 * ativa (o "workspace") - o ponto unico onde o multi-tenant e' aplicado
 * (ADR-0005).
 *
 * A organizacao ativa fica na sessao HTTP (nao na URL nem no corpo), e a
 * pertinencia e' revalidada a cada chamada: se o usuario deixou de ser membro,
 * ou a sessao aponta para uma organizacao invalida, cai para a primeira
 * organizacao dele. Assim, ProjetoService escopando por organizacaoAtiva()
 * protege transitivamente pecas/chapas/planos, que sempre resolvem o projeto
 * via ProjetoService.
 */
@Component
@RequiredArgsConstructor
public class OrganizacaoContexto {

    static final String SESSION_ATTR_ORG_ATIVA = "cutflow.organizacaoAtiva";

    private final UsuarioRepository usuarioRepository;
    private final MembroRepository membroRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final HttpServletRequest request;

    /** Usuario autenticado (local ou Google). Lanca 401 se nao houver sessao. */
    @Transactional(readOnly = true)
    public Usuario usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new NaoAutenticadoException("Nenhum usuário autenticado");
        }
        String email = extrairEmail(auth);
        if (email == null) {
            throw new NaoAutenticadoException("Nenhum usuário autenticado");
        }
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NaoAutenticadoException("Usuário da sessão não encontrado"));
    }

    /**
     * Organizacao ativa do workspace. Usa a da sessao se o usuario ainda for
     * membro dela; senao, cai para a primeira organizacao do usuario e a fixa
     * na sessao. Lanca SemOrganizacaoException se o usuario nao tiver nenhuma.
     */
    @Transactional(readOnly = true)
    public Organizacao organizacaoAtiva() {
        Usuario usuario = usuarioAtual();
        HttpSession session = request.getSession();

        Object atributo = session.getAttribute(SESSION_ATTR_ORG_ATIVA);
        if (atributo instanceof UUID ativaUuid
                && membroRepository.existsByUsuarioIdAndOrganizacaoUuid(usuario.getId(), ativaUuid)) {
            return organizacaoRepository.findByUuid(ativaUuid)
                    .orElseThrow(() -> new SemOrganizacaoException("Organização ativa não encontrada"));
        }

        List<Organizacao> organizacoes = membroRepository.findOrganizacoesDoUsuario(usuario.getId());
        if (organizacoes.isEmpty()) {
            throw new SemOrganizacaoException("Usuário não pertence a nenhuma organização");
        }
        Organizacao primeira = organizacoes.get(0);
        session.setAttribute(SESSION_ATTR_ORG_ATIVA, primeira.getUuid());
        return primeira;
    }

    /** Troca o workspace ativo, validando que o usuario e' membro do destino. */
    @Transactional(readOnly = true)
    public Organizacao definirOrganizacaoAtiva(UUID organizacaoUuid) {
        Usuario usuario = usuarioAtual();
        Membro membro = membroRepository.findByUsuarioIdAndOrganizacaoUuid(usuario.getId(), organizacaoUuid)
                .orElseThrow(() -> new AcessoNegadoException("Você não é membro desta organização"));
        request.getSession().setAttribute(SESSION_ATTR_ORG_ATIVA, organizacaoUuid);
        return membro.getOrganizacao();
    }

    /** Membership do usuario atual na organizacao ativa (para checar papel). */
    @Transactional(readOnly = true)
    public Membro membroAtual() {
        Usuario usuario = usuarioAtual();
        Organizacao organizacao = organizacaoAtiva();
        return membroRepository.findByUsuarioIdAndOrganizacaoId(usuario.getId(), organizacao.getId())
                .orElseThrow(() -> new AcessoNegadoException("Você não é membro desta organização"));
    }

    /** Garante que o usuario atual pode gerenciar a equipe (OWNER/ADMIN). */
    @Transactional(readOnly = true)
    public Membro exigirGestaoDeMembros() {
        Membro membro = membroAtual();
        if (!membro.getPapel().podeGerenciarMembros()) {
            throw new AcessoNegadoException("Apenas o dono ou administradores podem gerenciar a equipe");
        }
        return membro;
    }

    /**
     * Membership do usuario atual numa organizacao identificada pela URL (nao
     * necessariamente a ativa) - usado nos endpoints de gestao de equipe, que
     * agem sobre a organizacao do path.
     */
    @Transactional(readOnly = true)
    public Membro exigirMembroDe(UUID organizacaoUuid) {
        Usuario usuario = usuarioAtual();
        return membroRepository.findByUsuarioIdAndOrganizacaoUuid(usuario.getId(), organizacaoUuid)
                .orElseThrow(() -> new AcessoNegadoException("Você não é membro desta organização"));
    }

    /** Igual a exigirMembroDe, mas exige papel de gestao (OWNER/ADMIN). */
    @Transactional(readOnly = true)
    public Membro exigirGestaoDeMembrosDe(UUID organizacaoUuid) {
        Membro membro = exigirMembroDe(organizacaoUuid);
        if (!membro.getPapel().podeGerenciarMembros()) {
            throw new AcessoNegadoException("Apenas o dono ou administradores podem gerenciar a equipe");
        }
        return membro;
    }

    private String extrairEmail(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof UsuarioPrincipal usuarioPrincipal) {
            return usuarioPrincipal.getUsername();
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        return null;
    }
}
