#!/bin/bash

set -e

BACKUP_DIR="/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

log_info() {
    echo "[INFO] $1"
}

# Backup PostgreSQL databases
backup_postgres() {
    local db_name=$1
    local db_user=$2
    local db_host=$3

    log_info "Backing up database: $db_name"
    pg_dump -h "$db_host" -U "$db_user" -d "$db_name" \
        | gzip > "${BACKUP_DIR}/${db_name}_${TIMESTAMP}.sql.gz"
}

# Backup Redis
backup_redis() {
    log_info "Backing up Redis"
    kubectl exec -n production redis-0 -- redis-cli SAVE
    kubectl cp production/redis-0:/data/dump.rdb "${BACKUP_DIR}/redis_${TIMESTAMP}.rdb"
}

# Backup Kafka
backup_kafka() {
    log_info "Backing up Kafka topics"
    kubectl exec -n production kafka-0 -- kafka-dump-log --print-data-log \
        --files /var/lib/kafka/data/*.log > "${BACKUP_DIR}/kafka_${TIMESTAMP}.log"
}

# Upload to S3
upload_to_s3() {
    log_info "Uploading backups to S3"
    aws s3 sync "${BACKUP_DIR}" "s3://ewm-backups/${TIMESTAMP}/" \
        --exclude "*.tmp" \
        --storage-class STANDARD_IA
}

# Clean old backups
clean_old_backups() {
    log_info "Removing backups older than ${RETENTION_DAYS} days"
    find "${BACKUP_DIR}" -name "*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete
    find "${BACKUP_DIR}" -name "*.rdb" -mtime "+${RETENTION_DAYS}" -delete
    find "${BACKUP_DIR}" -name "*.log" -mtime "+${RETENTION_DAYS}" -delete
}

# Main
main() {
    log_info "Starting backup process..."

    backup_postgres "userdb" "user" "user-db"
    backup_postgres "eventdb" "event" "event-db"
    backup_postgres "requestdb" "request" "request-db"
    backup_postgres "commentdb" "comment" "comment-db"
    backup_postgres "statsdb" "stats" "stats-db"

    backup_redis
    backup_kafka

    upload_to_s3
    clean_old_backups

    log_info "Backup completed successfully"
}

main "$@"