-- ============================================================================
-- Migração 0003 — Multi-tenant + autenticação (ADR-0005)
-- Aplicar em bancos criados antes desta versão.
-- Em desenvolvimento, alternativa mais simples:
--   docker compose down -v && docker compose up --build
-- (recria o volume e roda o DDL/seed novos do zero).
--
-- Esta migração cria as tabelas de identidade/acesso, uma organização e um
-- usuário "de migração" (OWNER) e vincula TODOS os projetos existentes a essa
-- organização — para que nada fique órfão quando organizacao_id virar NOT NULL.
-- Login de migração:  admin@cutflow.local  /  demo1234  (TROQUE em produção!)
-- ============================================================================

BEGIN;

-- 1. Tabelas de identidade/acesso ------------------------------------------------
CREATE TABLE usuarios (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    senha_hash    VARCHAR(100),
    google_sub    VARCHAR(100) UNIQUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON usuarios FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE organizacoes (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    documento     VARCHAR(30),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_organizacoes_updated_at
    BEFORE UPDATE ON organizacoes FOR EACH ROW EXECUTE FUNCTION set_updated_at();

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
CREATE UNIQUE INDEX uq_membros_usuario_organizacao ON membros(usuario_id, organizacao_id);
CREATE INDEX idx_membros_usuario_id ON membros(usuario_id);
CREATE INDEX idx_membros_organizacao_id ON membros(organizacao_id);
CREATE TRIGGER trg_membros_updated_at
    BEFORE UPDATE ON membros FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Organização + usuário de migração ------------------------------------------
INSERT INTO organizacoes (nome) VALUES ('Marcenaria (migração)');
INSERT INTO usuarios (nome, email, senha_hash) VALUES
    ('Administrador', 'admin@cutflow.local',
     '$2b$10$vpgLZRCP21TyaVaOmYNt7.tykyeqqs.HS1jUNDdtgo9w7wWw2.qn2');
INSERT INTO membros (usuario_id, organizacao_id, papel)
SELECT u.id, o.id, 'OWNER'
FROM usuarios u, organizacoes o
WHERE u.email = 'admin@cutflow.local' AND o.nome = 'Marcenaria (migração)';

-- 3. projetos.organizacao_id: adiciona, backfill, trava NOT NULL -----------------
ALTER TABLE projetos ADD COLUMN organizacao_id BIGINT REFERENCES organizacoes(id) ON DELETE CASCADE;

UPDATE projetos SET organizacao_id = (SELECT id FROM organizacoes WHERE nome = 'Marcenaria (migração)')
WHERE organizacao_id IS NULL;

ALTER TABLE projetos ALTER COLUMN organizacao_id SET NOT NULL;
CREATE INDEX idx_projetos_organizacao_id ON projetos(organizacao_id);

COMMIT;
