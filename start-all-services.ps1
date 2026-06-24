# start-all-services.ps1
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Starting EWM Plus Services" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

# 1. Проверка Docker
Write-Host "`n[1/5] Checking Docker..." -ForegroundColor Yellow
docker --version
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker is not running!" -ForegroundColor Red
    exit 1
}

# 2. Запуск инфраструктуры
Write-Host "`n[2/5] Starting infrastructure..." -ForegroundColor Yellow
docker-compose up -d postgres redis zookeeper kafka

# 3. Ожидание готовности БД
Write-Host "`n[3/5] Waiting for databases to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# 4. Запуск Discovery и Config
Write-Host "`n[4/5] Starting Discovery and Config servers..." -ForegroundColor Yellow
docker-compose up -d discovery-server config-server

Start-Sleep -Seconds 10

# 5. Запуск всех сервисов
Write-Host "`n[5/5] Starting all services..." -ForegroundColor Yellow
docker-compose up -d

# 6. Проверка статуса
Write-Host "`nChecking status..." -ForegroundColor Yellow
Start-Sleep -Seconds 10
docker-compose ps

Write-Host "`n====================================" -ForegroundColor Cyan
Write-Host "All services started! 🚀" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Gateway: http://localhost:8080"
Write-Host "Discovery: http://localhost:8761"
Write-Host "Grafana: http://localhost:3000 (admin/admin)"
Write-Host "Prometheus: http://localhost:9090"
Write-Host "Kibana: http://localhost:5601"