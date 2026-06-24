# build-complete.ps1
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Building EWM Plus - Complete Build" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

# Переходим в корневую директорию
$rootDir = "C:\projects\java-explore-with-me-plus"
Set-Location $rootDir

# Шаг 1: Очистка кэша
Write-Host "`n[1/8] Cleaning Maven cache..." -ForegroundColor Yellow
$cachePath = "$env:USERPROFILE\.m2\repository\ru\practicum"
if (Test-Path $cachePath) {
    Remove-Item -Path $cachePath -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "Cache cleaned: $cachePath" -ForegroundColor Green
}

# Шаг 2: Установка корневого POM
Write-Host "`n[2/8] Installing root POM..." -ForegroundColor Yellow
mvn clean install -N -f pom.xml
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to install root POM!" -ForegroundColor Red
    exit 1
}
Write-Host "Root POM installed successfully" -ForegroundColor Green

# Шаг 3: Сборка инфраструктуры
Write-Host "`n[3/8] Building infrastructure..." -ForegroundColor Yellow
mvn clean install -pl infra -am -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build infrastructure!" -ForegroundColor Red
    exit 1
}
Write-Host "Infrastructure built successfully" -ForegroundColor Green

# Шаг 4: Сборка recommendation-schema
Write-Host "`n[4/8] Building recommendation-schema..." -ForegroundColor Yellow
mvn clean install -pl stats-service/recommendation-schema -am -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build recommendation-schema!" -ForegroundColor Red
    exit 1
}
Write-Host "recommendation-schema built successfully" -ForegroundColor Green

# Шаг 5: Сборка stats-service
Write-Host "`n[5/8] Building stats-service..." -ForegroundColor Yellow
mvn clean install -pl stats-service -am -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build stats-service!" -ForegroundColor Red
    exit 1
}
Write-Host "stats-service built successfully" -ForegroundColor Green

# Шаг 6: Сборка core (без auth-service)
Write-Host "`n[6/8] Building core modules..." -ForegroundColor Yellow
mvn clean install -pl core -am -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build core modules!" -ForegroundColor Red
    exit 1
}
Write-Host "core modules built successfully" -ForegroundColor Green

# Шаг 7: Сборка auth-service отдельно
Write-Host "`n[7/8] Building auth-service..." -ForegroundColor Yellow
mvn clean install -pl core/auth-service -am -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build auth-service!" -ForegroundColor Red
    exit 1
}
Write-Host "auth-service built successfully" -ForegroundColor Green

# Шаг 8: Полная сборка
Write-Host "`n[8/8] Building entire project..." -ForegroundColor Yellow
mvn clean install -DskipTests -U
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build entire project!" -ForegroundColor Red
    exit 1
}
Write-Host "Project built successfully!" -ForegroundColor Green

Write-Host "`n====================================" -ForegroundColor Cyan
Write-Host "Build completed successfully! 🎉" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Cyan