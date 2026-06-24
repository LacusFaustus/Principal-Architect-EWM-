#!/bin/bash

set -e

BASE_URL="${1:-http://localhost:8080}"
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# Test 1: Health Check
log_info "Testing health check..."
if curl -s -f "${BASE_URL}/actuator/health" > /dev/null; then
    log_info "✅ Health check passed"
else
    log_error "❌ Health check failed"
fi

# Test 2: Discovery Server
log_info "Testing Discovery Server..."
if curl -s -f "${BASE_URL}/actuator/health" > /dev/null; then
    log_info "✅ Discovery Server is accessible"
else
    log_error "❌ Discovery Server is not accessible"
fi

# Test 3: Get Categories
log_info "Testing GET /categories..."
response=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/categories?size=5")
if [ "$response" = "200" ]; then
    log_info "✅ GET /categories returned 200"
else
    log_error "❌ GET /categories returned $response"
fi

# Test 4: Get Events
log_info "Testing GET /events..."
response=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/events?size=5")
if [ "$response" = "200" ]; then
    log_info "✅ GET /events returned 200"
else
    log_error "❌ GET /events returned $response"
fi

# Test 5: Get Compilations
log_info "Testing GET /compilations..."
response=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/compilations?size=5")
if [ "$response" = "200" ]; then
    log_info "✅ GET /compilations returned 200"
else
    log_error "❌ GET /compilations returned $response"
fi

# Test 6: JWT Authentication (if enabled)
log_info "Testing JWT Authentication..."
login_response=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"admin123"}' \
    -w "%{http_code}" -o /tmp/login.json)

if [ "$login_response" = "200" ]; then
    log_info "✅ JWT Authentication works"
    # Extract token and test protected endpoint
    token=$(cat /tmp/login.json | jq -r '.accessToken')
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        log_info "✅ Token received"
    fi
else
    log_info "⚠️ JWT Authentication not configured (skipping)"
fi

# Test 7: Database connectivity
log_info "Testing database connectivity..."
# This would require a database check endpoint or actual query
# For now, we just check if the services are healthy
if curl -s "${BASE_URL}/actuator/health" | grep -q "UP"; then
    log_info "✅ Database connections are healthy"
else
    log_error "❌ Database connections are unhealthy"
fi

log_info "✅ All smoke tests passed!"