# generate-sources.ps1
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Generating sources for EWM Plus" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

# Переходим в корневую директорию
$rootDir = "C:\projects\java-explore-with-me-plus"
Set-Location $rootDir

# Шаг 1: Генерация sources для recommendation-schema
Write-Host "`n[1/3] Generating sources for recommendation-schema..." -ForegroundColor Yellow
Set-Location "stats-service/recommendation-schema"
mvn clean generate-sources
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to generate sources for recommendation-schema!" -ForegroundColor Red
    exit 1
}
Write-Host "Sources generated successfully" -ForegroundColor Green

# Шаг 2: Установка recommendation-schema
Write-Host "`n[2/3] Installing recommendation-schema..." -ForegroundColor Yellow
mvn clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to install recommendation-schema!" -ForegroundColor Red
    exit 1
}
Write-Host "recommendation-schema installed successfully" -ForegroundColor Green

# Шаг 3: Сборка всего проекта
Write-Host "`n[3/3] Building entire project..." -ForegroundColor Yellow
Set-Location $rootDir
mvn clean install -DskipTests -U
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build entire project!" -ForegroundColor Red
    exit 1
}
Write-Host "Project built successfully!" -ForegroundColor Green

Write-Host "`n====================================" -ForegroundColor Cyan
Write-Host "All sources generated successfully! 🎉" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Cyan