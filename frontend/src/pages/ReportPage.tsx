import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import * as projectsApi from '../api/projects'
import * as testsApi from '../api/tests'
import { ApiError } from '../api/http'
import type {
  ProjectReportResponse,
  ProjectReportTestResponse,
  ProjectReportTrendResponse,
} from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'

const CHART_WIDTH = 640
const CHART_HEIGHT = 190
const CHART_PADDING = 28

function formatDateTime(value: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
  }).format(new Date(value))
}

function formatMs(value: number | null | undefined) {
  return value === null || value === undefined ? '—' : `${value} ms`
}

function statusLabel(test: ProjectReportTestResponse) {
  if (!test.lastRunAt) return 'not run'
  return test.success ? 'passed' : 'failed'
}

function buildLinePoints(trend: ProjectReportTrendResponse[]) {
  if (trend.length === 0) return ''
  const maxDuration = Math.max(...trend.map((item) => item.totalDurationMs), 1)
  const usableWidth = CHART_WIDTH - CHART_PADDING * 2
  const usableHeight = CHART_HEIGHT - CHART_PADDING * 2

  return trend
    .map((item, index) => {
      const x =
        trend.length === 1
          ? CHART_WIDTH / 2
          : CHART_PADDING + (index * usableWidth) / (trend.length - 1)
      const y =
        CHART_HEIGHT -
        CHART_PADDING -
        (item.totalDurationMs * usableHeight) / maxDuration
      return `${x},${y}`
    })
    .join(' ')
}

function ResultPie({ report }: { report: ProjectReportResponse }) {
  const total = Math.max(report.totalTests, 1)
  const passed = (report.passedTests / total) * 100
  const failed = (report.failedTests / total) * 100
  const pieStyle = {
    background: `conic-gradient(#73d18b 0 ${passed}%, #f07178 ${passed}% ${
      passed + failed
    }%, #5a6378 ${passed + failed}% 100%)`,
  }

  return (
    <section className="card chart-card">
      <div>
        <h2>Result split</h2>
        <p className="muted small">Current status by latest run per test</p>
      </div>
      <div className="pie-wrap">
        <div className="pie" style={pieStyle} aria-label="Test result split" />
        <div className="legend">
          <span>
            <i className="legend-dot ok" /> Passed: {report.passedTests}
          </span>
          <span>
            <i className="legend-dot bad" /> Failed: {report.failedTests}
          </span>
          <span>
            <i className="legend-dot neutral" /> Not run: {report.notRunTests}
          </span>
        </div>
      </div>
    </section>
  )
}

function DurationBars({ tests }: { tests: ProjectReportTestResponse[] }) {
  const maxMs = Math.max(...tests.map((test) => test.responseTimeMs ?? 0), 1)

  return (
    <section className="card chart-card">
      <div>
        <h2>Test duration</h2>
        <p className="muted small">Latest response time for each test</p>
      </div>
      <div className="bar-list">
        {tests.map((test) => {
          const value = test.responseTimeMs ?? 0
          const width = `${Math.max((value / maxMs) * 100, test.lastRunAt ? 4 : 0)}%`
          return (
            <div className="bar-row" key={test.testId}>
              <span className="bar-label">{test.testName}</span>
              <div className="bar-track">
                <span className="bar-fill" style={{ width }} />
              </div>
              <span className="bar-value">{formatMs(test.responseTimeMs)}</span>
            </div>
          )
        })}
      </div>
    </section>
  )
}

function DurationTrend({ trend }: { trend: ProjectReportTrendResponse[] }) {
  const points = buildLinePoints(trend)

  return (
    <section className="card chart-card chart-wide">
      <div>
        <h2>Run duration trend</h2>
        <p className="muted small">Total saved response time by date</p>
      </div>
      {trend.length === 0 ? (
        <p className="muted">No execution data yet.</p>
      ) : (
        <div className="line-chart-wrap">
          <svg
            className="line-chart"
            viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
            role="img"
            aria-label="Run duration trend"
          >
            <line
              x1={CHART_PADDING}
              y1={CHART_HEIGHT - CHART_PADDING}
              x2={CHART_WIDTH - CHART_PADDING}
              y2={CHART_HEIGHT - CHART_PADDING}
              className="chart-axis"
            />
            <polyline points={points} className="chart-line" />
            {trend.map((item, index) => {
              const coords = points.split(' ')[index]?.split(',') ?? ['0', '0']
              return (
                <g key={item.date}>
                  <circle cx={coords[0]} cy={coords[1]} r="4" className="chart-point" />
                  <text x={coords[0]} y={CHART_HEIGHT - 6} textAnchor="middle">
                    {formatDate(item.date)}
                  </text>
                </g>
              )
            })}
          </svg>
        </div>
      )}
    </section>
  )
}

export function ReportPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const [report, setReport] = useState<ProjectReportResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [runBusy, setRunBusy] = useState(false)

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

  async function runAll() {
    setRunBusy(true)
    setError(null)
    try {
      await testsApi.runProjectTests(id)
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Run all failed')
    } finally {
      setRunBusy(false)
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
        <Link to={`/projects/${id}`}>Project</Link>
        <span>/</span>
        <span>Report</span>
      </nav>

      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      {loading ? (
        <p className="muted">Loading…</p>
      ) : !report ? null : (
        <>
          <header className="page-header">
            <div>
              <h1>Report: {report.projectName}</h1>
              <p className="muted">
                Last run: {formatDateTime(report.lastRunAt)} · saved runs:{' '}
                {report.totalRuns}
              </p>
            </div>
            <button
              type="button"
              className="btn btn-primary"
              disabled={runBusy}
              onClick={() => void runAll()}
            >
              {runBusy ? 'Running…' : 'Run all tests'}
            </button>
          </header>

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
              <span className="stat-label">p50 / p95</span>
              <span className="stat-value">
                {formatMs(report.responseTimeP50Ms)} / {formatMs(report.responseTimeP95Ms)}
              </span>
              <span className="stat-hint">n={report.responseTimeSampleCount}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Avg response</span>
              <span className="stat-value">{formatMs(report.averageResponseTimeMs)}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Last run duration</span>
              <span className="stat-value">{formatMs(report.lastRunTotalDurationMs)}</span>
            </div>
          </section>

          <section className="insight-card">
            <h2>Executive summary</h2>
            <p>
              Project has <strong>{report.totalTests}</strong> tests, current success
              rate is <strong>{report.successRate}%</strong>. The latest measured
              full run duration is{' '}
              <strong>{formatMs(report.lastRunTotalDurationMs)}</strong>, with
              average response time <strong>{formatMs(report.averageResponseTimeMs)}</strong>.
            </p>
          </section>

          <div className="dashboard-grid">
            <ResultPie report={report} />
            <DurationBars tests={report.tests} />
            <DurationTrend trend={report.trend} />
          </div>

          <section className="card">
            <h2>Recent runs</h2>
            {report.recentRuns.length === 0 ? (
              <p className="muted">No runs yet.</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Tests</th>
                    <th>Passed</th>
                    <th>Failed</th>
                    <th>Total duration</th>
                  </tr>
                </thead>
                <tbody>
                  {report.recentRuns.map((run) => (
                    <tr key={run.startedAt}>
                      <td className="small">{formatDateTime(run.startedAt)}</td>
                      <td>{run.testsCount}</td>
                      <td>{run.passedCount}</td>
                      <td>{run.failedCount}</td>
                      <td>{formatMs(run.totalDurationMs)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>

          <section className="card">
            <h2>Per test</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>Test</th>
                  <th>Status</th>
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
                    <td>{statusLabel(t)}</td>
                    <td>{t.statusCode ?? '—'}</td>
                    <td>{formatMs(t.responseTimeMs)}</td>
                    <td className="small muted">{formatDateTime(t.lastRunAt)}</td>
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
