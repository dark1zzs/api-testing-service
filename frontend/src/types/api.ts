export type ProjectResponse = {
  id: number
  name: string
  baseUrl: string
  description: string | null
}

export type ProjectRequest = {
  name: string
  baseUrl: string
  description?: string | null
}

export type ApiTestRequest = {
  name: string
  description?: string | null
  testKey?: string | null
  feature?: string | null
  story?: string | null
  method: string
  endpoint: string
  requestBody?: string | null
  expectedResponseBody?: string | null
  expectedJsonPath?: string | null
  expectedJsonValue?: string | null
  expectedHeaderName?: string | null
  expectedHeaderValue?: string | null
  maxResponseTimeMs?: number | null
  expectedStatus: number
  /** Lower runs first when using "Run all tests". */
  runOrder?: number | null
  /** JSON object: header name → value. Use `{{var}}` for values captured earlier in the same batch. */
  requestHeadersJson?: string | null
  /** After a successful run, read this JSONPath from the response body. */
  captureJsonPath?: string | null
  /** Store captured value under this name for later tests in the same batch. */
  captureVariableName?: string | null
}

export type ApiTestResponse = ApiTestRequest & {
  id: number
  projectId: number
}

export type ProjectReportTestResponse = {
  testId: number
  testName: string
  testKey: string | null
  success: boolean
  statusCode: number | null
  responseTimeMs: number | null
  errorMessage: string | null
  lastRunAt: string | null
}

export type ProjectReportRunResponse = {
  startedAt: string
  testsCount: number
  passedCount: number
  failedCount: number
  totalDurationMs: number
}

export type ProjectReportTrendResponse = {
  date: string
  runsCount: number
  passedCount: number
  failedCount: number
  totalDurationMs: number
}

export type ProjectReportResponse = {
  projectId: number
  projectName: string
  totalTests: number
  passedTests: number
  failedTests: number
  notRunTests: number
  successRate: number
  lastRunAt: string | null
  responseTimeSampleCount: number
  responseTimeP50Ms: number | null
  responseTimeP95Ms: number | null
  totalRuns: number
  averageResponseTimeMs: number | null
  lastRunTotalDurationMs: number | null
  tests: ProjectReportTestResponse[]
  recentRuns: ProjectReportRunResponse[]
  trend: ProjectReportTrendResponse[]
}

export type ExecutionResult = {
  success: boolean
  statusCode: number
  responseTimeMs: number
  responseBody: string | null
  errorMessage: string | null
}

export type TestExecutionResponse = {
  testId: number
  testName: string
  success: boolean
  statusCode: number
  responseTimeMs: number
  responseBody: string | null
  errorMessage: string | null
  executedAt: string
}

export type TestRunResponse = {
  id: number
  testId: number
  testName: string
  success: boolean
  statusCode: number
  responseTimeMs: number
  responseBody: string | null
  errorMessage: string | null
  executedAt: string
}
