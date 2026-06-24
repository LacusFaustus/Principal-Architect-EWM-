.PHONY: help build up down logs clean test

help:
	@echo "Available commands:"
	@echo "  make build   - Build all services"
	@echo "  make up      - Start all services"
	@echo "  make down    - Stop all services"
	@echo "  make logs    - Show logs from all services"
	@echo "  make clean   - Clean all build artifacts"
	@echo "  make test    - Run all tests"

build:
	@echo "Building all services..."
	mvn clean package -DskipTests

up:
	@echo "Starting all services..."
	docker-compose up -d

down:
	@echo "Stopping all services..."
	docker-compose down

logs:
	@echo "Showing logs..."
	docker-compose logs -f

clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	@echo "Cleaning Docker volumes..."
	docker-compose down -v

test:
	@echo "Running tests..."
	mvn test

build-prod:
	@echo "Building production images..."
	docker-compose -f docker-compose.prod.yml build

up-prod:
	@echo "Starting production services..."
	docker-compose -f docker-compose.prod.yml up -d

down-prod:
	@echo "Stopping production services..."
	docker-compose -f docker-compose.prod.yml down

health:
	@echo "Checking service health..."
	@curl -s http://localhost:8761/actuator/health | jq '.'
	@curl -s http://localhost:8080/actuator/health | jq '.'