# API Testing Service (Diploma MVP)

Backend service for automated API testing, similar to a lightweight Postman/Newman-style runner:
- create a project with a `baseUrl`;
- define API tests inside the project;
- run a single test or all tests in the project;
- persist run history;
- build a project-level quality report.

## Stack

- Java 21
- Spring Boot (Web MVC, Data JPA, Validation)
- H2 database
- springdoc-openapi (Swagger UI)
- React 19 + TypeScript + Vite (`frontend/`)

### Frontend (monorepo)

The UI lives in `frontend/` (Vite + React + TypeScript + React Router). It calls the Spring API using `VITE_API_BASE` (default `http://localhost:8080` in code if unset).

**CORS** for local dev is enabled for `http://localhost:*` and `http://127.0.0.1:*` (see `WebConfig`). In production, narrow `allowedOriginPatterns` to your real domain.

Run the UI (with the backend already on port 8080):

```bash
cd frontend
cp .env.example .env.development   # optional; default base URL works without file
npm install
npm run dev
```

Then open the URL printed by Vite (usually `http://localhost:5173`).

## Run (backend)

```bash
./mvnw spring-boot:run
```

Useful URLs after startup:
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

### Request headers and auth chaining (batch: `POST /projects/{projectId}/tests/run`)

- **`requestHeadersJson`** — JSON object of outbound request headers, e.g. `{"Authorization":"{{token}}","Content-Type":"application/json"}`.
- **`{{variable}}`** — placeholders in header values and in **`requestBody`** are replaced from the batch context (filled by earlier steps in the same run).
- **`captureJsonPath`** + **`captureVariableName`** — after a **successful** response, the service reads this JSONPath from the response body and stores `String.valueOf(...)` under the given variable name for later steps.
- **`runOrder`** — ascending execution order when running all tests (lower runs first). Tie-breaker: test `id`.

Single-test execution (`POST /tests/{testId}/run`) uses an **empty** variable map, so placeholders are not filled from other tests.

#### Example (login → booking)

Project `baseUrl`: `https://automationintesting.online`

**Test A — login (`runOrder`: 0)** — assert `200` and that the body mentions `token`, then capture `$.token` into variable `token`:

```json
{
  "name": "Login",
  "method": "POST",
  "endpoint": "/api/auth/login",
  "requestBody": "{\"username\":\"admin\",\"password\":\"password\"}",
  "expectedStatus": 200,
  "expectedResponseBody": "\"token\"",
  "runOrder": 0,
  "captureJsonPath": "$.token",
  "captureVariableName": "token"
}
```

**Test B — booking (`runOrder`: 1)** — send the token as a cookie (adjust header names to match your API):

```json
{
  "name": "Create booking",
  "method": "POST",
  "endpoint": "/api/booking",
  "requestHeadersJson": "{\"Cookie\":\"token={{token}}\",\"Content-Type\":\"application/json\"}",
  "requestBody": "{\"roomid\":1,\"firstname\":\"ivan\",\"lastname\":\"ivanov\",\"depositpaid\":false,\"bookingdates\":{\"checkin\":\"2026-05-05\",\"checkout\":\"2026-05-06\"},\"email\":\"ivan1Ivanov@gmail.com\",\"phone\":\"+12233331112\"}",
  "expectedStatus": 201,
  "runOrder": 1
}
```

If your API expects the raw token in `Authorization` instead, use e.g. `"requestHeadersJson": "{\"Authorization\":\"{{token}}\"}"`.

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
- `responseTimeSampleCount` — number of stored runs in the project with non-null `responseTimeMs >= 0` (**all time**)
- `responseTimeP50Ms`, `responseTimeP95Ms` — response-time percentiles over that sample (`null` if there are no timed runs yet)
- `tests[]` with per-test details (`statusCode`, `responseTimeMs`, `errorMessage`, `lastRunAt`)

### 8) Delete test (CRUD check)

```bash
curl -i -X DELETE "$API/projects/1/tests/1"
```

Expected: `204 No Content`.

## Error Examples (demo / defense)

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

## Short Demo Checklist

- Open Swagger UI and show endpoint groups.
- Create project and test from Swagger or `curl`.
- Update test (`PUT`) and show changed fields.
- Run one test and explain assertions (status/body/jsonpath/header/time).
- Run all tests and open report (`/projects/{id}/report`).
- Show latency aggregates: `responseTimeP50Ms` / `responseTimeP95Ms` over full project history.
- Show one `400` and one `404` to demonstrate error handling.

## Suggested Commit Message

- `docs: add end-to-end mvp demo flow to readme`
