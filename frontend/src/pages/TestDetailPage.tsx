import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as testsApi from '../api/tests'
import { ApiError } from '../api/http'
import type { ApiTestResponse, ExecutionResult, TestRunResponse } from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'

export function TestDetailPage() {
  const { projectId, testId } = useParams<{ projectId: string; testId: string }>()
  const pid = Number(projectId)
  const tid = Number(testId)
  const navigate = useNavigate()

  const [test, setTest] = useState<ApiTestResponse | null>(null)
  const [history, setHistory] = useState<TestRunResponse[]>([])
  const [lastRun, setLastRun] = useState<ExecutionResult | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [runBusy, setRunBusy] = useState(false)

  const load = useCallback(async () => {
    if (!Number.isFinite(pid) || !Number.isFinite(tid)) return
    setLoading(true)
    setError(null)
    try {
      const [t, h] = await Promise.all([
        testsApi.getTest(pid, tid),
        testsApi.getTestHistory(tid),
      ])
      setTest(t)
      setHistory(h)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Failed to load test')
    } finally {
      setLoading(false)
    }
  }, [pid, tid])

  useEffect(() => {
    void load()
  }, [load])

  async function run() {
    setRunBusy(true)
    setError(null)
    try {
      const r = await testsApi.runTest(tid)
      setLastRun(r)
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Run failed')
    } finally {
      setRunBusy(false)
    }
  }

  if (!Number.isFinite(pid) || !Number.isFinite(tid)) {
    return <p className="muted">Invalid ids.</p>
  }

  return (
    <div className="page">
      <nav className="breadcrumb">
        <Link to="/projects">Projects</Link>
        <span>/</span>
        <Link to={`/projects/${pid}`}>Project</Link>
        <span>/</span>
        <span>{test?.name ?? 'Test'}</span>
      </nav>

      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      {loading || !test ? (
        <p className="muted">Loading…</p>
      ) : (
        <>
          <header className="page-header">
            <div>
              <h1>{test.name}</h1>
              <p>
                <span className="pill">{test.method}</span>{' '}
                <span className="mono">{test.endpoint}</span> → expect{' '}
                <strong>{test.expectedStatus}</strong>
              </p>
            </div>
            <div className="page-actions">
              <button
                type="button"
                className="btn btn-primary"
                disabled={runBusy}
                onClick={() => void run()}
              >
                {runBusy ? 'Running…' : 'Run now'}
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => navigate(`/projects/${pid}/tests/${tid}/edit`)}
              >
                Edit
              </button>
            </div>
          </header>

          {lastRun && (
            <section className="card">
              <h2>Last run result</h2>
              <dl className="dl-grid">
                <dt>Success</dt>
                <dd>{lastRun.success ? 'yes' : 'no'}</dd>
                <dt>HTTP</dt>
                <dd>{lastRun.statusCode}</dd>
                <dt>Time (ms)</dt>
                <dd>{lastRun.responseTimeMs}</dd>
                <dt>Error</dt>
                <dd className="muted">{lastRun.errorMessage ?? '—'}</dd>
              </dl>
              {lastRun.responseBody && (
                <pre className="code-block">{lastRun.responseBody}</pre>
              )}
            </section>
          )}

          <section className="card">
            <h2>Definition</h2>
            <pre className="code-block">{JSON.stringify(test, null, 2)}</pre>
          </section>

          <section className="card">
            <h2>History</h2>
            {history.length === 0 ? (
              <p className="muted">No runs yet.</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>At</th>
                    <th>OK</th>
                    <th>HTTP</th>
                    <th>ms</th>
                    <th>Error</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((h) => (
                    <tr key={h.id}>
                      <td className="small">{h.executedAt}</td>
                      <td>{h.success ? 'yes' : 'no'}</td>
                      <td>{h.statusCode}</td>
                      <td>{h.responseTimeMs}</td>
                      <td className="small muted">{h.errorMessage ?? '—'}</td>
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
