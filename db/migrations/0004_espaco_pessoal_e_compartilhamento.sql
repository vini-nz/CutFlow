-- ============================================================================
-- Migração 0004 — Espaço pessoal + compartilhamento direto de projeto (ADR-0006)
-- Aplicar em bancos criados antes desta versão.
-- Em desenvolvimento: docker compose down -v && docker compose up --build.
-- ============================================================================

BEGIN;

-- 1. Espaço pessoal: organizações existentes ficam como "de verdade" (pessoal
--    = FALSE). As novas contas passam a ganhar um espaço pessoal no cadastro.
ALTER TABLE organizacoes ADD COLUMN pessoal BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Colaboradores diretos de um projeto (sem organização).
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
    BEFORE UPDATE ON colaboradores_projeto FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. Convites / links de compartilhamento (email_alvo NULL = link reutilizável).
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
    BEFORE UPDATE ON convites_projeto FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;
