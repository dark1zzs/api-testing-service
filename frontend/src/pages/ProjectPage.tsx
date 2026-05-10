import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as projectsApi from '../api/projects'
import * as testsApi from '../api/tests'
import { ApiError } from '../api/http'
import type { ApiTestResponse, ProjectResponse, TestExecutionResponse } from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'
import { useI18n } from '../i18n'

export function ProjectPage() {
  const { t } = useI18n()
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
      setError(e instanceof ApiError ? e.message : t('errors.loadProject'))
    } finally {
      setLoading(false)
    }
  }, [id, t])

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
      setError(e instanceof ApiError ? e.message : t('errors.runAll'))
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
      setError(e instanceof ApiError ? e.message : t('errors.runTest'))
    }
  }

  async function removeTest(testId: number, name: string) {
    if (!window.confirm(`${t('actions.delete')} "${name}"?`)) return
    setError(null)
    try {
      await testsApi.deleteTest(id, testId)
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : t('errors.deleteTest'))
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
        <span>{project?.name ?? '…'}</span>
      </nav>

      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      {loading || !project ? (
        <p className="muted">{t('common.loading')}</p>
      ) : (
        <>
          <header className="page-header">
            <div>
              <h1>{project.name}</h1>
              {project.description ? <p>{project.description}</p> : null}
            </div>
            <div className="page-actions">
              <Link to={`/projects/${id}/report`} className="btn btn-secondary">
                {t('nav.report')}
              </Link>
              <button
                type="button"
                className="btn btn-primary"
                disabled={runBusy}
                onClick={() => void runAll()}
              >
                {runBusy ? t('actions.running') : t('actions.runAll')}
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => navigate(`/projects/${id}/tests/new`)}
              >
                {t('actions.newTest')}
              </button>
            </div>
          </header>

          {runAllResult && (
            <section className="card">
              <h2>{t('test.lastRunResult')}</h2>
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('nav.test')}</th>
                    <th>{t('common.status')}</th>
                    <th>{t('common.http')}</th>
                    <th>{t('common.ms')}</th>
                    <th>{t('common.error')}</th>
                  </tr>
                </thead>
                <tbody>
                  {runAllResult.map((r) => (
                    <tr key={r.testId}>
                      <td>{r.testName}</td>
                      <td>{r.success ? t('report.passed') : t('report.failed')}</td>
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
            <h2>{t('tests.title')}</h2>
            {tests.length === 0 ? (
              <p className="muted">{t('tests.empty')}</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('tests.order')}</th>
                    <th>{t('form.name')}</th>
                    <th>{t('form.method').replace(' *', '')}</th>
                    <th>Endpoint</th>
                    <th>{t('tests.expected')}</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {tests.map((test) => (
                    <tr key={test.id}>
                      <td>{test.runOrder ?? 0}</td>
                      <td>
                        <Link to={`/projects/${id}/tests/${test.id}`}>{test.name}</Link>
                      </td>
                      <td>
                        <span className="pill">{test.method}</span>
                      </td>
                      <td className="mono small">{test.endpoint}</td>
                      <td>{test.expectedStatus}</td>
                      <td className="actions">
                        <button
                          type="button"
                          className="btn btn-ghost"
                          onClick={() => void runOne(test.id)}
                        >
                          {t('actions.run')}
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => navigate(`/projects/${id}/tests/${test.id}/edit`)}
                        >
                          {t('actions.edit')}
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger"
                          onClick={() => void removeTest(test.id, test.name)}
                        >
                          {t('actions.delete')}
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
