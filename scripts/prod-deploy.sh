#!/bin/bash

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi

    if [ ! -f ".env.production" ]; then
        log_error ".env.production file not found"
        exit 1
    fi

    log_info "Prerequisites OK"
}

# Load environment
load_env() {
    log_info "Loading environment variables..."
    set -a
    source .env.production
    set +a
    log_info "Environment loaded"
}

# Create directories
create_directories() {
    log_info "Creating directories..."
    mkdir -p monitoring/grafana/provisioning
    mkdir -p monitoring/grafana/dashboards
    mkdir -p monitoring/logstash/pipeline
    mkdir -p ssl
    mkdir -p init-scripts
    mkdir -p backups
    log_info "Directories created"
}

# Pull images
pull_images() {
    log_info "Pulling Docker images..."
    docker-compose -f docker-compose.prod.yml pull --parallel
    log_info "Images pulled"
}

# Deploy services
deploy_services() {
    log_info "Deploying services..."
    docker-compose -f docker-compose.prod.yml up -d
    log_info "Services deployed"
}

# Wait for services
wait_for_services() {
    log_info "Waiting for services to be healthy..."

    local services=(
        "discovery-server:8761"
        "config-server:8082"
        "gateway-server:8080"
        "user-service:8081"
        "auth-service:8086"
        "event-service:8083"
        "request-service:8084"
        "comment-service:8085"
        "stats-server:9090"
    )

    for service in "${services[@]}"; do
        local name="${service%:*}"
        local port="${service#*:}"

        log_info "Waiting for $name on port $port..."

        local retries=0
        local max_retries=60

        while [ $retries -lt $max_retries ]; do
            if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
                log_info "$name is healthy"
                break
            fi
            retries=$((retries + 1))
            sleep 2
        done

        if [ $retries -eq $max_retries ]; then
            log_error "$name failed to start"
            exit 1
        fi
    done

    log_info "All services are healthy"
}

# Run smoke tests
run_smoke_tests() {
    log_info "Running smoke tests..."
    ./scripts/smoke-tests.sh http://localhost:8080
    log_info "Smoke tests passed"
}

# Show status
show_status() {
    log_info "=== Deployment Status ==="
    docker-compose -f docker-compose.prod.yml ps
    log_info "==========================="
}

# Main
main() {
    log_info "Starting production deployment..."

    check_prerequisites
    load_env
    create_directories
    pull_images
    deploy_services
    wait_for_services
    run_smoke_tests
    show_status

    log_info "Production deployment completed successfully!"
    log_info "Gateway: http://localhost:8080"
    log_info "Grafana: http://localhost:3000 (user: $GRAFANA_USER)"
    log_info "Kibana: http://localhost:5601"
    log_info "Prometheus: http://localhost:9090"
}

main "$@"