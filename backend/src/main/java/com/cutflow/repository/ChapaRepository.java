package com.cutflow.repository;

import com.cutflow.entity.Chapa;
import com.cutflow.enums.TipoAcabamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapaRepository extends JpaRepository<Chapa, Long> {
    List<Chapa> findByProjetoIdOrderByEspessuraMmAscTipoAcabamentoAsc(Long projetoId);
    Optional<Chapa> findByUuidAndProjetoId(UUID uuid, Long projetoId);
    Optional<Chapa> findByProjetoIdAndEspessuraMmAndTipoAcabamento(Long projetoId, Integer espessuraMm,
                                                                    TipoAcabamento tipoAcabamento);
}
