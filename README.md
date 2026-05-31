# Explore With Me Plus

Микросервисное приложение-афиша для публикации и поиска событий.

## Архитектура

### Инфраструктурные сервисы
- **Discovery Server** (Eureka) - порт 8761
- **Config Server** - порт 8082
- **Gateway Server** - порт 8080

### Бизнес-сервисы
- **User Service** - управление пользователями
- **Event Service** - управление событиями, категориями, подборками
- **Request Service** - управление заявками на участие
- **Stats Service** - сбор и хранение статистики

## Технологии
- Java 21
- Spring Boot 3.3.4
- Spring Cloud 2023.0.3
- PostgreSQL / H2
- MapStruct 1.6.3
- Lombok

## Запуск проекта

### Локальный запуск с H2
```bash
mvn clean install -DskipTests
# Запустить сервисы в порядке: discovery-server → config-server → gateway-server → остальные