# ============================================================
# Explore With Me Plus - Makefile
# ============================================================

.PHONY: help build up down logs clean test gen dev prod

# Colors
GREEN  := $(shell tput -Txterm setaf 2)
YELLOW := $(shell tput -Txterm setaf 3)
WHITE  := $(shell tput -Txterm setaf 7)
RESET  := $(shell tput -Txterm sgr0)

help: ## Show this help
	@echo ''
	@echo '${GREEN}Explore With Me Plus${RESET} - Available commands:'
	@echo ''
	@echo '${YELLOW}Development:${RESET}'
	@echo '  ${WHITE}make dev${RESET}        🚀 Start all services for development'
	@echo '  ${WHITE}make build${RESET}      📦 Build all services (skip tests)'
	@echo '  ${WHITE}make test${RESET}       🧪 Run all tests'
	@echo '  ${WHITE}make gen${RESET}        ⚙️  Generate sources (Avro, Protobuf)'
	@echo '  ${WHITE}make clean${RESET}      🧹 Clean all artifacts'
	@echo ''
	@echo '${YELLOW}Docker:${RESET}'
	@echo '  ${WHITE}make up${RESET}         🐳 Start all services with Docker Compose'
	@echo '  ${WHITE}make down${RESET}       🛑 Stop all services'
	@echo '  ${WHITE}make logs${RESET}       📋 Show logs from all services'
	@echo '  ${WHITE}make ps${RESET}         📊 Show service status'
	@echo ''
	@echo '${YELLOW}Production:${RESET}'
	@echo '  ${WHITE}make prod${RESET}       🚀 Deploy to production'
	@echo '  ${WHITE}make health${RESET}     🏥 Check all services health'

# ============================================================
# DEVELOPMENT
# ============================================================

dev: build up ## 🚀 Full development setup
	@echo '${GREEN}✅ All services started!${RESET}'
	@echo ''
	@echo '📍 Gateway:      http://localhost:8080'
	@echo '📍 Discovery:    http://localhost:8761'
	@echo '📍 Grafana:      http://localhost:3000 (admin/admin)'
	@echo '📍 Prometheus:   http://localhost:9090'
	@echo '📍 Kibana:       http://localhost:5601'

build: ## 📦 Build all services
	@echo '${YELLOW}📦 Building all services...${RESET}'
	mvn clean install -DskipTests -U
	@echo '${GREEN}✅ Build completed!${RESET}'

test: ## 🧪 Run all tests
	@echo '${YELLOW}🧪 Running tests...${RESET}'
	mvn test
	@echo '${GREEN}✅ Tests completed!${RESET}'

gen: ## ⚙️ Generate sources (Avro, Protobuf)
	@echo '${YELLOW}⚙️  Generating sources...${RESET}'
	mvn clean generate-sources
	@echo '${GREEN}✅ Sources generated!${RESET}'

clean: ## 🧹 Clean all artifacts
	@echo '${YELLOW}🧹 Cleaning artifacts...${RESET}'
	mvn clean
	docker-compose down -v 2>/dev/null || true
	@echo '${GREEN}✅ Cleaned!${RESET}'

# ============================================================
# DOCKER
# ============================================================

up: ## 🐳 Start all services with Docker Compose
	@echo '${YELLOW}🐳 Starting Docker Compose...${RESET}'
	docker-compose up -d
	@echo '${GREEN}✅ Services started!${RESET}'

down: ## 🛑 Stop all services
	@echo '${YELLOW}🛑 Stopping Docker Compose...${RESET}'
	docker-compose down
	@echo '${GREEN}✅ Services stopped!${RESET}'

logs: ## 📋 Show logs
	docker-compose logs -f

ps: ## 📊 Show service status
	docker-compose ps

# ============================================================
# PRODUCTION
# ============================================================

prod: ## 🚀 Deploy to production
	@echo '${YELLOW}🚀 Deploying to production...${RESET}'
	./scripts/prod-deploy.sh
	@echo '${GREEN}✅ Production deployed!${RESET}'

health: ## 🏥 Check all services health
	@echo '${YELLOW}🏥 Checking health...${RESET}'
	./scripts/health-check.sh

# ============================================================
# UTILITY
# ============================================================

.PHONY: help build up down logs clean test gen dev prod health