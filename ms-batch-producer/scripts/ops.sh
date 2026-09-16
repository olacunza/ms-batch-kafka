#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
compose=(docker compose)
if [[ -n "${ENV_FILE:-}" ]]; then compose+=(--env-file "$ENV_FILE"); fi
compose+=(-f docker-compose.yml)
sql() {
  "${compose[@]}" exec -T sqlserver sh -c 'export SQLCMDPASSWORD="$MSSQL_SA_PASSWORD"; exec /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -d reto_db -W'
}
case "${1:-help}" in
  status) "${compose[@]}" ps ;;
  logs) "${compose[@]}" logs --tail=100 producer consumer monitor ;;
  lag) "${compose[@]}" exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --describe --group record-processor-v2 ;;
  sql) sql ;;
  summary)
    sql <<'SQL'
SET NOCOUNT ON;
SELECT upload_id,status,COUNT_BIG(*) AS records FROM batch_record GROUP BY upload_id,status;
SELECT COUNT_BIG(*) AS unpublished FROM outbox_event WHERE published_at IS NULL;
SELECT COUNT_BIG(*) AS handled FROM consumed_event;
SELECT COUNT_BIG(*) AS quarantined FROM rejected_event;
SELECT TOP (20) id,topic,partition_number,record_offset,reason,rejected_at FROM rejected_event ORDER BY rejected_at DESC;
GO
SQL
    ;;
  requeue-pending)
    if [[ "${2:-}" != "--execute" ]]; then
      sql <<'SQL'
SELECT COUNT_BIG(*) AS events_to_requeue FROM outbox_event o JOIN batch_record r ON r.id=o.record_id
WHERE r.status='PENDING' AND NOT EXISTS(SELECT 1 FROM consumed_event c WHERE c.event_id=o.event_id);
GO
SQL
      echo 'Vista previa. Para aplicar: scripts/ops.sh requeue-pending --execute'
    else
      # Stop the relay while changing publication state; event IDs remain stable.
      "${compose[@]}" stop producer
      if ! sql < infra/requeue-pending.sql; then
        "${compose[@]}" up -d producer
        exit 1
      fi
      "${compose[@]}" up -d producer
    fi
    ;;
  reset-dev)
    if [[ "${2:-}" != "--confirm-delete-all" ]]; then
      echo 'Esto elimina SQL Server, Kafka y archivos de carga del proyecto Compose actual.'
      echo 'Para borrar todos sus datos: scripts/ops.sh reset-dev --confirm-delete-all'
      exit 2
    fi
    "${compose[@]}" down --volumes --remove-orphans
    ;;
  *) echo 'Uso: scripts/ops.sh {status|logs|lag|summary|sql|requeue-pending [--execute]|reset-dev --confirm-delete-all}' ;;
esac
