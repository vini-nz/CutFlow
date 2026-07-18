package com.cutflow.repository;

import com.cutflow.entity.Sobra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SobraRepository extends JpaRepository<Sobra, Long> {
    List<Sobra> findByChapaUtilizadaIdOrderByIdAsc(Long chapaUtilizadaId);
}
