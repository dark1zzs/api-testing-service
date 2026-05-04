import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as testsApi from '../api/tests'
import { ApiError } from '../api/http'
import type { ApiTestRequest } from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'

const METHODS = ['GET', 'POST', 'PUT', 'DELETE'] as const

function emptyToNull(s: string): string | null {
  const t = s.trim()
  return t === '' ? null : t
}

export function TestFormPage() {
  const { projectId, testId } = useParams<{ projectId: string; testId?: string }>()
  const pid = Number(projectId)
  const isNew = testId === undefined
  const tid = isNew ? null : Number(testId)
  const navigate = useNavigate()

  const [loading, setLoading] = useState(() => !isNew)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<ApiTestRequest>({
    name: '',
    description: '',
    testKey: '',
    method: 'GET',
    endpoint: '',
    requestBody: '',
    expectedResponseBody: '',
    expectedJsonPath: '',
    expectedJsonValue: '',
    expectedHeaderName: '',
    expectedHeaderValue: '',
    maxResponseTimeMs: undefined,
    expectedStatus: 200,
  })

  const load = useCallback(async () => {
    if (!Number.isFinite(pid) || tid === null) return
    setLoading(true)
    setError(null)
    try {
      const t = await testsApi.getTest(pid, tid)
      setForm({
        name: t.name,
        description: t.description ?? '',
        testKey: t.testKey ?? '',
        method: t.method,
        endpoint: t.endpoint,
        requestBody: t.requestBody ?? '',
        expectedResponseBody: t.expectedResponseBody ?? '',
        expectedJsonPath: t.expectedJsonPath ?? '',
        expectedJsonValue: t.expectedJsonValue ?? '',
        expectedHeaderName: t.expectedHeaderName ?? '',
        expectedHeaderValue: t.expectedHeaderValue ?? '',
        maxResponseTimeMs: t.maxResponseTimeMs ?? undefined,
        expectedStatus: t.expectedStatus,
      })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Failed to load test')
    } finally {
      setLoading(false)
    }
  }, [pid, tid])

  useEffect(() => {
    if (isNew) {
      return
    }
    void load()
  }, [isNew, load])

  function buildPayload(): ApiTestRequest {
    const maxMs = form.maxResponseTimeMs
    return {
      name: form.name.trim(),
      description: emptyToNull(form.description ?? ''),
      testKey: emptyToNull(form.testKey ?? ''),
      method: form.method,
      endpoint: form.endpoint.trim(),
      requestBody: emptyToNull(form.requestBody ?? ''),
      expectedResponseBody: emptyToNull(form.expectedResponseBody ?? ''),
      expectedJsonPath: emptyToNull(form.expectedJsonPath ?? ''),
      expectedJsonValue: emptyToNull(form.expectedJsonValue ?? ''),
      expectedHeaderName: emptyToNull(form.expectedHeaderName ?? ''),
      expectedHeaderValue: emptyToNull(form.expectedHeaderValue ?? ''),
      maxResponseTimeMs: maxMs === undefined || maxMs === null || String(maxMs) === '' ? null : Number(maxMs),
      expectedStatus: Number(form.expectedStatus),
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const body = buildPayload()
      if (isNew) {
        await testsApi.createTest(pid, body)
      } else if (tid !== null) {
        await testsApi.updateTest(pid, tid, body)
      }
      navigate(`/projects/${pid}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Save failed')
    }
  }

  if (!Number.isFinite(pid)) {
    return <p className="muted">Invalid project id.</p>
  }

  return (
    <div className="page">
      <nav className="breadcrumb">
        <Link to="/projects">Projects</Link>
        <span>/</span>
        <Link to={`/projects/${pid}`}>Project</Link>
        <span>/</span>
        <span>{isNew ? 'New test' : 'Edit test'}</span>
      </nav>

      <h1>{isNew ? 'New API test' : 'Edit API test'}</h1>
      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <form className="card form-stack" onSubmit={onSubmit}>
          <label>
            Name *
            <input
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
          </label>
          <label>
            Description
            <input
              value={form.description ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </label>
          <label>
            Test key
            <input
              value={form.testKey ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, testKey: e.target.value }))}
            />
          </label>
          <div className="form-row">
            <label>
              Method *
              <select
                value={form.method}
                onChange={(e) => setForm((f) => ({ ...f, method: e.target.value }))}
              >
                {METHODS.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Expected HTTP status *
              <input
                type="number"
                required
                value={form.expectedStatus}
                onChange={(e) =>
                  setForm((f) => ({ ...f, expectedStatus: Number(e.target.value) }))
                }
              />
            </label>
            <label>
              Max response time (ms)
              <input
                type="number"
                min={0}
                value={form.maxResponseTimeMs ?? ''}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    maxResponseTimeMs: e.target.value === '' ? undefined : Number(e.target.value),
                  }))
                }
              />
            </label>
          </div>
          <label>
            Endpoint path *
            <input
              required
              placeholder="/posts/1"
              value={form.endpoint}
              onChange={(e) => setForm((f) => ({ ...f, endpoint: e.target.value }))}
            />
          </label>
          <label>
            Request body (raw)
            <textarea
              rows={4}
              className="mono"
              value={form.requestBody ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, requestBody: e.target.value }))}
            />
          </label>
          <label>
            Expected response body (substring)
            <input
              value={form.expectedResponseBody ?? ''}
              onChange={(e) =>
                setForm((f) => ({ ...f, expectedResponseBody: e.target.value }))
              }
            />
          </label>
          <div className="form-row">
            <label>
              JSONPath
              <input
                placeholder="$.id"
                value={form.expectedJsonPath ?? ''}
                onChange={(e) =>
                  setForm((f) => ({ ...f, expectedJsonPath: e.target.value }))
                }
              />
            </label>
            <label>
              Expected JSON value (string)
              <input
                value={form.expectedJsonValue ?? ''}
                onChange={(e) =>
                  setForm((f) => ({ ...f, expectedJsonValue: e.target.value }))
                }
              />
            </label>
          </div>
          <div className="form-row">
            <label>
              Expected header name
              <input
                value={form.expectedHeaderName ?? ''}
                onChange={(e) =>
                  setForm((f) => ({ ...f, expectedHeaderName: e.target.value }))
                }
              />
            </label>
            <label>
              Expected header value (substring)
              <input
                value={form.expectedHeaderValue ?? ''}
                onChange={(e) =>
                  setForm((f) => ({ ...f, expectedHeaderValue: e.target.value }))
                }
              />
            </label>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary">
              Save
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate(`/projects/${pid}`)}
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
