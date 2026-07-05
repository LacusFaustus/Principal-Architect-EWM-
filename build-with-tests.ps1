# build-with-tests.ps1
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Building EWM Plus with Tests" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

$rootDir = "C:\projects\java-explore-with-me-plus"
Set-Location $rootDir

Write-Host "`n[1/5] Cleaning previous builds..." -ForegroundColor Yellow
mvn clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to clean!" -ForegroundColor Red
    exit 1
}

Write-Host "`n[2/5] Installing root POM..." -ForegroundColor Yellow
mvn install -N -f pom.xml -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to install root POM!" -ForegroundColor Red
    exit 1
}

Write-Host "`n[3/5] Building recommendation-schema..." -ForegroundColor Yellow
mvn clean install -pl stats-service/recommendation-schema -am -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build recommendation-schema!" -ForegroundColor Red
    exit 1
}

Write-Host "`n[4/5] Building infra modules..." -ForegroundColor Yellow
mvn clean install -pl infra -am -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build infra!" -ForegroundColor Red
    exit 1
}

Write-Host "`n[5/5] Building entire project with tests..." -ForegroundColor Yellow
mvn clean install -U
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Some tests failed!" -ForegroundColor Red
    Write-Host "You can run with -DskipTests to skip tests" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n====================================" -ForegroundColor Cyan
Write-Host "Build with tests completed successfully! 🎉" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Cyan