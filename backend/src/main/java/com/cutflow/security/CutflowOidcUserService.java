package com.cutflow.security;

import com.cutflow.entity.Usuario;
import com.cutflow.repository.UsuarioRepository;
import com.cutflow.service.EspacoPessoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vincula o login Google a um Usuario do CutFlow (ADR-0005). Executa durante o
 * fluxo OAuth2/OIDC: pega e-mail e "sub" do Google e faz upsert -
 *
 * - ja existe conta com esse googleSub -> usa ela;
 * - existe conta com o mesmo e-mail (cadastro local previo) -> vincula o
 *   googleSub a ela (mesma pessoa, um unico login);
 * - nao existe -> cria uma conta nova sem senha (so Google) + espaco pessoal
 *   automatico (ADR-0006), igual ao cadastro local.
 */
@Service
@RequiredArgsConstructor
public class CutflowOidcUserService extends OidcUserService {

    private final UsuarioRepository usuarioRepository;
    private final EspacoPessoalService espacoPessoalService;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String sub = oidcUser.getSubject();
        String nome = oidcUser.getFullName() != null ? oidcUser.getFullName() : email;

        if (email == null) {
            // Sem e-mail nao ha como identificar/mesclar a conta com seguranca.
            throw new IllegalStateException("Conta Google sem e-mail verificado");
        }

        Usuario usuario = usuarioRepository.findByGoogleSub(sub)
                .or(() -> usuarioRepository.findByEmail(email))
                .orElseGet(Usuario::new);

        boolean contaNova = usuario.getId() == null;
        if (contaNova) {
            usuario.setEmail(email);
            usuario.setNome(nome);
        }
        if (usuario.getGoogleSub() == null) {
            usuario.setGoogleSub(sub);
        }
        usuario = usuarioRepository.save(usuario);

        if (contaNova) {
            espacoPessoalService.criarPara(usuario);
        }

        return oidcUser;
    }
}
