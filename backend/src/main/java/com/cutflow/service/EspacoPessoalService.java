package com.cutflow.service;

import com.cutflow.entity.Membro;
import com.cutflow.entity.Organizacao;
import com.cutflow.entity.Usuario;
import com.cutflow.enums.PapelMembro;
import com.cutflow.repository.MembroRepository;
import com.cutflow.repository.OrganizacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o espaco pessoal de um usuario no cadastro (ADR-0006): uma Organizacao
 * marcada como pessoal, com o proprio usuario como unico membro (OWNER). E' o
 * que faz o MEI solo cair direto em "criar projeto" sem nunca ver uma tela de
 * "criar empresa". Chamado por AuthService (cadastro local) e por
 * CutflowOidcUserService (primeiro login Google).
 */
@Service
@RequiredArgsConstructor
public class EspacoPessoalService {

    static final String NOME_PADRAO = "Meus projetos";

    private final OrganizacaoRepository organizacaoRepository;
    private final MembroRepository membroRepository;

    @Transactional
    public Organizacao criarPara(Usuario usuario) {
        Organizacao organizacao = new Organizacao();
        organizacao.setNome(NOME_PADRAO);
        organizacao.setPessoal(true);
        organizacao = organizacaoRepository.save(organizacao);

        Membro membro = new Membro();
        membro.setUsuario(usuario);
        membro.setOrganizacao(organizacao);
        membro.setPapel(PapelMembro.OWNER);
        membroRepository.save(membro);

        return organizacao;
    }
}
