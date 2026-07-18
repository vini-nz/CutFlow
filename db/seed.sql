-- ============================================================================
-- CutFlow — dados de demonstração
-- Reproduz o exemplo de armário de cozinha usado na entrevista com o
-- marceneiro-piloto (doc introdução), para servir de primeiro caso de teste.
-- ============================================================================

INSERT INTO projetos (nome, cliente) VALUES
    ('Armário Cozinha João', 'João');

-- Uma chapa por combinacao espessura+acabamento (ADR-0004): as pecas de 15mm
-- do projeto demo existem nos dois acabamentos, entao ha duas chapas de 15mm.
INSERT INTO chapas (projeto_id, largura_mm, altura_mm, espessura_mm, tipo_acabamento, material, kerf_mm, margem_borda_mm)
SELECT id, 1840, 2740, 15, 'LISO', 'MDF', 4, 6 FROM projetos WHERE nome = 'Armário Cozinha João'
UNION ALL
SELECT id, 1840, 2740, 15, 'COM_VEIO', 'MDF', 4, 6 FROM projetos WHERE nome = 'Armário Cozinha João';

INSERT INTO pecas (projeto_id, nome, altura_mm, largura_mm, espessura_mm, quantidade, tipo_acabamento)
SELECT id, 'Lateral', 2200, 550, 15, 2, 'COM_VEIO' FROM projetos WHERE nome = 'Armário Cozinha João'
UNION ALL
SELECT id, 'Fundo', 700, 550, 15, 1, 'LISO' FROM projetos WHERE nome = 'Armário Cozinha João'
UNION ALL
SELECT id, 'Tampa', 700, 550, 15, 1, 'LISO' FROM projetos WHERE nome = 'Armário Cozinha João'
UNION ALL
SELECT id, 'Prateleira', 668, 500, 15, 5, 'LISO' FROM projetos WHERE nome = 'Armário Cozinha João'
UNION ALL
SELECT id, 'Porta', 2200, 350, 15, 2, 'COM_VEIO' FROM projetos WHERE nome = 'Armário Cozinha João';
