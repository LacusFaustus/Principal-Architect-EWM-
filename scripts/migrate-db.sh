#!/bin/bash

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Функция для выполнения миграций
migrate_database() {
    local service=$1
    local db_name=$2
    local db_user=$3

    log_info "Running migrations for $service..."

    # Используем Liquibase для миграций
    docker-compose -f docker-compose.prod.yml run --rm $service \
        java -jar /app.jar --spring.profiles.active=migrate

    log_info "Migrations for $service completed"
}

# Основные миграции
log_info "Starting database migrations..."

migrate_database "user-service" "userdb" "user"
migrate_database "event-service" "eventdb" "event"
migrate_database "request-service" "requestdb" "request"
migrate_database "comment-service" "commentdb" "comment"
migrate_database "stats-server" "statsdb" "stats"

log_info "All migrations completed successfully!"