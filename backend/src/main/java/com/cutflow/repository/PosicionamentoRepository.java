package com.cutflow.repository;

import com.cutflow.entity.Posicionamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosicionamentoRepository extends JpaRepository<Posicionamento, Long> {
    List<Posicionamento> findByChapaUtilizadaIdOrderByNumeroEtiquetaAsc(Long chapaUtilizadaId);
}
