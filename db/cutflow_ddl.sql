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
-- DOMÍNIO: IDENTIDADE E ACESSO (ADR-0005)
-- Multi-tenant: Usuario (global) <-> Organizacao (tenant) via Membro, com
-- papel. Todo Projeto pertence a uma Organizacao; um usuario so enxerga
-- projetos das organizacoes das quais e membro.
-- ============================================================================

CREATE TABLE usuarios (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    -- Nulo para contas que so entram via Google (nunca definiram senha local).
    senha_hash    VARCHAR(100),
    -- "sub" do OIDC do Google; nulo enquanto a conta nunca entrou via Google.
    google_sub    VARCHAR(100) UNIQUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE organizacoes (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    -- Espaco pessoal (criado no cadastro) x organizacao de verdade (ADR-0006).
    pessoal       BOOLEAN NOT NULL DEFAULT FALSE,
    documento     VARCHAR(30),          -- CNPJ opcional; nao e chave de login
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_organizacoes_updated_at
    BEFORE UPDATE ON organizacoes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE membros (
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    usuario_id        BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    organizacao_id    BIGINT NOT NULL REFERENCES organizacoes(id) ON DELETE CASCADE,
    papel             VARCHAR(20) NOT NULL DEFAULT 'MEMBRO'
                      CHECK (papel IN ('OWNER', 'ADMIN', 'MEMBRO')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Um unico vinculo por (usuario, organizacao): trocar o papel edita o Membro.
CREATE UNIQUE INDEX uq_membros_usuario_organizacao ON membros(usuario_id, organizacao_id);
CREATE INDEX idx_membros_usuario_id ON membros(usuario_id);
CREATE INDEX idx_membros_organizacao_id ON membros(organizacao_id);

CREATE TRIGGER trg_membros_updated_at
    BEFORE UPDATE ON membros
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- DOMÍNIO: PROJETO
-- ============================================================================

CREATE TABLE projetos (
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organizacao_id BIGINT NOT NULL REFERENCES organizacoes(id) ON DELETE CASCADE,
    nome           VARCHAR(150) NOT NULL,
    cliente        VARCHAR(150),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_projetos_organizacao_id ON projetos(organizacao_id);

CREATE TRIGGER trg_projetos_updated_at
    BEFORE UPDATE ON projetos
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ----------------------------------------------------------------------------
-- Compartilhamento direto de projeto (ADR-0006): acesso a UM projeto sem
-- passar por organizacao - o caminho "Joaquim compartilha o plano com o
-- Carlos". Colaborador so enxerga o projeto compartilhado, nada mais da
-- organizacao dona.
-- ----------------------------------------------------------------------------

CREATE TABLE colaboradores_projeto (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    projeto_id    BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id    BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    papel         VARCHAR(20) NOT NULL DEFAULT 'VISUALIZADOR'
                  CHECK (papel IN ('EDITOR', 'VISUALIZADOR')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_colaboradores_projeto_usuario ON colaboradores_projeto(projeto_id, usuario_id);
CREATE INDEX idx_colaboradores_projeto_projeto_id ON colaboradores_projeto(projeto_id);
CREATE INDEX idx_colaboradores_projeto_usuario_id ON colaboradores_projeto(usuario_id);

CREATE TRIGGER trg_colaboradores_projeto_updated_at
    BEFORE UPDATE ON colaboradores_projeto
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Convite/link de compartilhamento. email_alvo NULL = link reutilizavel
-- (varios aceitam, vale ate revogar); preenchido = convite de uso unico para
-- aquele e-mail (marca aceito_em ao aceitar). O uuid e' o token da URL.
CREATE TABLE convites_projeto (
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    projeto_id     BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    papel          VARCHAR(20) NOT NULL DEFAULT 'VISUALIZADOR'
                   CHECK (papel IN ('EDITOR', 'VISUALIZADOR')),
    email_alvo     VARCHAR(180),
    criado_por_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    aceito_por_id  BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    aceito_em      TIMESTAMPTZ,
    revogado       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_convites_projeto_projeto_id ON convites_projeto(projeto_id);

CREATE TRIGGER trg_convites_projeto_updated_at
    BEFORE UPDATE ON convites_projeto
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- DOMÍNIO: CHAPAS E PEÇAS
-- ============================================================================

-- Uma chapa por combinacao espessura+acabamento dentro do projeto: o
-- marceneiro-piloto usa sempre a mesma medida de chapa (274x184cm), variando
-- a espessura (6/15/18/25mm) - confirmado na entrevista (doc secao 3.1). O
-- acabamento (LISO/COM_VEIO) e caracteristica fisica que ja vem de fabrica
-- na chapa (ADR-0004): peca com veio so sai de chapa com veio, entao a chapa
-- e identificada pela combinacao, nunca so pela espessura. A constraint
-- abaixo trava essa regra no banco.
CREATE TABLE chapas (
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    projeto_id             BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    largura_mm             INTEGER NOT NULL CHECK (largura_mm > 0),
    altura_mm              INTEGER NOT NULL CHECK (altura_mm > 0),
    espessura_mm           INTEGER NOT NULL CHECK (espessura_mm IN (6, 15, 18, 25)),
    tipo_acabamento        VARCHAR(20) NOT NULL DEFAULT 'LISO'
                           CHECK (tipo_acabamento IN ('LISO', 'COM_VEIO')),
    material               VARCHAR(30) NOT NULL DEFAULT 'MDF',
    kerf_mm                INTEGER NOT NULL DEFAULT 4 CHECK (kerf_mm >= 0),
    margem_borda_mm        INTEGER NOT NULL DEFAULT 6 CHECK (margem_borda_mm >= 0),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chapas_projeto_id ON chapas(projeto_id);
CREATE UNIQUE INDEX uq_chapas_projeto_espessura_acabamento
    ON chapas(projeto_id, espessura_mm, tipo_acabamento);

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

-- chapa_id/peca_id (abaixo) cascateiam de proposito: um plano e uma foto
-- derivada de pecas/chapas no momento da geracao. A camada de servico ja
-- descarta os planos do projeto em toda mutacao de peca/chapa (ADR-0004);
-- o cascade e a rede de seguranca no banco para nunca bloquear a exclusao
-- de uma peca/chapa por causa de um plano obsoleto.
CREATE TABLE chapas_utilizadas (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    plano_de_corte_id           BIGINT NOT NULL REFERENCES planos_de_corte(id) ON DELETE CASCADE,
    chapa_id                    BIGINT NOT NULL REFERENCES chapas(id) ON DELETE CASCADE,
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
    peca_id                BIGINT NOT NULL REFERENCES pecas(id) ON DELETE CASCADE,
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
