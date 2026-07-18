package com.cutflow.service;

import com.cutflow.dto.peca.PecaRequest;
import com.cutflow.entity.Peca;
import com.cutflow.entity.Projeto;
import com.cutflow.enums.TipoAcabamento;
import com.cutflow.repository.PecaRepository;
import com.cutflow.repository.PlanoDeCorteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o efeito colateral central da ADR-0004 no ciclo de vida da Peca:
 * toda mutacao invalida os planos de corte do projeto. Sem isso, remover
 * uma peca que aparece num plano antigo estoura a FK de posicionamentos e o
 * usuario fica "travado" no primeiro plano gerado (bug reportado apos a
 * apresentacao ao marceneiro-piloto).
 */
@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    @Mock private PecaRepository pecaRepository;
    @Mock private PlanoDeCorteRepository planoDeCorteRepository;
    @Mock private ProjetoService projetoService;
    @Mock private ChapaService chapaService;

    private PecaService pecaService;

    private Projeto projeto;

    @BeforeEach
    void setUp() {
        pecaService = new PecaService(pecaRepository, planoDeCorteRepository, projetoService, chapaService);

        projeto = new Projeto();
        projeto.setId(1L);
        projeto.setUuid(UUID.randomUUID());
        projeto.setNome("Armário Cozinha João");
    }

    private PecaRequest request(TipoAcabamento tipoAcabamento) {
        return new PecaRequest("Porta", 2200, 350, 15, 2, tipoAcabamento);
    }

    @Test
    void create_garanteChapaDaCombinacaoEInvalidaPlanosAntigos() {
        when(projetoService.findOrThrow(projeto.getUuid())).thenReturn(projeto);
        when(pecaRepository.save(any())).thenAnswer(inv -> {
            Peca salva = inv.getArgument(0);
            salva.setUuid(UUID.randomUUID());
            return salva;
        });

        pecaService.create(projeto.getUuid(), request(TipoAcabamento.COM_VEIO));

        verify(chapaService).garantirChapa(1L, projeto, 15, TipoAcabamento.COM_VEIO);
        verify(planoDeCorteRepository).deleteByProjetoId(1L);
    }

    @Test
    void delete_invalidaPlanosANTESDeRemoverAPeca() {
        // A ordem importa: posicionamentos de planos antigos referenciam a
        // peca; deletar a peca primeiro violaria a FK (mesmo com o cascade
        // de seguranca no banco, a regra de negocio e descartar o plano).
        Peca peca = new Peca();
        peca.setId(7L);
        peca.setUuid(UUID.randomUUID());
        peca.setProjeto(projeto);

        when(projetoService.findOrThrow(projeto.getUuid())).thenReturn(projeto);
        when(pecaRepository.findByUuidAndProjetoId(peca.getUuid(), 1L)).thenReturn(Optional.of(peca));

        pecaService.delete(projeto.getUuid(), peca.getUuid());

        InOrder ordem = inOrder(planoDeCorteRepository, pecaRepository);
        ordem.verify(planoDeCorteRepository).deleteByProjetoId(1L);
        ordem.verify(pecaRepository).delete(peca);
    }
}
