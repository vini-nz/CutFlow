package com.cutflow.repository;

import com.cutflow.entity.Peca;
import com.cutflow.enums.TipoAcabamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, Long> {
    List<Peca> findByProjetoIdOrderByCreatedAtAsc(Long projetoId);
    Optional<Peca> findByUuidAndProjetoId(UUID uuid, Long projetoId);
    boolean existsByProjetoIdAndEspessuraMmAndTipoAcabamento(Long projetoId, Integer espessuraMm,
                                                              TipoAcabamento tipoAcabamento);
}
