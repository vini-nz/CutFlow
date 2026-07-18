package com.cutflow.repository;

import com.cutflow.entity.Chapa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapaRepository extends JpaRepository<Chapa, Long> {
    List<Chapa> findByProjetoIdOrderByEspessuraMmAsc(Long projetoId);
    Optional<Chapa> findByUuidAndProjetoId(UUID uuid, Long projetoId);
    Optional<Chapa> findByProjetoIdAndEspessuraMm(Long projetoId, Integer espessuraMm);
    boolean existsByProjetoIdAndEspessuraMm(Long projetoId, Integer espessuraMm);
}
