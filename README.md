# API Testing Service

Сервис для автоматизации API-тестирования, разработанный в рамках дипломного проекта.

Приложение позволяет:
- создавать проекты для тестируемых веб-сервисов;
- хранить базовый URL проекта;
- создавать API-тесты внутри проекта;
- генерировать проект и тесты на основе OpenAPI/Swagger-спецификации;
- группировать тесты по `Feature` и `Story`;
- запускать один тест или все тесты проекта;
- сохранять историю запусков;
- переиспользовать токены и другие значения между тестами в рамках общего прогона;
- смотреть BI-отчет по проекту с тестовыми метриками и диаграммами.

## Технологии

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- H2 Database
- springdoc-openapi / Swagger UI
- React 19
- TypeScript
- Vite

## Структура проекта

- `src/main/java` — backend-часть приложения.
- `src/test/java` — автотесты backend-логики.
- `frontend/` — frontend-часть на React + TypeScript.
- `docs/` — дополнительные материалы для диплома, например схема классов.

## Запуск backend

```bash
./mvnw spring-boot:run
```

После запуска доступны:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console`

## Запуск frontend

Backend должен быть запущен на `http://localhost:8080`.

```bash
cd frontend
npm install
npm run dev
```

После запуска Vite покажет адрес frontend-приложения. Обычно это:

```text
http://localhost:5173
```

Чтобы остановить frontend, нажмите `Ctrl + C` в терминале, где он запущен.

## Основные возможности

### Проекты

Проект описывает тестируемый API.

В проекте хранятся:
- название;
- базовый URL;
- описание.

Проект можно создать, посмотреть, изменить и удалить.

### API-тесты

API-тест описывает проверку одной ручки backend-сервиса.

В тесте можно указать:
- `Feature` — функциональная область;
- `Story` — сценарий внутри функциональной области;
- HTTP-метод;
- endpoint;
- тело запроса;
- ожидаемый HTTP-статус;
- ожидаемый фрагмент тела ответа;
- JSONPath-проверку;
- проверку заголовка;
- максимальное время ответа;
- порядок запуска;
- заголовки запроса;
- правило для сохранения значения из ответа.

### Генерация тестов по OpenAPI

Сервис умеет создавать проект и набор API-тестов на основе OpenAPI/Swagger JSON.

Пользователь передает:
- название проекта;
- базовый URL тестируемого сервиса;
- URL OpenAPI/Swagger-спецификации;
- описание проекта.

Генератор:
- читает `paths` из OpenAPI-документа;
- создает тесты для методов `GET`, `POST`, `PUT`, `DELETE`;
- берет `Feature` из `tags`;
- берет `Story` из `summary`, `operationId` или path;
- выбирает успешный ожидаемый статус из `responses`;
- подставляет тестовые значения в path/query-параметры;
- формирует пример request body по schema, если она описана;
- сохраняет тесты как обычные `ApiTest`.

Сгенерированные тесты являются черновиками. Их можно отредактировать перед запуском.

### Feature / Story

Тесты на странице проекта группируются по структуре:

```text
Project -> Feature -> Story -> ApiTest
```

Например:

```text
JSONPlaceholder API
└── Users
    └── Profile
        └── Получение списка пользователей
```

Если у теста не указаны `Feature` или `Story`, он попадает в группу `Без feature` / `Без story`.

### Запуск тестов

Можно запустить:
- один конкретный тест;
- все тесты проекта.

При запуске всех тестов сервис выполняет их по `runOrder`. Если порядок одинаковый, используется id теста.

### Передача токена между тестами

Для авторизации не нужно вручную копировать токен.

Сценарий:
1. Первый тест выполняет логин.
2. Сервис извлекает токен из ответа через `captureJsonPath`.
3. Значение сохраняется в переменную, например `token`.
4. Следующие тесты используют `{{token}}` в заголовках или теле запроса.

Пример теста логина:

```json
{
  "name": "Login",
  "feature": "Auth",
  "story": "Token",
  "method": "POST",
  "endpoint": "/api/login",
  "requestBody": "{\"username\":\"admin\",\"password\":\"password\"}",
  "expectedStatus": 200,
  "runOrder": 0,
  "captureJsonPath": "$.token",
  "captureVariableName": "token"
}
```

Пример теста, который использует токен:

```json
{
  "name": "Get profile",
  "feature": "Profile",
  "story": "Authorized user",
  "method": "GET",
  "endpoint": "/api/profile",
  "expectedStatus": 200,
  "runOrder": 1,
  "requestHeadersJson": "{\"Authorization\":\"Bearer {{token}}\"}"
}
```

Важно: переменные работают внутри общего запуска проекта через `POST /projects/{projectId}/tests/run`.
Если запускать второй тест отдельно, переменная `{{token}}` не будет заполнена.

## BI-отчет

Для каждого проекта доступен отчет:

```http
GET /projects/{id}/report
```

В отчете отображаются:
- общее количество тестов;
- количество успешных тестов;
- количество упавших тестов;
- количество еще не запущенных тестов;
- процент успешности;
- дата последнего запуска;
- p50 и p95 времени ответа;
- среднее время ответа;
- длительность последнего общего прогона;
- круговая диаграмма результатов;
- столбиковая диаграмма длительности тестов;
- линейный график длительности прогонов по датам;
- таблица последних прогонов;
- детализация по каждому тесту.

## Основные REST-ручки

### Проекты

- `POST /projects` — создать проект.
- `POST /projects/generate-from-openapi` — создать проект и тесты из OpenAPI/Swagger.
- `GET /projects` — получить список проектов.
- `GET /projects/{id}` — получить проект по id.
- `PUT /projects/{id}` — изменить проект.
- `DELETE /projects/{id}` — удалить проект.
- `GET /projects/{id}/report` — получить отчет по проекту.

### Тесты внутри проекта

- `POST /projects/{projectId}/tests` — создать тест.
- `GET /projects/{projectId}/tests` — получить тесты проекта.
- `GET /projects/{projectId}/tests/{testId}` — получить тест.
- `PUT /projects/{projectId}/tests/{testId}` — изменить тест.
- `DELETE /projects/{projectId}/tests/{testId}` — удалить тест.
- `POST /projects/{projectId}/tests/run` — запустить все тесты проекта.

### Запуск и история

- `POST /tests/{testId}/run` — запустить один тест.
- `GET /tests/{testId}/history` — получить историю запусков теста.

## Демо-сценарий

Для ручной проверки можно использовать JSONPlaceholder.

```bash
export API="http://localhost:8080"
```

### 1. Создать проект

```bash
curl -X POST "$API/projects" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "JSONPlaceholder API",
    "baseUrl": "https://jsonplaceholder.typicode.com",
    "description": "Тестовый сервис"
  }'
```

### 2. Создать тест

```bash
curl -X POST "$API/projects/1/tests" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Получение поста",
    "description": "Проверка GET /posts/1",
    "testKey": "POST-GET-001",
    "feature": "Posts",
    "story": "Read",
    "method": "GET",
    "endpoint": "/posts/1",
    "expectedResponseBody": "\"id\": 1",
    "expectedJsonPath": "$.id",
    "expectedJsonValue": "1",
    "expectedHeaderName": "Content-Type",
    "expectedHeaderValue": "application/json",
    "maxResponseTimeMs": 2000,
    "expectedStatus": 200,
    "runOrder": 0
  }'
```

### Альтернативно: сгенерировать проект из OpenAPI

```bash
curl -X POST "$API/projects/generate-from-openapi" \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "Petstore API",
    "baseUrl": "https://petstore.swagger.io/v2",
    "openApiUrl": "https://petstore.swagger.io/v2/swagger.json",
    "description": "Проект и тесты, созданные по Swagger"
  }'
```

Ожидаемый результат: созданный проект и список сгенерированных тестов.

### 3. Запустить один тест

```bash
curl -X POST "$API/tests/1/run"
```

### 4. Запустить все тесты проекта

```bash
curl -X POST "$API/projects/1/tests/run"
```

### 5. Получить историю запусков теста

```bash
curl "$API/tests/1/history"
```

### 6. Получить отчет по проекту

```bash
curl "$API/projects/1/report"
```
