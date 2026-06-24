#!/bin/bash

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

check_service() {
    local service=$1
    local url=$2
    local response=$(curl -s -o /dev/null -w "%{http_code}" "$url")

    if [ "$response" = "200" ] || [ "$response" = "204" ]; then
        log_info "✅ $service is healthy"
        return 0
    else
        log_error "❌ $service is not responding (HTTP $response)"
        return 1
    fi
}

echo "===================================="
echo "EWM Plus Health Check"
echo "===================================="

# 1. Discovery Server
check_service "Discovery Server" "http://localhost:8761/actuator/health"

# 2. Gateway
check_service "Gateway" "http://localhost:8080/actuator/health"

# 3. User Service
check_service "User Service" "http://localhost:8080/actuator/health"

# 4. Event Service
check_service "Event Service" "http://localhost:8083/actuator/health"

# 5. Request Service
check_service "Request Service" "http://localhost:8084/actuator/health"

# 6. Comment Service
check_service "Comment Service" "http://localhost:8085/actuator/health"

# 7. Stats Service
check_service "Stats Service" "http://localhost:9090/actuator/health"

# 8. Eureka Registry
log_info "Checking Eureka Registry..."
eureka_response=$(curl -s http://localhost:8761/eureka/apps)
if echo "$eureka_response" | grep -q "applications"; then
    log_info "✅ Eureka Registry is accessible"
else
    log_warn "⚠️ Eureka Registry response may be empty"
fi

echo "===================================="
echo "Health check complete! ✅"
echo "===================================="