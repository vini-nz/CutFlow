#!/bin/sh
# Backup diário do banco do CutFlow em produção. Gera um dump comprimido em
# ./backups/ e apaga backups com mais de 14 dias. Pensado para rodar via cron
# no mesmo servidor do docker-compose.prod.yml - ver docs/deploy.md.
#
# Uso manual:   ./scripts/backup-db.sh
# Uso via cron: crontab -e
#   0 3 * * *  cd /caminho/do/CutFlow && ./scripts/backup-db.sh >> backups/backup.log 2>&1

set -eu

cd "$(dirname "$0")/.."

DB_NAME="${DB_NAME:-cutflow}"
DB_USER="${DB_USER:-cutflow}"
DIAS_PARA_MANTER=14

mkdir -p backups
ARQUIVO="backups/cutflow_$(date +%Y-%m-%d_%H%M%S).sql.gz"

docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$ARQUIVO"

echo "Backup salvo em $ARQUIVO ($(du -h "$ARQUIVO" | cut -f1))"

find backups -name "cutflow_*.sql.gz" -mtime "+$DIAS_PARA_MANTER" -delete
