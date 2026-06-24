#!/bin/bash

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 1. Создание пользователей
log_info "Creating test users..."

# Создаем пользователей через админ API
for i in {1..5}; do
    curl -s -X POST "${BASE_URL}/admin/users" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"user${i}@example.com\",\"name\":\"Test User ${i}\"}" \
        > /dev/null
    log_info "Created user${i}@example.com"
done

# 2. Создание категорий
log_info "Creating categories..."

categories=("Concert" "Festival" "Exhibition" "Workshop" "Conference")
for category in "${categories[@]}"; do
    curl -s -X POST "${BASE_URL}/admin/categories" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"${category}\"}" \
        > /dev/null
    log_info "Created category: ${category}"
done

# 3. Создание событий
log_info "Creating events..."

for i in {1..10}; do
    curl -s -X POST "${BASE_URL}/users/1/events" \
        -H "Content-Type: application/json" \
        -H "X-EWM-USER-ID: 1" \
        -d "{
            \"annotation\": \"Test event annotation ${i} - at least 20 characters long\",
            \"category\": 1,
            \"description\": \"Detailed description of test event ${i} - at least 20 characters and up to 7000 characters\",
            \"eventDate\": \"2026-12-31 20:00:00\",
            \"location\": {
                \"lat\": 55.7558,
                \"lon\": 37.6173
            },
            \"paid\": false,
            \"participantLimit\": 100,
            \"requestModeration\": true,
            \"title\": \"Test Event ${i}\"
        }" \
        > /dev/null
    log_info "Created event ${i}"
done

log_info "Test data creation complete! ✅"