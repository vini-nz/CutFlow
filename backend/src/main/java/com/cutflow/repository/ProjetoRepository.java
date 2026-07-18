package com.cutflow.repository;

import com.cutflow.entity.Projeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    Optional<Projeto> findByUuid(UUID uuid);
    Page<Projeto> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
