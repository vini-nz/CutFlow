package com.cutflow.repository;

import com.cutflow.entity.ChapaUtilizada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapaUtilizadaRepository extends JpaRepository<ChapaUtilizada, Long> {
    List<ChapaUtilizada> findByPlanoDeCorteIdOrderByNumeroChapaAsc(Long planoDeCorteId);
}
