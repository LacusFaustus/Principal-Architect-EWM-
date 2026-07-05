# check-test-failures.ps1
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Checking Test Failures" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

$rootDir = "C:\projects\java-explore-with-me-plus"
$reportDir = "$rootDir\core\user-service\target\surefire-reports"

if (Test-Path $reportDir) {
    Write-Host "`n📁 Found test reports in: $reportDir" -ForegroundColor Green

    # Показываем все XML отчеты
    Get-ChildItem $reportDir -Filter "*.xml" | ForEach-Object {
        Write-Host "`n--- $($_.Name) ---" -ForegroundColor Cyan
        $content = Get-Content $_.FullName -Raw
        # Ищем ошибки
        if ($content -match "<failure|<error") {
            Write-Host "❌ Test failed!" -ForegroundColor Red
            # Показываем подробности
            $content -replace ".*?(<failure.*?>.*?</failure>).*?", "`$1" -replace "<[^>]+>", " " | Out-String
        } else {
            Write-Host "✅ All tests passed" -ForegroundColor Green
        }
    }

    # Показываем текстовые отчеты
    Get-ChildItem $reportDir -Filter "*.txt" | ForEach-Object {
        Write-Host "`n📄 $($_.Name)" -ForegroundColor Yellow
        Get-Content $_.FullName -Tail 30
    }
} else {
    Write-Host "❌ No test reports found!" -ForegroundColor Red
}

Write-Host "`n====================================" -ForegroundColor Cyan