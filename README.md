# Explore With Me Plus 🎉

[![Java CI with Maven](https://github.com/LacusFaustus/java-explore-with-me-plus/actions/workflows/maven.yml/badge.svg)](https://github.com/LacusFaustus/java-explore-with-me-plus/actions/workflows/maven.yml)
[![Coverage](https://img.shields.io/badge/Coverage-20%25-yellow)](https://github.com/LacusFaustus/java-explore-with-me-plus)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue)](https://kubernetes.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## 📋 О проекте

**Explore With Me Plus** — это современная микросервисная платформа для управления событиями. Проект позволяет пользователям создавать, публиковать и находить события, а также получать персонализированные рекомендации на основе их действий.

### Ключевые особенности:

- 🔐 **JWT Аутентификация** — безопасный доступ к API
- 📊 **Рекомендательная система** — на основе действий пользователей (просмотры, регистрации, лайки)
- 🏗️ **Микросервисная архитектура** — 12+ сервисов с Service Discovery
- 📈 **Мониторинг** — Prometheus + Grafana
- 🔍 **Распределенная трассировка** — Zipkin
- 🐳 **Контейнеризация** — Docker & Kubernetes
- 📝 **API Документация** — Swagger / OpenAPI 3
- 🎯 **GraphQL** — гибкие запросы для событий
- 🧪 **Нагрузочное тестирование** — JMeter сценарии

## 🏗 Архитектура
┌─────────────────────────────────────────────────────────────────┐
│ API Gateway (port: 8080) │
│ (JWT Authentication + Rate Limiter) │
└─────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────┐
│ Service Discovery (Eureka) │
└─────────────────────────────────────────────────────────────────┘
│
┌───────────────────────┼───────────────────────┐
│ │ │
▼ ▼ ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│ User Service │ │ Event Service │ │Request Service│
│ (port: 8081) │ │ (port: 8083) │ │ (port: 8084) │
└───────────────┘ └───────────────┘ └───────────────┘
│ │ │
▼ ▼ ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│Comment Service│ │ Stats Service │ │Recommendation │
│ (port: 8085) │ │ (port: 9090) │ │ Collectors │
└───────────────┘ └───────────────┘ └───────────────┘
│
▼
┌─────────────────────┐
│ Kafka + Schema │
│ (port: 9092, 8081)│
└─────────────────────┘

text

## 🛠 Технологический стек

| Компонент | Технология | Версия |
|-----------|------------|--------|
| **Язык** | Java | 21 |
| **Фреймворк** | Spring Boot | 3.3.4 |
| **Cloud** | Spring Cloud | 2023.0.3 |
| **Базы данных** | PostgreSQL / H2 | 15 / 2.2.224 |
| **Кеширование** | Redis | 7.2 |
| **Очереди** | Apache Kafka | 7.6.0 |
| **Сериализация** | Avro + Schema Registry | 1.11.3 |
| **RPC** | gRPC | 1.60.0 |
| **ORM** | Hibernate / JPA | 6.4.0 |
| **Маппинг** | MapStruct | 1.6.3 |
| **Миграции** | Liquibase | 4.27.0 |
| **Мониторинг** | Prometheus + Grafana | 2.52.0 / 10.4.0 |
| **Тестирование** | JUnit 5 / Testcontainers | 5.10.0 |
| **Контейнеризация** | Docker | 24.0.0 |
| **Оркестрация** | Kubernetes (Helm) | 1.28 |

## 🚀 Быстрый старт

```bash
# 1. Клонирование репозитория
git clone https://github.com/LacusFaustus/java-explore-with-me-plus.git
cd java-explore-with-me-plus

# 2. Сборка проекта
mvn clean install -DskipTests

# 3. Запуск всех сервисов через Docker Compose
docker-compose up -d

# 4. Проверка статуса
docker-compose ps
📚 API Документация
После запуска доступны следующие эндпоинты:

Gateway: http://localhost:8080

Discovery Server: http://localhost:8761

Grafana: http://localhost:3000 (admin/admin)

Prometheus: http://localhost:9090

Kibana: http://localhost:5601

Swagger UI: http://localhost:8080/swagger-ui.html

📁 Структура проекта
text
java-explore-with-me-plus/
├── core/                    # Основные микросервисы
│   ├── auth-service/        # Аутентификация и JWT
│   ├── user-service/        # Управление пользователями
│   ├── event-service/       # Управление событиями
│   ├── request-service/     # Управление заявками
│   ├── comment-service/     # Управление комментариями
│   └── common-*/            # Общие модули (DTO, инфраструктура)
├── infra/                   # Инфраструктурные сервисы
│   ├── discovery-server/    # Eureka Service Discovery
│   ├── config-server/       # Spring Cloud Config
│   └── gateway-server/      # API Gateway
├── stats-service/           # Статистика и рекомендации
│   ├── stat-svc-*/          # Сервис статистики
│   └── recommendation-*/    # Рекомендательная система
├── event-context/           # DDD контекст событий
├── helm/                    # Helm чарты для Kubernetes
├── k8s/                     # Kubernetes манифесты
├── monitoring/              # Prometheus и Grafana
├── scripts/                 # Вспомогательные скрипты
└── docker-compose.yml       # Docker Compose для разработки
📄 Лицензия
MIT License - см. файл LICENSE