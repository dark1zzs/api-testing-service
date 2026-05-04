# API Testing Service (Diploma MVP)

Backend-сервис для автоматизированного тестирования API в стиле упрощенного Postman/Newman:
- создается проект с `baseUrl`;
- в проекте описываются API-тесты;
- тесты запускаются по одному или все сразу;
- сохраняется история запусков;
- формируется отчет по качеству API.

## Stack

- Java 21
- Spring Boot (Web MVC, Data JPA, Validation)
- H2 database
- springdoc-openapi (Swagger UI)

## Run

```bash
./mvnw spring-boot:run
```

Полезные URL после запуска:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 console: `http://localhost:8080/h2-console`

## Main Endpoints

### Projects
- `POST /projects` - create project
- `GET /projects` - list projects
- `GET /projects/{id}` - get project
- `PUT /projects/{id}` - update project
- `DELETE /projects/{id}` - delete project
- `GET /projects/{id}/report` - project report

### API Tests (inside project)
- `POST /projects/{projectId}/tests` - create test
- `GET /projects/{projectId}/tests` - list tests in project
- `GET /projects/{projectId}/tests/{testId}` - get test
- `PUT /projects/{projectId}/tests/{testId}` - update test
- `DELETE /projects/{projectId}/tests/{testId}` - delete test (`204 No Content`)
- `POST /projects/{projectId}/tests/run` - run all tests in project

### Test Execution / History
- `POST /tests/{testId}/run` - run one test
- `GET /tests/{testId}/history` - test run history

## Demo Scenario (3-5 minutes)

Base URL for demo:

```bash
export API="http://localhost:8080"
```

### 1) Create project

```bash
curl -X POST "$API/projects" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "JSONPlaceholder checks",
    "baseUrl": "https://jsonplaceholder.typicode.com",
    "description": "Demo project for diploma"
  }'
```

Expected response (example):

```json
{
  "id": 1,
  "name": "JSONPlaceholder checks",
  "baseUrl": "https://jsonplaceholder.typicode.com",
  "description": "Demo project for diploma"
}
```

### 2) Create API test in project

```bash
curl -X POST "$API/projects/1/tests" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Get post by id",
    "description": "GET /posts/1 returns valid post",
    "testKey": "POST-GET-001",
    "method": "GET",
    "endpoint": "/posts/1",
    "requestBody": null,
    "expectedResponseBody": "\"id\": 1",
    "expectedJsonPath": "$.id",
    "expectedJsonValue": "1",
    "expectedHeaderName": "Content-Type",
    "expectedHeaderValue": "application/json",
    "maxResponseTimeMs": 2000,
    "expectedStatus": 200
  }'
```

### 3) Update test (CRUD check)

```bash
curl -X PUT "$API/projects/1/tests/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Get post by id (updated)",
    "description": "Checks id and userId",
    "testKey": "POST-GET-001",
    "method": "GET",
    "endpoint": "/posts/1",
    "requestBody": null,
    "expectedResponseBody": "\"userId\": 1",
    "expectedJsonPath": "$.userId",
    "expectedJsonValue": "1",
    "expectedHeaderName": "Content-Type",
    "expectedHeaderValue": "application/json",
    "maxResponseTimeMs": 1500,
    "expectedStatus": 200
  }'
```

Expected: `200 OK` + updated test JSON.

### 4) Run one test

```bash
curl -X POST "$API/tests/1/run"
```

Expected response fields:
- `success`
- `statusCode`
- `responseTimeMs`
- `responseBody`
- `errorMessage`

### 5) Run all tests in project

```bash
curl -X POST "$API/projects/1/tests/run"
```

Expected: JSON array with execution results for each test.

### 6) Get history for test

```bash
curl "$API/tests/1/history"
```

Expected response item (example):

```json
{
  "id": 5,
  "testId": 1,
  "testName": "Get post by id (updated)",
  "success": true,
  "statusCode": 200,
  "responseTimeMs": 120,
  "responseBody": "{...}",
  "errorMessage": null,
  "executedAt": "2026-05-04T19:10:00"
}
```

### 7) Get project quality report

```bash
curl "$API/projects/1/report"
```

Expected response fields:
- `totalTests`
- `passedTests`
- `failedTests`
- `notRunTests`
- `successRate`
- `lastRunAt`
- `tests[]` with per-test details (`statusCode`, `responseTimeMs`, `errorMessage`, `lastRunAt`)

### 8) Delete test (CRUD check)

```bash
curl -i -X DELETE "$API/projects/1/tests/1"
```

Expected: `204 No Content`.

## Error Examples for Defense

### Validation error (`400`)

```bash
curl -X POST "$API/projects/1/tests" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "method": "PATCH",
    "endpoint": "",
    "expectedStatus": null
  }'
```

Expected: `400` with `message: "Request validation failed"` and `validationErrors`.

### Not found (`404`)

```bash
curl "$API/projects/9999/report"
```

Expected: `404` with `message: "Project not found"`.

## Short Defense Checklist

- Open Swagger UI and show endpoint groups.
- Create project and test from Swagger or `curl`.
- Update test (`PUT`) and show changed fields.
- Run one test and explain assertions (status/body/jsonpath/header/time).
- Run all tests and open report (`/projects/{id}/report`).
- Show one `400` and one `404` to demonstrate error handling.

## Suggested Commit Message

- `docs: add end-to-end mvp demo flow to readme`
