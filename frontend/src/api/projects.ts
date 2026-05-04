import { apiFetch } from './http'
import type {
  ProjectReportResponse,
  ProjectRequest,
  ProjectResponse,
} from '../types/api'

export function listProjects() {
  return apiFetch<ProjectResponse[]>('/projects')
}

export function getProject(id: number) {
  return apiFetch<ProjectResponse>(`/projects/${id}`)
}

export function createProject(body: ProjectRequest) {
  return apiFetch<ProjectResponse>('/projects', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateProject(id: number, body: ProjectRequest) {
  return apiFetch<ProjectResponse>(`/projects/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deleteProject(id: number) {
  return apiFetch<void>(`/projects/${id}`, { method: 'DELETE' })
}

export function getProjectReport(id: number) {
  return apiFetch<ProjectReportResponse>(`/projects/${id}/report`)
}
