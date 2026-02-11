# Task Tracker + CRM (Backend)

## Обзор
Микросервисный бэкенд для трекинга задач и CRM с API Gateway, Auth, User, Task, Client сервисами. Java 17, Spring Boot, PostgreSQL, Flyway, JWT, OAuth2 (Google для пользователей), Docker, Kubernetes.

## Сервисы
- **api-gateway**: единая точка входа, маршрутизация, проверка JWT, агрегация Swagger UI.
- **auth-service**: регистрация (только USER), логин, выдача JWT, Google OAuth2, эндпоинты админ-панели.
- **user-service**: управление профилем пользователя.
- **task-service**: CRUD задач, назначение, статусы, приоритеты, дедлайны.
- **client-service**: CRUD клиентов, контактные данные.

## Версионирование API
Все эндпоинты находятся под `/api/v1/...`.

## Админ-панель
Админские эндпоинты находятся под `/api/v1/admin/...` в auth-service и требуют `ROLE_ADMIN`.

## Формат ошибок
```
{
  "timestamp": "2026-02-02T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation error",
  "path": "/api/v1/..."
}
```
