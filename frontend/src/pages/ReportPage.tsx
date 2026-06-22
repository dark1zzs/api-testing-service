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
import { useI18n } from '../i18n'
import type { Language } from '../i18n'

const CHART_WIDTH = 640
const CHART_HEIGHT = 190
const CHART_PADDING = 28
const REPORT_PERIODS = ['ALL', 'WEEK', 'MONTH'] as const

type ReportPeriod = (typeof REPORT_PERIODS)[number]

const REPORT_PERIOD_LABELS: Record<
  ReportPeriod,
  'report.period.all' | 'report.period.week' | 'report.period.month'
> = {
  ALL: 'report.period.all',
  WEEK: 'report.period.week',
  MONTH: 'report.period.month',
}

function locale(language: Language) {
  return language === 'ru' ? 'ru-RU' : 'en-US'
}

function formatDateTime(value: string | null, language: Language) {
  if (!value) return '—'
  return new Intl.DateTimeFormat(locale(language), {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatDate(value: string, language: Language) {
  return new Intl.DateTimeFormat(locale(language), {
    day: '2-digit',
    month: '2-digit',
  }).format(new Date(value))
}

function formatMs(value: number | null | undefined) {
  return value === null || value === undefined ? '—' : `${value} ms`
}

function statusLabel(
  test: ProjectReportTestResponse,
  t: ReturnType<typeof useI18n>['t'],
) {
  if (!test.lastRunAt) return t('report.notRun')
  return test.success ? t('report.passed') : t('report.failed')
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

function ResultPie({
  report,
  t,
}: {
  report: ProjectReportResponse
  t: ReturnType<typeof useI18n>['t']
}) {
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
        <h2>{t('report.resultSplit')}</h2>
        <p className="muted small">{t('report.currentStatusHint')}</p>
      </div>
      <div className="pie-wrap">
        <div className="pie" style={pieStyle} aria-label={t('report.resultSplit')} />
        <div className="legend">
          <span>
            <i className="legend-dot ok" /> {t('report.passed')}: {report.passedTests}
          </span>
          <span>
            <i className="legend-dot bad" /> {t('report.failed')}: {report.failedTests}
          </span>
          <span>
            <i className="legend-dot neutral" /> {t('report.notRun')}: {report.notRunTests}
          </span>
        </div>
      </div>
    </section>
  )
}

function DurationBars({
  tests,
  t,
}: {
  tests: ProjectReportTestResponse[]
  t: ReturnType<typeof useI18n>['t']
}) {
  const maxMs = Math.max(...tests.map((test) => test.responseTimeMs ?? 0), 1)

  return (
    <section className="card chart-card">
      <div>
        <h2>{t('report.testDuration')}</h2>
        <p className="muted small">{t('report.testDurationHint')}</p>
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

function DurationTrend({
  trend,
  language,
  t,
}: {
  trend: ProjectReportTrendResponse[]
  language: Language
  t: ReturnType<typeof useI18n>['t']
}) {
  const points = buildLinePoints(trend)

  return (
    <section className="card chart-card chart-wide">
      <div>
        <h2>{t('report.trend')}</h2>
        <p className="muted small">{t('report.trendHint')}</p>
      </div>
      {trend.length === 0 ? (
        <p className="muted">{t('report.noData')}</p>
      ) : (
        <div className="line-chart-wrap">
          <svg
            className="line-chart"
            viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
            role="img"
            aria-label={t('report.trend')}
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
                    {formatDate(item.date, language)}
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
  const { language, t } = useI18n()
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const [report, setReport] = useState<ProjectReportResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [runBusy, setRunBusy] = useState(false)
  const [period, setPeriod] = useState<ReportPeriod>('ALL')

  const load = useCallback(async () => {
    if (!Number.isFinite(id)) return
    setLoading(true)
    setError(null)
    try {
      const r = await projectsApi.getProjectReport(id, period)
      setReport(r)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : t('errors.loadReport'))
    } finally {
      setLoading(false)
    }
  }, [id, period, t])

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
      setError(e instanceof ApiError ? e.message : t('errors.runAll'))
    } finally {
      setRunBusy(false)
    }
  }

  if (!Number.isFinite(id)) {
    return <p className="muted">{t('errors.invalidProjectId')}</p>
  }

  return (
    <div className="page">
      <nav className="breadcrumb">
        <Link to="/projects">{t('nav.projects')}</Link>
        <span>/</span>
        <Link to={`/projects/${id}`}>{t('nav.project')}</Link>
        <span>/</span>
        <span>{t('nav.report')}</span>
      </nav>

      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      {loading ? (
        <p className="muted">{t('common.loading')}</p>
      ) : !report ? null : (
        <>
          <header className="page-header">
            <div>
              <h1>{t('report.title')}</h1>
              <p className="muted">
                {report.projectName} · {t('report.lastRun')}:{' '}
                {formatDateTime(report.lastRunAt, language)} · {t('report.savedRuns')}:{' '}
                {report.totalRuns}
              </p>
            </div>
            <div className="page-actions">
              <div className="segmented-control" aria-label={t('report.period')}>
                {REPORT_PERIODS.map((value) => (
                  <button
                    key={value}
                    type="button"
                    className={period === value ? 'active' : ''}
                    onClick={() => setPeriod(value)}
                  >
                    {t(REPORT_PERIOD_LABELS[value])}
                  </button>
                ))}
              </div>
              <button
                type="button"
                className="btn btn-primary"
                disabled={runBusy}
                onClick={() => void runAll()}
              >
                {runBusy ? t('actions.running') : t('actions.runAll')}
              </button>
            </div>
          </header>

          <section className="stats">
            <div className="stat">
              <span className="stat-label">{t('report.total')}</span>
              <span className="stat-value">{report.totalTests}</span>
            </div>
            <div className="stat stat-ok">
              <span className="stat-label">{t('report.passed')}</span>
              <span className="stat-value">{report.passedTests}</span>
            </div>
            <div className="stat stat-bad">
              <span className="stat-label">{t('report.failed')}</span>
              <span className="stat-value">{report.failedTests}</span>
            </div>
            <div className="stat">
              <span className="stat-label">{t('report.notRun')}</span>
              <span className="stat-value">{report.notRunTests}</span>
            </div>
            <div className="stat">
              <span className="stat-label">{t('report.successRate')}</span>
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
              <span className="stat-label">{t('report.avgResponse')}</span>
              <span className="stat-value">{formatMs(report.averageResponseTimeMs)}</span>
            </div>
            <div className="stat">
              <span className="stat-label">{t('report.lastRunDuration')}</span>
              <span className="stat-value">{formatMs(report.lastRunTotalDurationMs)}</span>
            </div>
          </section>

          <section className="insight-card">
            <h2>{t('report.executiveSummary')}</h2>
            <p>
              {t('report.executiveSummaryText', {
                total: report.totalTests,
                rate: report.successRate,
                duration: formatMs(report.lastRunTotalDurationMs),
                avg: formatMs(report.averageResponseTimeMs),
              })}
            </p>
          </section>

          <div className="dashboard-grid">
            <ResultPie report={report} t={t} />
            <DurationBars tests={report.tests} t={t} />
            <DurationTrend trend={report.trend} language={language} t={t} />
          </div>

          <section className="card">
            <h2>{t('report.recentRuns')}</h2>
            {report.recentRuns.length === 0 ? (
              <p className="muted">{t('report.noRuns')}</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('report.date')}</th>
                    <th>{t('report.tests')}</th>
                    <th>{t('report.passed')}</th>
                    <th>{t('report.failed')}</th>
                    <th>{t('report.totalDuration')}</th>
                  </tr>
                </thead>
                <tbody>
                  {report.recentRuns.map((run) => (
                    <tr key={run.startedAt}>
                      <td className="small">{formatDateTime(run.startedAt, language)}</td>
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
            <h2>{t('report.perTest')}</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>{t('nav.test')}</th>
                  <th>{t('common.status')}</th>
                  <th>{t('common.http')}</th>
                  <th>{t('common.ms')}</th>
                  <th>{t('report.lastRun')}</th>
                  <th>{t('common.error')}</th>
                </tr>
              </thead>
              <tbody>
                {report.tests.map((test) => (
                  <tr key={test.testId}>
                    <td>
                      <Link to={`/projects/${id}/tests/${test.testId}`}>
                        {test.testName}
                      </Link>
                    </td>
                    <td>{statusLabel(test, t)}</td>
                    <td>{test.statusCode ?? '—'}</td>
                    <td>{formatMs(test.responseTimeMs)}</td>
                    <td className="small muted">
                      {formatDateTime(test.lastRunAt, language)}
                    </td>
                    <td className="small muted">{test.errorMessage ?? '—'}</td>
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
