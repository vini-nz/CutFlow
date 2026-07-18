package com.cutflow.service;

import com.cutflow.dto.projeto.ProjetoRequest;
import com.cutflow.dto.projeto.ProjetoResponse;
import com.cutflow.entity.Projeto;
import com.cutflow.exception.ResourceNotFoundException;
import com.cutflow.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;

    @Transactional(readOnly = true)
    public Page<ProjetoResponse> list(Pageable pageable) {
        return projetoRepository.findAllByOrderByCreatedAtDesc(pageable).map(ProjetoResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjetoResponse get(UUID uuid) {
        return ProjetoResponse.from(findOrThrow(uuid));
    }

    @Transactional
    public ProjetoResponse create(ProjetoRequest request) {
        Projeto projeto = new Projeto();
        projeto.setNome(request.nome());
        projeto.setCliente(request.cliente());
        return ProjetoResponse.from(projetoRepository.save(projeto));
    }

    @Transactional
    public ProjetoResponse update(UUID uuid, ProjetoRequest request) {
        Projeto projeto = findOrThrow(uuid);
        projeto.setNome(request.nome());
        projeto.setCliente(request.cliente());
        return ProjetoResponse.from(projetoRepository.save(projeto));
    }

    @Transactional
    public void delete(UUID uuid) {
        projetoRepository.delete(findOrThrow(uuid));
    }

    @Transactional(readOnly = true)
    public Projeto findOrThrow(UUID uuid) {
        return projetoRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));
    }
}
