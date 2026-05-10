import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as testsApi from '../api/tests'
import { ApiError } from '../api/http'
import type { ApiTestResponse, ExecutionResult, TestRunResponse } from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'
import { useI18n } from '../i18n'

export function TestDetailPage() {
  const { t } = useI18n()
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
      setError(e instanceof ApiError ? e.message : t('errors.loadTest'))
    } finally {
      setLoading(false)
    }
  }, [pid, tid, t])

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
      setError(e instanceof ApiError ? e.message : t('errors.runTest'))
    } finally {
      setRunBusy(false)
    }
  }

  if (!Number.isFinite(pid) || !Number.isFinite(tid)) {
    return <p className="muted">{t('errors.invalidIds')}</p>
  }

  return (
    <div className="page">
      <nav className="breadcrumb">
        <Link to="/projects">{t('nav.projects')}</Link>
        <span>/</span>
        <Link to={`/projects/${pid}`}>{t('nav.project')}</Link>
        <span>/</span>
        <span>{test?.name ?? t('nav.test')}</span>
      </nav>

      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      {loading || !test ? (
        <p className="muted">{t('common.loading')}</p>
      ) : (
        <>
          <header className="page-header">
            <div>
              <h1>{test.name}</h1>
              <p>
                <span className="pill">{test.method}</span>{' '}
                <span className="mono">{test.endpoint}</span> → {t('test.expect')}{' '}
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
                {runBusy ? t('actions.running') : t('actions.runNow')}
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => navigate(`/projects/${pid}/tests/${tid}/edit`)}
              >
                {t('actions.edit')}
              </button>
            </div>
          </header>

          {lastRun && (
            <section className="card">
              <h2>{t('test.lastRunResult')}</h2>
              <dl className="dl-grid">
                <dt>{t('test.success')}</dt>
                <dd>{lastRun.success ? t('common.yes') : t('common.no')}</dd>
                <dt>{t('common.http')}</dt>
                <dd>{lastRun.statusCode}</dd>
                <dt>{t('report.testDuration')}</dt>
                <dd>{lastRun.responseTimeMs}</dd>
                <dt>{t('common.error')}</dt>
                <dd className="muted">{lastRun.errorMessage ?? '—'}</dd>
              </dl>
              {lastRun.responseBody && (
                <pre className="code-block">{lastRun.responseBody}</pre>
              )}
            </section>
          )}

          <section className="card">
            <h2>{t('test.definition')}</h2>
            <pre className="code-block">{JSON.stringify(test, null, 2)}</pre>
          </section>

          <section className="card">
            <h2>{t('test.history')}</h2>
            {history.length === 0 ? (
              <p className="muted">{t('test.noRuns')}</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('report.date')}</th>
                    <th>{t('common.status')}</th>
                    <th>{t('common.http')}</th>
                    <th>{t('common.ms')}</th>
                    <th>{t('common.error')}</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((h) => (
                    <tr key={h.id}>
                      <td className="small">{h.executedAt}</td>
                      <td>{h.success ? t('common.yes') : t('common.no')}</td>
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
