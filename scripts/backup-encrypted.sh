#!/bin/bash

set -e

BACKUP_DIR="/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30
ENCRYPTION_KEY="${BACKUP_ENCRYPTION_KEY:-your-encryption-key}"
GPG_RECIPIENT="${GPG_RECIPIENT:-your-email@example.com}"

log_info() {
    echo "[INFO] $1"
}

# Backup и шифрование
backup_and_encrypt() {
    local db_name=$1
    local db_user=$2
    local db_host=$3

    log_info "Backing up and encrypting database: $db_name"

    # Создаем дамп
    pg_dump -h "$db_host" -U "$db_user" -d "$db_name" \
        | gzip > "${BACKUP_DIR}/${db_name}_${TIMESTAMP}.sql.gz"

    # Шифруем дамп
    gpg --batch --yes --recipient "$GPG_RECIPIENT" \
        --encrypt "${BACKUP_DIR}/${db_name}_${TIMESTAMP}.sql.gz"

    # Удаляем незашифрованный дамп
    rm "${BACKUP_DIR}/${db_name}_${TIMESTAMP}.sql.gz"

    log_info "Backup encrypted: ${db_name}_${TIMESTAMP}.sql.gz.gpg"
}

# Upload to S3 с шифрованием
upload_to_s3_encrypted() {
    log_info "Uploading encrypted backups to S3"
    aws s3 sync "${BACKUP_DIR}" "s3://ewm-backups/${TIMESTAMP}/" \
        --exclude "*.tmp" \
        --storage-class STANDARD_IA \
        --sse AES256
}

# Clean old backups
clean_old_backups() {
    log_info "Removing backups older than ${RETENTION_DAYS} days"
    find "${BACKUP_DIR}" -name "*.sql.gz.gpg" -mtime "+${RETENTION_DAYS}" -delete
    find "${BACKUP_DIR}" -name "*.rdb" -mtime "+${RETENTION_DAYS}" -delete
}

# Main
main() {
    log_info "Starting encrypted backup process..."

    mkdir -p "${BACKUP_DIR}"

    backup_and_encrypt "userdb" "user" "user-db"
    backup_and_encrypt "eventdb" "event" "event-db"
    backup_and_encrypt "requestdb" "request" "request-db"
    backup_and_encrypt "commentdb" "comment" "comment-db"
    backup_and_encrypt "statsdb" "stats" "stats-db"

    upload_to_s3_encrypted
    clean_old_backups

    log_info "Encrypted backup completed successfully"
}

main "$@"