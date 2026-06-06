# Фикс конфликта версий Spring Kafka
Write-Host "Fixing Spring Kafka version conflict..." -ForegroundColor Green

# Обновить главный pom.xml - изменить версию spring-kafka
$pomPath = "C:\projects\java-explore-with-me-plus\pom.xml"
$pomContent = Get-Content $pomPath -Raw

# Добавить spring-kafka.version если нет
if ($pomContent -notmatch "spring-kafka.version") {
    $pomContent = $pomContent -replace "(<protobuf.version>.*?</protobuf.version>)", "`$1`n        <spring-kafka.version>3.2.4</spring-kafka.version>"
    $pomContent | Set-Content $pomPath
    Write-Host "Added spring-kafka.version property" -ForegroundColor Green
}

# Удалить явную версию spring-kafka из dependencyManagement
$pomContent = Get-Content $pomPath -Raw
$pomContent = $pomContent -replace "<dependency>`s*<groupId>org.springframework.kafka</groupId>`s*<artifactId>spring-kafka</artifactId>`s*<version>.*?</version>`s*</dependency>",
    "<dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>"
$pomContent | Set-Content $pomPath

Write-Host "Fixed pom.xml" -ForegroundColor Green

# Очистить и пересобрать
Write-Host "Cleaning and rebuilding..." -ForegroundColor Green
mvn clean install -DskipTests

Write-Host "Done! Now try to run recommendation services again." -ForegroundColor Green