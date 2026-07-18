-- ============================================================================
-- CutFlow — DDL PostgreSQL (MVP)
-- Escopo: tabelas do MVP (Projeto, Chapa, Peca, Plano de Corte, Sobra)
-- SGBD: PostgreSQL 15+
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- necessário para gen_random_uuid()

-- ----------------------------------------------------------------------------
-- Função utilitária: atualiza updated_at automaticamente em qualquer UPDATE
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- DOMÍNIO: PROJETO
-- ============================================================================

CREATE TABLE projetos (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    cliente       VARCHAR(150),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_projetos_updated_at
    BEFORE UPDATE ON projetos
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- DOMÍNIO: CHAPAS E PEÇAS
-- ============================================================================

-- Uma chapa por espessura dentro do projeto: o marceneiro-piloto usa sempre
-- a mesma medida de chapa (274x184cm) e só varia a espessura (6/15/18/25mm) -
-- confirmado na entrevista (doc secao 3.1). A constraint abaixo trava essa
-- regra no banco.
CREATE TABLE chapas (
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    projeto_id             BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    largura_mm             INTEGER NOT NULL CHECK (largura_mm > 0),
    altura_mm              INTEGER NOT NULL CHECK (altura_mm > 0),
    espessura_mm           INTEGER NOT NULL CHECK (espessura_mm IN (6, 15, 18, 25)),
    material               VARCHAR(30) NOT NULL DEFAULT 'MDF',
    quantidade_disponivel  INTEGER NOT NULL CHECK (quantidade_disponivel >= 0),
    kerf_mm                INTEGER NOT NULL DEFAULT 4 CHECK (kerf_mm >= 0),
    margem_borda_mm        INTEGER NOT NULL DEFAULT 6 CHECK (margem_borda_mm >= 0),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chapas_projeto_id ON chapas(projeto_id);
CREATE UNIQUE INDEX uq_chapas_projeto_espessura ON chapas(projeto_id, espessura_mm);

CREATE TRIGGER trg_chapas_updated_at
    BEFORE UPDATE ON chapas
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ----------------------------------------------------------------------------

CREATE TABLE pecas (
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    projeto_id        BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    nome              VARCHAR(100) NOT NULL,
    altura_mm         INTEGER NOT NULL CHECK (altura_mm > 0),
    largura_mm        INTEGER NOT NULL CHECK (largura_mm > 0),
    espessura_mm      INTEGER NOT NULL CHECK (espessura_mm IN (6, 15, 18, 25)),
    quantidade        INTEGER NOT NULL CHECK (quantidade > 0),
    tipo_acabamento   VARCHAR(20) NOT NULL DEFAULT 'LISO'
                      CHECK (tipo_acabamento IN ('LISO', 'COM_VEIO')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pecas_projeto_id ON pecas(projeto_id);

CREATE TRIGGER trg_pecas_updated_at
    BEFORE UPDATE ON pecas
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- DOMÍNIO: PLANO DE CORTE (resultado do algoritmo de otimização)
-- ============================================================================

-- Cada geracao de plano cria um novo registro (sem versionamento/historico
-- de comparacao entre planos no MVP - ver docs/architecture.md e roadmap V2).
CREATE TABLE planos_de_corte (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    projeto_id                  BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    total_chapas_utilizadas     INTEGER NOT NULL CHECK (total_chapas_utilizadas > 0),
    percentual_aproveitamento   NUMERIC(5,2) NOT NULL,
    percentual_desperdicio      NUMERIC(5,2) NOT NULL,
    gerado_em                   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_planos_de_corte_projeto_id ON planos_de_corte(projeto_id);

-- ----------------------------------------------------------------------------

CREATE TABLE chapas_utilizadas (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    plano_de_corte_id           BIGINT NOT NULL REFERENCES planos_de_corte(id) ON DELETE CASCADE,
    chapa_id                    BIGINT NOT NULL REFERENCES chapas(id),
    numero_chapa                INTEGER NOT NULL CHECK (numero_chapa > 0),
    area_desperdicada_mm2       BIGINT NOT NULL CHECK (area_desperdicada_mm2 >= 0),
    percentual_aproveitamento   NUMERIC(5,2) NOT NULL
);

CREATE INDEX idx_chapas_utilizadas_plano_id ON chapas_utilizadas(plano_de_corte_id);

-- ----------------------------------------------------------------------------

CREATE TABLE posicionamentos (
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    chapa_utilizada_id     BIGINT NOT NULL REFERENCES chapas_utilizadas(id) ON DELETE CASCADE,
    peca_id                BIGINT NOT NULL REFERENCES pecas(id),
    numero_etiqueta        INTEGER NOT NULL CHECK (numero_etiqueta > 0),
    x_mm                   INTEGER NOT NULL CHECK (x_mm >= 0),
    y_mm                   INTEGER NOT NULL CHECK (y_mm >= 0),
    largura_mm             INTEGER NOT NULL CHECK (largura_mm > 0),
    altura_mm              INTEGER NOT NULL CHECK (altura_mm > 0),
    rotacionada            BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_posicionamentos_chapa_utilizada_id ON posicionamentos(chapa_utilizada_id);
CREATE INDEX idx_posicionamentos_peca_id ON posicionamentos(peca_id);

-- ----------------------------------------------------------------------------

-- Retalho registrado ao final do encaixe de cada chapa, sem tamanho minimo
-- (doc secao 3.3 - confirmado que sobra de projeto e reaproveitada mesmo
-- pequena). sentido_veio fica reservado para o reaproveitamento automatico
-- de V2 e não é populado pelo algoritmo do MVP.
CREATE TABLE sobras (
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    chapa_utilizada_id     BIGINT NOT NULL REFERENCES chapas_utilizadas(id) ON DELETE CASCADE,
    x_mm                   INTEGER NOT NULL CHECK (x_mm >= 0),
    y_mm                   INTEGER NOT NULL CHECK (y_mm >= 0),
    largura_mm             INTEGER NOT NULL CHECK (largura_mm > 0),
    altura_mm              INTEGER NOT NULL CHECK (altura_mm > 0),
    sentido_veio           VARCHAR(20)
);

CREATE INDEX idx_sobras_chapa_utilizada_id ON sobras(chapa_utilizada_id);
