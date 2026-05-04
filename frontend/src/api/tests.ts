import { apiFetch } from './http'
import type {
  ApiTestRequest,
  ApiTestResponse,
  ExecutionResult,
  TestExecutionResponse,
  TestRunResponse,
} from '../types/api'

export function listTests(projectId: number) {
  return apiFetch<ApiTestResponse[]>(`/projects/${projectId}/tests`)
}

export function getTest(projectId: number, testId: number) {
  return apiFetch<ApiTestResponse>(`/projects/${projectId}/tests/${testId}`)
}

export function createTest(projectId: number, body: ApiTestRequest) {
  return apiFetch<ApiTestResponse>(`/projects/${projectId}/tests`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateTest(projectId: number, testId: number, body: ApiTestRequest) {
  return apiFetch<ApiTestResponse>(`/projects/${projectId}/tests/${testId}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deleteTest(projectId: number, testId: number) {
  return apiFetch<void>(`/projects/${projectId}/tests/${testId}`, {
    method: 'DELETE',
  })
}

export function runProjectTests(projectId: number) {
  return apiFetch<TestExecutionResponse[]>(`/projects/${projectId}/tests/run`, {
    method: 'POST',
  })
}

export function runTest(testId: number) {
  return apiFetch<ExecutionResult>(`/tests/${testId}/run`, { method: 'POST' })
}

export function getTestHistory(testId: number) {
  return apiFetch<TestRunResponse[]>(`/tests/${testId}/history`)
}
