package com.cutflow.service;

import com.cutflow.dto.auth.RegistroRequest;
import com.cutflow.dto.auth.SessaoResponse;
import com.cutflow.dto.auth.UsuarioResponse;
import com.cutflow.dto.organizacao.OrganizacaoResponse;
import com.cutflow.entity.Membro;
import com.cutflow.entity.Usuario;
import com.cutflow.exception.BusinessRuleException;
import com.cutflow.repository.MembroRepository;
import com.cutflow.repository.UsuarioRepository;
import com.cutflow.security.OrganizacaoContexto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cadastro local e leitura do estado da sessao (ADR-0005). O login em si e'
 * feito pelo AuthController via AuthenticationManager (Spring Security); aqui
 * ficam a criacao de conta e a montagem do SessaoResponse.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final MembroRepository membroRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizacaoContexto organizacaoContexto;
    private final EspacoPessoalService espacoPessoalService;

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Já existe uma conta com esse e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario = usuarioRepository.save(usuario);

        // Espaco pessoal automatico (ADR-0006): o usuario ja cai em "criar
        // projeto", sem nunca ver uma tela de "criar organizacao".
        espacoPessoalService.criarPara(usuario);

        return UsuarioResponse.from(usuario);
    }

    /**
     * Estado atual da sessao: usuario logado, organizacoes das quais e' membro
     * (com papel) e qual esta ativa. Sem organizacoes, organizacaoAtivaUuid e'
     * nulo - o frontend leva ao onboarding.
     */
    @Transactional(readOnly = true)
    public SessaoResponse sessaoAtual() {
        Usuario usuario = organizacaoContexto.usuarioAtual();
        List<Membro> membros = membroRepository.findByUsuarioIdOrderByCreatedAtAsc(usuario.getId());

        List<OrganizacaoResponse> organizacoes = membros.stream()
                .map(m -> OrganizacaoResponse.from(m.getOrganizacao(), m.getPapel()))
                .toList();

        UUID ativa = organizacoes.isEmpty() ? null : organizacaoContexto.organizacaoAtiva().getUuid();

        return new SessaoResponse(UsuarioResponse.from(usuario), organizacoes, ativa);
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
