package com.cutflow.repository;

import com.cutflow.entity.Membro;
import com.cutflow.entity.Organizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembroRepository extends JpaRepository<Membro, Long> {

    // Vinculos do usuario (para montar o seletor de workspace e a sessao),
    // mais antigos primeiro para uma ordem estavel. Usado em contexto
    // transacional, onde acessar membro.organizacao (lazy) e' seguro.
    List<Membro> findByUsuarioIdOrderByCreatedAtAsc(Long usuarioId);

    // Organizacoes do usuario ja materializadas - usado pelo OrganizacaoContexto
    // fora de transacao, sem depender de lazy loading.
    @Query("select m.organizacao from Membro m where m.usuario.id = :usuarioId order by m.createdAt asc")
    List<Organizacao> findOrganizacoesDoUsuario(@Param("usuarioId") Long usuarioId);

    // Membership do usuario numa organizacao - base do controle de acesso:
    // quem nao tem Membro na organizacao ativa nao enxerga nada dela.
    Optional<Membro> findByUsuarioIdAndOrganizacaoId(Long usuarioId, Long organizacaoId);
    Optional<Membro> findByUsuarioIdAndOrganizacaoUuid(Long usuarioId, UUID organizacaoUuid);
    boolean existsByUsuarioIdAndOrganizacaoUuid(Long usuarioId, UUID organizacaoUuid);
    boolean existsByUsuarioIdAndOrganizacaoId(Long usuarioId, Long organizacaoId);

    // Membros de uma organizacao (tela de gestao de equipe).
    List<Membro> findByOrganizacaoIdOrderByCreatedAtAsc(Long organizacaoId);
    Optional<Membro> findByUuidAndOrganizacaoId(UUID uuid, Long organizacaoId);
}
