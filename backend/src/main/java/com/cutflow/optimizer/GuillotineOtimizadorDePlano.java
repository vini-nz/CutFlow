package com.cutflow.optimizer;

import com.cutflow.enums.TipoAcabamento;
import com.cutflow.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Heuristica Guillotine (doc secao 6): cortes sempre retos, de ponta a ponta,
 * reproduzindo o corte real da esquadrejadeira manual do marceneiro-piloto.
 * Nao busca o otimo matematico (o problema e NP-dificil, doc secao 6.1) - a
 * meta e 90-95% de aproveitamento em segundos.
 *
 * Regras de negocio aplicadas (doc secao 6.3):
 * - pecas mais compridas entram primeiro (ordenacao por maior dimensao);
 * - so pecas LISO podem rotacionar durante o encaixe, COM_VEIO mantem a
 *   orientacao original;
 * - kerf e descontado em toda divisao de espaço livre gerada por um corte;
 * - margem de borda e aplicada uma vez, no retangulo inicial da chapa.
 */
@Component
public class GuillotineOtimizadorDePlano implements OtimizadorDePlano {

    @Override
    public ResultadoOtimizacao gerar(ParametrosChapa chapaParams, List<PecaParaEmpacotar> pecas) {
        if (pecas == null || pecas.isEmpty()) {
            throw new BusinessRuleException("Nenhuma peça informada para gerar o plano de corte");
        }

        int larguraUtil = chapaParams.larguraMm() - 2 * chapaParams.margemBordaMm();
        int alturaUtil = chapaParams.alturaMm() - 2 * chapaParams.margemBordaMm();
        if (larguraUtil <= 0 || alturaUtil <= 0) {
            throw new BusinessRuleException("Margem de borda excede o tamanho da chapa");
        }

        List<UnidadePeca> unidades = expandirEValidar(pecas, larguraUtil, alturaUtil);
        unidades.sort(Comparator.comparingInt(UnidadePeca::comprimento).reversed());

        List<ChapaEmpacotada> chapasResultado = new ArrayList<>();
        List<UnidadePeca> restantes = unidades;
        int numeroEtiqueta = 1;

        for (int numeroChapa = 1; !restantes.isEmpty(); numeroChapa++) {
            if (numeroChapa > chapaParams.quantidadeDisponivel()) {
                throw new BusinessRuleException(
                        "Chapas insuficientes: seriam necessárias pelo menos %d chapas, mas há apenas %d disponíveis."
                                .formatted(numeroChapa, chapaParams.quantidadeDisponivel()));
            }

            ResultadoBin bin = empacotarUmaChapa(restantes, larguraUtil, alturaUtil,
                    chapaParams.margemBordaMm(), chapaParams.kerfMm(), numeroEtiqueta);

            if (bin.posicionamentos().isEmpty()) {
                // Nao deveria acontecer: expandirEValidar ja garante que toda
                // peca cabe sozinha numa chapa vazia. Se ainda assim nenhuma
                // peca coube, ha um bug no encaixe - falhar alto em vez de
                // entrar em loop infinito abrindo chapas vazias.
                throw new IllegalStateException("Falha interna no otimizador: nenhuma peça encaixada em chapa nova");
            }

            long areaUtilizada = bin.posicionamentos().stream()
                    .mapToLong(p -> (long) p.larguraMm() * p.alturaMm())
                    .sum();

            chapasResultado.add(new ChapaEmpacotada(
                    numeroChapa,
                    bin.posicionamentos(),
                    bin.sobras(),
                    areaUtilizada,
                    (long) larguraUtil * alturaUtil));

            restantes = bin.naoAlocadas();
            numeroEtiqueta = bin.proximoNumeroEtiqueta();
        }

        return new ResultadoOtimizacao(chapasResultado);
    }

    private List<UnidadePeca> expandirEValidar(List<PecaParaEmpacotar> pecas, int larguraUtil, int alturaUtil) {
        List<UnidadePeca> unidades = new ArrayList<>();
        for (PecaParaEmpacotar peca : pecas) {
            boolean podeRotacionar = peca.tipoAcabamento() == TipoAcabamento.LISO;
            boolean cabeSemGirar = peca.larguraMm() <= larguraUtil && peca.alturaMm() <= alturaUtil;
            boolean cabeGirada = podeRotacionar && peca.alturaMm() <= larguraUtil && peca.larguraMm() <= alturaUtil;

            if (!cabeSemGirar && !cabeGirada) {
                throw new BusinessRuleException(
                        "Peça \"%s\" (%dx%dmm) não cabe na área útil da chapa (%dx%dmm)"
                                .formatted(peca.nome(), peca.larguraMm(), peca.alturaMm(), larguraUtil, alturaUtil));
            }

            for (int i = 0; i < peca.quantidade(); i++) {
                unidades.add(new UnidadePeca(peca.pecaId(), peca.nome(), peca.larguraMm(), peca.alturaMm(), podeRotacionar));
            }
        }
        return unidades;
    }

    private ResultadoBin empacotarUmaChapa(List<UnidadePeca> candidatas, int larguraUtil, int alturaUtil,
                                            int margemBordaMm, int kerfMm, int numeroEtiquetaInicial) {
        List<FreeRect> livres = new ArrayList<>();
        livres.add(new FreeRect(margemBordaMm, margemBordaMm, larguraUtil, alturaUtil));

        List<PosicionamentoCalculado> posicionadas = new ArrayList<>();
        List<UnidadePeca> naoAlocadas = new ArrayList<>();
        int numeroEtiqueta = numeroEtiquetaInicial;

        for (UnidadePeca peca : candidatas) {
            Encaixe encaixe = encontrarMelhorEncaixe(livres, peca);
            if (encaixe == null) {
                naoAlocadas.add(peca);
                continue;
            }

            posicionadas.add(new PosicionamentoCalculado(
                    peca.pecaId(), peca.nome(), numeroEtiqueta++,
                    encaixe.rect().x(), encaixe.rect().y(),
                    encaixe.larguraOcupada(), encaixe.alturaOcupada(), encaixe.rotacionada()));

            livres.remove(encaixe.rect());
            livres.addAll(dividir(encaixe.rect(), encaixe.larguraOcupada(), encaixe.alturaOcupada(), kerfMm));
            livres = podar(livres);
        }

        List<SobraCalculada> sobras = livres.stream()
                .map(r -> new SobraCalculada(r.x(), r.y(), r.largura(), r.altura()))
                .toList();

        return new ResultadoBin(posicionadas, naoAlocadas, sobras, numeroEtiqueta);
    }

    /**
     * Best Area Fit: entre todos os retangulos livres onde a peca cabe (com
     * ou sem rotacao, conforme permitido), escolhe o de menor sobra de area -
     * tende a preservar retangulos grandes para pecas grandes futuras.
     */
    private Encaixe encontrarMelhorEncaixe(List<FreeRect> livres, UnidadePeca peca) {
        Encaixe melhor = null;
        long menorSobra = Long.MAX_VALUE;

        for (FreeRect rect : livres) {
            if (peca.largura() <= rect.largura() && peca.altura() <= rect.altura()) {
                long sobra = (long) rect.largura() * rect.altura() - (long) peca.largura() * peca.altura();
                if (sobra < menorSobra) {
                    menorSobra = sobra;
                    melhor = new Encaixe(rect, peca.largura(), peca.altura(), false);
                }
            }
            if (peca.podeRotacionar() && peca.altura() <= rect.largura() && peca.largura() <= rect.altura()) {
                long sobra = (long) rect.largura() * rect.altura() - (long) peca.largura() * peca.altura();
                if (sobra < menorSobra) {
                    menorSobra = sobra;
                    melhor = new Encaixe(rect, peca.altura(), peca.largura(), true);
                }
            }
        }
        return melhor;
    }

    /**
     * Guillotine split com heuristica de eixo mais curto: a divisao escolhida
     * e a que deixa o retangulo remanescente maior ser "inteiro" (largura ou
     * altura total da area livre original), reduzindo fragmentacao.
     */
    private List<FreeRect> dividir(FreeRect livre, int larguraOcupada, int alturaOcupada, int kerfMm) {
        int larguraRestante = livre.largura() - larguraOcupada - kerfMm;
        int alturaRestante = livre.altura() - alturaOcupada - kerfMm;

        List<FreeRect> novos = new ArrayList<>();
        if (larguraRestante >= alturaRestante) {
            if (larguraRestante > 0) {
                novos.add(new FreeRect(livre.x() + larguraOcupada + kerfMm, livre.y(), larguraRestante, livre.altura()));
            }
            if (alturaRestante > 0) {
                novos.add(new FreeRect(livre.x(), livre.y() + alturaOcupada + kerfMm, larguraOcupada, alturaRestante));
            }
        } else {
            if (alturaRestante > 0) {
                novos.add(new FreeRect(livre.x(), livre.y() + alturaOcupada + kerfMm, livre.largura(), alturaRestante));
            }
            if (larguraRestante > 0) {
                novos.add(new FreeRect(livre.x() + larguraOcupada + kerfMm, livre.y(), larguraRestante, alturaOcupada));
            }
        }
        return novos;
    }

    /**
     * Remove retangulos livres totalmente contidos em outro, para manter a
     * lista de sobras enxuta. Em caso de retangulos com area identica (ex:
     * duplicata exata), mantem apenas o de menor indice para nunca zerar a
     * lista removendo os dois lados de uma comparacao simetrica.
     */
    private List<FreeRect> podar(List<FreeRect> livres) {
        List<FreeRect> resultado = new ArrayList<>();
        for (int i = 0; i < livres.size(); i++) {
            FreeRect atual = livres.get(i);
            long areaAtual = (long) atual.largura() * atual.altura();
            boolean contido = false;
            for (int j = 0; j < livres.size(); j++) {
                if (i == j || !livres.get(j).contem(atual)) {
                    continue;
                }
                long areaOutro = (long) livres.get(j).largura() * livres.get(j).altura();
                if (areaOutro > areaAtual || (areaOutro == areaAtual && j < i)) {
                    contido = true;
                    break;
                }
            }
            if (!contido) {
                resultado.add(atual);
            }
        }
        return resultado;
    }

    private record UnidadePeca(Long pecaId, String nome, int largura, int altura, boolean podeRotacionar) {
        int comprimento() {
            return Math.max(largura, altura);
        }
    }

    private record FreeRect(int x, int y, int largura, int altura) {
        boolean contem(FreeRect outro) {
            return outro.x() >= x && outro.y() >= y
                    && outro.x() + outro.largura() <= x + largura
                    && outro.y() + outro.altura() <= y + altura;
        }
    }

    private record Encaixe(FreeRect rect, int larguraOcupada, int alturaOcupada, boolean rotacionada) {}

    private record ResultadoBin(List<PosicionamentoCalculado> posicionamentos, List<UnidadePeca> naoAlocadas,
                                 List<SobraCalculada> sobras, int proximoNumeroEtiqueta) {}
}
