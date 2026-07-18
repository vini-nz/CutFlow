-- ============================================================================
-- Migração 0002 — Chapa por combinação espessura+acabamento (ADR-0004)
-- Aplicar em bancos criados com o DDL anterior a esta versão.
-- Em ambiente de desenvolvimento, alternativa mais simples:
--   docker compose down -v && docker compose up --build
-- (recria o volume e roda o DDL/seed novos do zero).
-- ============================================================================

BEGIN;

-- 1. Planos existentes foram gerados com a regra antiga (veio e liso podiam
--    dividir chapa) — são inválidos por definição. Descartar antes de mexer
--    no schema evita conflito com as novas FKs.
DELETE FROM planos_de_corte;

-- 2. Acabamento na chapa (LISO por padrão para linhas existentes).
ALTER TABLE chapas
    ADD COLUMN tipo_acabamento VARCHAR(20) NOT NULL DEFAULT 'LISO'
        CHECK (tipo_acabamento IN ('LISO', 'COM_VEIO'));

-- 3. Unicidade passa a ser por combinação, não só por espessura.
DROP INDEX IF EXISTS uq_chapas_projeto_espessura;
CREATE UNIQUE INDEX uq_chapas_projeto_espessura_acabamento
    ON chapas(projeto_id, espessura_mm, tipo_acabamento);

-- 4. Cascades de segurança: plano é dado derivado, nunca pode bloquear a
--    exclusão de uma peça ou chapa (a camada de serviço já invalida os
--    planos em toda mutação; isto é a rede de segurança no banco).
ALTER TABLE posicionamentos
    DROP CONSTRAINT posicionamentos_peca_id_fkey,
    ADD CONSTRAINT posicionamentos_peca_id_fkey
        FOREIGN KEY (peca_id) REFERENCES pecas(id) ON DELETE CASCADE;

ALTER TABLE chapas_utilizadas
    DROP CONSTRAINT chapas_utilizadas_chapa_id_fkey,
    ADD CONSTRAINT chapas_utilizadas_chapa_id_fkey
        FOREIGN KEY (chapa_id) REFERENCES chapas(id) ON DELETE CASCADE;

COMMIT;
