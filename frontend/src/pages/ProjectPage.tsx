import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as projectsApi from '../api/projects'
import * as testsApi from '../api/tests'
import { ApiError } from '../api/http'
import type {
  ApiTestResponse,
  ProjectRequest,
  ProjectResponse,
  TestExecutionResponse,
} from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'
import { useI18n } from '../i18n'

type StoryGroup = {
  story: string
  tests: ApiTestResponse[]
}

type FeatureGroup = {
  feature: string
  stories: StoryGroup[]
}

function groupTestsByFeatureAndStory(
  tests: ApiTestResponse[],
  withoutFeature: string,
  withoutStory: string,
): FeatureGroup[] {
  const features = new Map<string, Map<string, ApiTestResponse[]>>()

  tests.forEach((test) => {
    const feature = test.feature?.trim() || withoutFeature
    const story = test.story?.trim() || withoutStory
    if (!features.has(feature)) {
      features.set(feature, new Map())
    }
    const stories = features.get(feature)!
    stories.set(story, [...(stories.get(story) ?? []), test])
  })

  return [...features.entries()].map(([feature, stories]) => ({
    feature,
    stories: [...stories.entries()].map(([story, groupedTests]) => ({
      story,
      tests: groupedTests,
    })),
  }))
}

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
  const [editProjectOpen, setEditProjectOpen] = useState(false)
  const [projectSaving, setProjectSaving] = useState(false)
  const [projectForm, setProjectForm] = useState<ProjectRequest>({
    name: '',
    baseUrl: '',
    description: '',
  })
  const [runAllResult, setRunAllResult] = useState<TestExecutionResponse[] | null>(null)
  const testGroups = useMemo(
    () =>
      groupTestsByFeatureAndStory(
        tests,
        t('tests.withoutFeature'),
        t('tests.withoutStory'),
      ),
    [tests, t],
  )

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
      setProjectForm({
        name: p.name,
        baseUrl: p.baseUrl,
        description: p.description ?? '',
      })
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

  async function saveProject(e: React.FormEvent) {
    e.preventDefault()
    setProjectSaving(true)
    setError(null)
    try {
      const updated = await projectsApi.updateProject(id, {
        name: projectForm.name.trim(),
        baseUrl: projectForm.baseUrl.trim(),
        description: projectForm.description?.trim() || undefined,
      })
      setProject(updated)
      setProjectForm({
        name: updated.name,
        baseUrl: updated.baseUrl,
        description: updated.description ?? '',
      })
      setEditProjectOpen(false)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : t('errors.updateProject'))
    } finally {
      setProjectSaving(false)
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
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setEditProjectOpen((open) => !open)}
              >
                {t('actions.edit')}
              </button>
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

          {editProjectOpen && (
            <section className="card">
              <h2>{t('projects.edit')}</h2>
              <form className="form-grid" onSubmit={saveProject}>
                <label>
                  {t('form.name')}
                  <input
                    required
                    value={projectForm.name}
                    onChange={(e) =>
                      setProjectForm((form) => ({ ...form, name: e.target.value }))
                    }
                  />
                </label>
                <label>
                  {t('projects.baseUrl')}
                  <input
                    required
                    value={projectForm.baseUrl}
                    onChange={(e) =>
                      setProjectForm((form) => ({ ...form, baseUrl: e.target.value }))
                    }
                  />
                </label>
                <label className="span-2">
                  {t('form.description')}
                  <input
                    value={projectForm.description ?? ''}
                    onChange={(e) =>
                      setProjectForm((form) => ({
                        ...form,
                        description: e.target.value,
                      }))
                    }
                  />
                </label>
                <div className="span-2 form-actions">
                  <button type="submit" className="btn btn-primary" disabled={projectSaving}>
                    {projectSaving ? t('actions.running') : t('actions.save')}
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => {
                      setProjectForm({
                        name: project.name,
                        baseUrl: project.baseUrl,
                        description: project.description ?? '',
                      })
                      setEditProjectOpen(false)
                    }}
                  >
                    {t('actions.cancel')}
                  </button>
                </div>
              </form>
            </section>
          )}

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
              <div className="test-groups">
                {testGroups.map((featureGroup) => (
                  <details className="test-folder" key={featureGroup.feature} open>
                    <summary>
                      <span>{t('tests.feature')}</span>
                      <strong>{featureGroup.feature}</strong>
                    </summary>
                    <div className="story-groups">
                      {featureGroup.stories.map((storyGroup) => (
                        <details
                          className="story-folder"
                          key={`${featureGroup.feature}-${storyGroup.story}`}
                          open
                        >
                          <summary>
                            <span>{t('tests.story')}</span>
                            <strong>{storyGroup.story}</strong>
                            <em>{storyGroup.tests.length}</em>
                          </summary>
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
                              {storyGroup.tests.map((test) => (
                                <tr key={test.id}>
                                  <td>{test.runOrder ?? 0}</td>
                                  <td>
                                    <Link to={`/projects/${id}/tests/${test.id}`}>
                                      {test.name}
                                    </Link>
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
                                      onClick={() =>
                                        navigate(`/projects/${id}/tests/${test.id}/edit`)
                                      }
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
                        </details>
                      ))}
                    </div>
                  </details>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}
