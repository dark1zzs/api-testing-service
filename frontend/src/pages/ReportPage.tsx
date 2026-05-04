import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import * as projectsApi from '../api/projects'
import { ApiError } from '../api/http'
import type { ProjectReportResponse } from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'

export function ReportPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const [report, setReport] = useState<ProjectReportResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!Number.isFinite(id)) return
    setLoading(true)
    setError(null)
    try {
      const r = await projectsApi.getProjectReport(id)
      setReport(r)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Failed to load report')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    void load()
  }, [load])

  if (!Number.isFinite(id)) {
    return <p className="muted">Invalid project id.</p>
  }

  return (
    <div className="page">
      <nav className="breadcrumb">
        <Link to="/projects">Projects</Link>
        <span>/</span>
        <Link to={`/projects/${id}`}>Project</Link>
        <span>/</span>
        <span>Report</span>
      </nav>

      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      {loading ? (
        <p className="muted">Loading…</p>
      ) : !report ? null : (
        <>
          <h1>Report: {report.projectName}</h1>

          <section className="stats">
            <div className="stat">
              <span className="stat-label">Total</span>
              <span className="stat-value">{report.totalTests}</span>
            </div>
            <div className="stat stat-ok">
              <span className="stat-label">Passed</span>
              <span className="stat-value">{report.passedTests}</span>
            </div>
            <div className="stat stat-bad">
              <span className="stat-label">Failed</span>
              <span className="stat-value">{report.failedTests}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Not run</span>
              <span className="stat-value">{report.notRunTests}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Success rate</span>
              <span className="stat-value">{report.successRate}%</span>
            </div>
            <div className="stat">
              <span className="stat-label">p50 / p95 (ms)</span>
              <span className="stat-value">
                {report.responseTimeP50Ms ?? '—'} / {report.responseTimeP95Ms ?? '—'}
              </span>
              <span className="stat-hint">n={report.responseTimeSampleCount}</span>
            </div>
          </section>

          <section className="card">
            <h2>Per test</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>Test</th>
                  <th>OK</th>
                  <th>HTTP</th>
                  <th>ms</th>
                  <th>Last run</th>
                  <th>Error</th>
                </tr>
              </thead>
              <tbody>
                {report.tests.map((t) => (
                  <tr key={t.testId}>
                    <td>{t.testName}</td>
                    <td>{t.lastRunAt ? (t.success ? 'yes' : 'no') : '—'}</td>
                    <td>{t.statusCode ?? '—'}</td>
                    <td>{t.responseTimeMs ?? '—'}</td>
                    <td className="small muted">{t.lastRunAt ?? '—'}</td>
                    <td className="small muted">{t.errorMessage ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        </>
      )}
    </div>
  )
}
