import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as projectsApi from '../api/projects'
import * as testsApi from '../api/tests'
import { ApiError } from '../api/http'
import type { ApiTestResponse, ProjectResponse, TestExecutionResponse } from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'

export function ProjectPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const navigate = useNavigate()

  const [project, setProject] = useState<ProjectResponse | null>(null)
  const [tests, setTests] = useState<ApiTestResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [runBusy, setRunBusy] = useState(false)
  const [runAllResult, setRunAllResult] = useState<TestExecutionResponse[] | null>(null)

  const load = useCallback(async () => {
    if (!Number.isFinite(id)) return
    setLoading(true)
    setError(null)
    try {
      const [p, t] = await Promise.all([
        projectsApi.getProject(id),
        testsApi.listTests(id),
      ])
      setProject(p)
      setTests(t)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Failed to load project')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    void load()
  }, [load])

  async function runAll() {
    setRunBusy(true)
    setError(null)
    setRunAllResult(null)
    try {
      const res = await testsApi.runProjectTests(id)
      setRunAllResult(res)
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Run all failed')
    } finally {
      setRunBusy(false)
    }
  }

  async function runOne(testId: number) {
    setError(null)
    try {
      await testsApi.runTest(testId)
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Run failed')
    }
  }

  async function removeTest(testId: number, name: string) {
    if (!window.confirm(`Delete test "${name}"?`)) return
    setError(null)
    try {
      await testsApi.deleteTest(id, testId)
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Delete failed')
    }
  }

  if (!Number.isFinite(id)) {
    return <p className="muted">Invalid project id.</p>
  }

  return (
    <div className="page">
      <nav className="breadcrumb">
        <Link to="/projects">Projects</Link>
        <span>/</span>
        <span>{project?.name ?? '…'}</span>
      </nav>

      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      {loading || !project ? (
        <p className="muted">Loading…</p>
      ) : (
        <>
          <header className="page-header">
            <div>
              <h1>{project.name}</h1>
              <p className="mono muted">{project.baseUrl}</p>
              {project.description ? <p>{project.description}</p> : null}
            </div>
            <div className="page-actions">
              <Link to={`/projects/${id}/report`} className="btn btn-secondary">
                Report
              </Link>
              <button
                type="button"
                className="btn btn-primary"
                disabled={runBusy}
                onClick={() => void runAll()}
              >
                {runBusy ? 'Running…' : 'Run all tests'}
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => navigate(`/projects/${id}/tests/new`)}
              >
                New test
              </button>
            </div>
          </header>

          {runAllResult && (
            <section className="card">
              <h2>Last batch run</h2>
              <table className="table">
                <thead>
                  <tr>
                    <th>Test</th>
                    <th>OK</th>
                    <th>HTTP</th>
                    <th>ms</th>
                    <th>Error</th>
                  </tr>
                </thead>
                <tbody>
                  {runAllResult.map((r) => (
                    <tr key={r.testId}>
                      <td>{r.testName}</td>
                      <td>{r.success ? 'yes' : 'no'}</td>
                      <td>{r.statusCode}</td>
                      <td>{r.responseTimeMs}</td>
                      <td className="muted small">{r.errorMessage ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}

          <section className="card">
            <h2>Tests</h2>
            {tests.length === 0 ? (
              <p className="muted">No tests yet. Create one to get started.</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Method</th>
                    <th>Endpoint</th>
                    <th>Expected</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {tests.map((t) => (
                    <tr key={t.id}>
                      <td>
                        <Link to={`/projects/${id}/tests/${t.id}`}>{t.name}</Link>
                      </td>
                      <td>
                        <span className="pill">{t.method}</span>
                      </td>
                      <td className="mono small">{t.endpoint}</td>
                      <td>{t.expectedStatus}</td>
                      <td className="actions">
                        <button
                          type="button"
                          className="btn btn-ghost"
                          onClick={() => void runOne(t.id)}
                        >
                          Run
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => navigate(`/projects/${id}/tests/${t.id}/edit`)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger"
                          onClick={() => void removeTest(t.id, t.name)}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}
    </div>
  )
}
