# Explore With Me Plus 🎉

[![Java CI with Maven](https://github.com/LacusFaustus/java-explore-with-me-plus/actions/workflows/maven.yml/badge.svg)](https://github.com/LacusFaustus/java-explore-with-me-plus/actions/workflows/maven.yml)
[![Coverage](https://img.shields.io/badge/Coverage-70%25-green)](https://github.com/LacusFaustus/java-explore-with-me-plus)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue)](https://kubernetes.io/)

## 📋 О проекте

**Explore With Me Plus** — это современная микросервисная платформа для управления событиями с персонализированными рекомендациями на основе поведения пользователей.

### Ключевые особенности:

- 🔐 **JWT Аутентификация** — безопасный доступ к API с refresh-токенами
- 📊 **Рекомендательная система** — на основе действий пользователей (просмотры, регистрации, лайки)
- 🏗️ **Микросервисная архитектура** — 12+ сервисов с Service Discovery
- 📈 **Мониторинг** — Prometheus + Grafana с бизнес-метриками
- 🔍 **Распределенная трассировка** — Zipkin для отладки
- 🐳 **Контейнеризация** — Docker & Kubernetes
- 📝 **API Документация** — Swagger / OpenAPI 3
- 🎯 **GraphQL** — гибкие запросы для событий

## 🏗 Архитектура

```mermaid
graph TD
    subgraph "Clients"
        Client[Web/Mobile Client]
    end

    subgraph "Gateway Layer"
        Gateway[API Gateway<br/>Port: 8080]
        Gateway -->|JWT Validation| AuthFilter[JWT Filter]
        Gateway -->|Rate Limiting| RateLimit[Rate Limiter]
        Gateway -->|Circuit Breaker| CB[Circuit Breaker]
    end

    subgraph "Infrastructure"
        Eureka[Service Discovery<br/>Port: 8761]
        Config[Config Server<br/>Port: 8082]
        Redis[(Redis Cache<br/>Port: 6379)]
    end

    subgraph "Core Services"
        Auth[Auth Service<br/>Port: 8086]
        User[User Service<br/>Port: 8081]
        Event[Event Service<br/>Port: 8083]
        Request[Request Service<br/>Port: 8084]
        Comment[Comment Service<br/>Port: 8085]
        Stats[Stats Service<br/>Port: 9090]
    end

    subgraph "Recommendation System"
        Kafka[Apache Kafka]
        Collector[Recommendation Collector]
        Aggregator[Recommendation Aggregator]
        Analyzer[Recommendation Analyzer]
    end

    subgraph "Data Layer"
        UserDB[(User DB<br/>PostgreSQL)]
        EventDB[(Event DB<br/>PostgreSQL)]
        RequestDB[(Request DB<br/>PostgreSQL)]
        CommentDB[(Comment DB<br/>PostgreSQL)]
        StatsDB[(Stats DB<br/>PostgreSQL)]
    end

    subgraph "Observability"
        Prometheus[Prometheus<br/>Monitoring]
        Grafana[Grafana<br/>Dashboards]
        Zipkin[Zipkin<br/>Tracing]
    end

    Client --> Gateway
    Gateway --> Eureka
    Gateway --> Config
    
    Auth --> Eureka
    Auth --> Redis
    Auth --> UserDB
    
    User --> Eureka
    User --> Redis
    User --> UserDB
    
    Event --> Eureka
    Event --> Redis
    Event --> EventDB
    Event --> Kafka
    
    Request --> Eureka
    Request --> Redis
    Request --> RequestDB
    Request --> Kafka
    
    Comment --> Eureka
    Comment --> Redis
    Comment --> CommentDB
    
    Stats --> Eureka
    Stats --> StatsDB
    
    Kafka --> Collector
    Collector --> Aggregator
    Aggregator --> Analyzer
    Analyzer --> StatsDB
    
    Event --> Prometheus
    User --> Prometheus
    Request --> Prometheus
    Comment --> Prometheus
    Stats --> Prometheus
    Gateway --> Prometheus
    
    Prometheus --> Grafana
    Event --> Zipkin
    User --> Zipkin
    Request --> Zipkin
    Comment --> Zipkin
    Stats --> Zipkin
    Gateway --> Zipkin
    
🚀 Быстрый старт

Требования

Java 21
Docker & Docker Compose
Maven 3.9+

Запуск

# 1. Клонирование
git clone https://github.com/LacusFaustus/java-explore-with-me-plus.git
cd java-explore-with-me-plus
# 2. Сборка
make build
# 3. Запуск всех сервисов
make dev
# 4. Проверка статуса
make health

Доступные команды

make help      # Показать все команды
make build     # Собрать все сервисы
make up        # Запустить Docker Compose
make down      # Остановить все сервисы
make logs      # Показать логи
make test      # Запустить тесты
make clean     # Очистить артефакты
make health    # Проверить здоровье сервисов

📚 API Документация
Сервис	Swagger	GraphQL
Gateway	/swagger-ui.html	-
Event Service	/swagger-ui.html	/graphql
User Service	/swagger-ui.html	-
Auth Service	/swagger-ui.html	-
🛠 Технологический стек
Компонент	Технология	Версия
Язык	Java	21
Фреймворк	Spring Boot	3.3.4
Cloud	Spring Cloud	2023.0.3
Базы данных	PostgreSQL / H2	15
Кеширование	Redis	7.2
Очереди	Apache Kafka	7.6.0
Сериализация	Avro	1.11.3
RPC	gRPC	1.60.0
Маппинг	MapStruct	1.6.3
Миграции	Liquibase	4.27.0
Мониторинг	Prometheus + Grafana	2.52.0 / 10.4.0
Тестирование	JUnit 5 / Testcontainers	5.10.0
Контейнеризация	Docker	24.0.0
Оркестрация	Kubernetes	1.28

📊 Мониторинг
Сервис	URL	Логин
Grafana	http://localhost:3000	admin/admin
Prometheus	http://localhost:9090	-
Kibana	http://localhost:5601	-
Zipkin	http://localhost:9411	-

📁 Структура проекта

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