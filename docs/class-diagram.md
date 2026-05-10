# Class Diagram

```mermaid
classDiagram
    direction LR

    class Project {
        Long id
        String name
        String baseUrl
        String description
    }

    class ApiTest {
        Long id
        String name
        String description
        String testKey
        String method
        String endpoint
        String requestBody
        String requestHeadersJson
        String captureJsonPath
        String captureVariableName
        int runOrder
        String expectedResponseBody
        String expectedJsonPath
        String expectedJsonValue
        String expectedHeaderName
        String expectedHeaderValue
        Long maxResponseTimeMs
        Integer expectedStatus
    }

    class TestRun {
        Long id
        boolean success
        int statusCode
        Long responseTimeMs
        String responseBody
        String errorMessage
        LocalDateTime executedAt
    }

    class ProjectController {
        createProject(ProjectRequest) ProjectResponse
        getAllProjects() List~ProjectResponse~
        getProjectById(Long) ProjectResponse
        getProjectReport(Long) ProjectReportResponse
        updateProject(Long, ProjectRequest) ProjectResponse
        deleteProject(Long) void
    }

    class ApiTestController {
        createApiTest(Long, ApiTestRequest) ApiTestResponse
        getTestsByProjectId(Long) List~ApiTestResponse~
        getApiTest(Long, Long) ApiTestResponse
        updateApiTest(Long, Long, ApiTestRequest) ApiTestResponse
        deleteApiTest(Long, Long) void
        runProjectTests(Long) List~TestExecutionResponse~
    }

    class TestExecutionController {
        runTest(Long) ExecutionResult
        getHistory(Long) List~TestRunResponse~
    }

    class ProjectService {
        createProject(ProjectRequest) ProjectResponse
        getAllProjects() List~ProjectResponse~
        getProjectById(Long) ProjectResponse
        updateProject(Long, ProjectRequest) ProjectResponse
        deleteProject(Long) void
        getProjectReport(Long) ProjectReportResponse
    }

    class ApiTestService {
        createApiTest(Long, ApiTestRequest) ApiTestResponse
        getTestsByProjectId(Long) List~ApiTestResponse~
        getApiTest(Long, Long) ApiTestResponse
        updateApiTest(Long, Long, ApiTestRequest) ApiTestResponse
        deleteApiTest(Long, Long) void
    }

    class TestExecutionService {
        runTest(Long) ExecutionResult
        runProjectTests(Long) List~TestExecutionResponse~
        getTestHistory(Long) List~TestRunResponse~
    }

    class ApiTestExecutor {
        execute(ApiTestExecutionRequest) ExecutionResult
    }

    class ProjectRepository
    class ApiTestRepository
    class TestRunRepository

    class ProjectRequest {
        String name
        String baseUrl
        String description
    }

    class ProjectResponse {
        Long id
        String name
        String baseUrl
        String description
    }

    class ApiTestRequest {
        String name
        String description
        String testKey
        String method
        String endpoint
        String requestBody
        String expectedResponseBody
        String expectedJsonPath
        String expectedJsonValue
        String expectedHeaderName
        String expectedHeaderValue
        Long maxResponseTimeMs
        Integer expectedStatus
        Integer runOrder
        String requestHeadersJson
        String captureJsonPath
        String captureVariableName
    }

    class ApiTestResponse {
        Long id
        String name
        String description
        String testKey
        String method
        String endpoint
        Long projectId
    }

    class ApiTestExecutionRequest {
        String baseUrl
        String endpoint
        String method
        String requestBody
        Map requestHeaders
        Integer expectedStatus
    }

    class ExecutionResult {
        boolean success
        int statusCode
        Long responseTimeMs
        String responseBody
        String errorMessage
    }

    class TestExecutionResponse {
        Long testId
        String testName
        boolean success
        int statusCode
        Long responseTimeMs
        String responseBody
        String errorMessage
        LocalDateTime executedAt
    }

    class TestRunResponse {
        Long id
        Long testId
        String testName
        boolean success
        int statusCode
        Long responseTimeMs
        String responseBody
        String errorMessage
        LocalDateTime executedAt
    }

    class ProjectReportResponse {
        Long projectId
        String projectName
        long totalTests
        long passedTests
        long failedTests
        long notRunTests
        double successRate
        LocalDateTime lastRunAt
        long responseTimeSampleCount
        Long responseTimeP50Ms
        Long responseTimeP95Ms
    }

    class ProjectReportTestResponse {
        Long testId
        String testName
        String testKey
        Boolean success
        Integer statusCode
        Long responseTimeMs
        String errorMessage
        LocalDateTime lastRunAt
    }

    class GlobalExceptionHandler {
        handleNotFound(NotFoundException)
        handleValidation(MethodArgumentNotValidException)
        handleUnreadableMessage(HttpMessageNotReadableException)
        handleIllegalArgument(IllegalArgumentException)
        handleUnexpected(Exception)
    }

    class ErrorResponse {
        LocalDateTime timestamp
        int status
        String error
        String message
        Map validationErrors
    }

    Project "1" --> "0..*" ApiTest : contains
    ApiTest "1" --> "0..*" TestRun : has history

    ProjectController --> ProjectService
    ApiTestController --> ApiTestService
    ApiTestController --> TestExecutionService
    TestExecutionController --> TestExecutionService

    ProjectService --> ProjectRepository
    ProjectService --> ApiTestRepository
    ProjectService --> TestRunRepository
    ApiTestService --> ProjectRepository
    ApiTestService --> ApiTestRepository
    TestExecutionService --> ProjectRepository
    TestExecutionService --> ApiTestRepository
    TestExecutionService --> TestRunRepository
    TestExecutionService --> ApiTestExecutor

    ProjectRepository --> Project
    ApiTestRepository --> ApiTest
    TestRunRepository --> TestRun

    ProjectController ..> ProjectRequest
    ProjectController ..> ProjectResponse
    ProjectController ..> ProjectReportResponse
    ApiTestController ..> ApiTestRequest
    ApiTestController ..> ApiTestResponse
    ApiTestController ..> TestExecutionResponse
    TestExecutionController ..> ExecutionResult
    TestExecutionController ..> TestRunResponse

    ProjectService ..> ProjectReportTestResponse
    TestExecutionService ..> ApiTestExecutionRequest
    ApiTestExecutor ..> ApiTestExecutionRequest
    ApiTestExecutor ..> ExecutionResult
    GlobalExceptionHandler ..> ErrorResponse
```

## Main Structure

- `Project`является корневой сущностью системы. Она хранит базовый URL тестируемого API.
- `ApiTest` принадлежит определённому проекту и описывает одну проверку API: HTTP-метод, endpoint, ожидаемый статус ответа, проверки содержимого ответа, заголовки, ограничение времени ответа, порядок выполнения и возможность сохранения переменных.
- `TestRun` представляет собой сохранённый результат выполнения API-теста.
Контроллеры предоставляют REST endpoint’ы и передают выполнение бизнес-логики сервисному слою.
Сервисы содержат основную бизнес-логику приложения: CRUD-операции, пакетный запуск тестов, хранение истории выполнения и формирование отчётов по проекту.
Репозитории изолируют работу с базой данных с использованием Spring Data JPA.
DTO и model-классы разделяют данные HTTP-запросов и ответов от JPA-сущностей.
- `ApiTestExecutor` выполняет исходящий HTTP-запрос к тестируемому API и возвращает результат выполнения в виде объекта ExecutionResult.
- `GlobalExceptionHandler` обеспечивает централизованную обработку исключений и преобразует ошибки в единообразные HTTP-ответы API.