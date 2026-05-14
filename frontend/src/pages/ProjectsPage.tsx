import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as projectsApi from '../api/projects'
import { ApiError } from '../api/http'
import type {
  OpenApiGenerationRequest,
  ProjectRequest,
  ProjectResponse,
} from '../types/api'
import { ErrorBanner } from '../components/ErrorBanner'
import { useI18n } from '../i18n'

export function ProjectsPage() {
  const { t } = useI18n()
  const [items, setItems] = useState<ProjectResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [generatedMessage, setGeneratedMessage] = useState<string | null>(null)
  const [generating, setGenerating] = useState(false)
  const [form, setForm] = useState<ProjectRequest>({
    name: '',
    baseUrl: 'https://jsonplaceholder.typicode.com',
    description: '',
  })
  const [openApiForm, setOpenApiForm] = useState<OpenApiGenerationRequest>({
    projectName: '',
    baseUrl: '',
    openApiUrl: '',
    description: '',
  })

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await projectsApi.listProjects()
      setItems(data)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : t('errors.loadProjects'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    void load()
  }, [load])

  async function onCreate(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setGeneratedMessage(null)
    try {
      await projectsApi.createProject({
        name: form.name.trim(),
        baseUrl: form.baseUrl.trim(),
        description: form.description?.trim() || undefined,
      })
      setForm((f) => ({ ...f, name: '', description: '' }))
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t('errors.createProject'))
    }
  }

  async function onDelete(id: number, name: string) {
    if (!window.confirm(`${t('projects.confirmDelete')} "${name}"?`)) return
    setError(null)
    try {
      await projectsApi.deleteProject(id)
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t('errors.deleteProject'))
    }
  }

  async function onGenerateFromOpenApi(e: React.FormEvent) {
    e.preventDefault()
    setGenerating(true)
    setError(null)
    setGeneratedMessage(null)
    try {
      const result = await projectsApi.generateFromOpenApi({
        projectName: openApiForm.projectName.trim(),
        baseUrl: openApiForm.baseUrl.trim(),
        openApiUrl: openApiForm.openApiUrl.trim(),
        description: openApiForm.description?.trim() || undefined,
      })
      setGeneratedMessage(
        `${result.project.name}: ${t('projects.generated')} ${result.generatedTestsCount}`,
      )
      setOpenApiForm({
        projectName: '',
        baseUrl: '',
        openApiUrl: '',
        description: '',
      })
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t('errors.openApiGeneration'))
    } finally {
      setGenerating(false)
    }
  }

  return (
    <div className="page">
      <h1>{t('projects.title')}</h1>
      <ErrorBanner message={error} onDismiss={() => setError(null)} />
      {generatedMessage ? (
        <div className="success-banner" role="status">
          {generatedMessage}
        </div>
      ) : null}

      <section className="card">
        <h2>{t('projects.new')}</h2>
        <form className="form-grid" onSubmit={onCreate}>
          <label>
            {t('form.name')}
            <input
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
          </label>
          <label>
            {t('projects.baseUrl')}
            <input
              required
              value={form.baseUrl}
              onChange={(e) => setForm((f) => ({ ...f, baseUrl: e.target.value }))}
            />
          </label>
          <label className="span-2">
            {t('form.description')}
            <input
              value={form.description ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </label>
          <div className="span-2">
            <button type="submit" className="btn btn-primary">
              {t('actions.create')}
            </button>
          </div>
        </form>
      </section>

      <section className="card">
        <h2>{t('projects.generateFromOpenApi')}</h2>
        <form className="form-grid" onSubmit={onGenerateFromOpenApi}>
          <label>
            {t('form.name')}
            <input
              required
              value={openApiForm.projectName}
              onChange={(e) =>
                setOpenApiForm((f) => ({ ...f, projectName: e.target.value }))
              }
            />
          </label>
          <label>
            {t('projects.baseUrl')}
            <input
              required
              placeholder="https://petstore.swagger.io/v2"
              value={openApiForm.baseUrl}
              onChange={(e) =>
                setOpenApiForm((f) => ({ ...f, baseUrl: e.target.value }))
              }
            />
          </label>
          <label className="span-2">
            {t('projects.openApiUrl')}
            <input
              required
              placeholder="https://petstore.swagger.io/v2/swagger.json"
              value={openApiForm.openApiUrl}
              onChange={(e) =>
                setOpenApiForm((f) => ({ ...f, openApiUrl: e.target.value }))
              }
            />
          </label>
          <label className="span-2">
            {t('form.description')}
            <input
              value={openApiForm.description ?? ''}
              onChange={(e) =>
                setOpenApiForm((f) => ({ ...f, description: e.target.value }))
              }
            />
          </label>
          <div className="span-2">
            <button type="submit" className="btn btn-primary" disabled={generating}>
              {generating ? t('actions.running') : t('actions.create')}
            </button>
          </div>
        </form>
      </section>

      <section className="card">
        <h2>{t('projects.all')}</h2>
        {loading ? (
          <p className="muted">{t('common.loading')}</p>
        ) : items.length === 0 ? (
          <p className="muted">{t('projects.empty')}</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>{t('form.name')}</th>
                <th>{t('projects.baseUrl')}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((p) => (
                <tr key={p.id}>
                  <td>
                    <Link to={`/projects/${p.id}`}>{p.name}</Link>
                  </td>
                  <td className="mono muted">{p.baseUrl}</td>
                  <td className="actions">
                    <button
                      type="button"
                      className="btn btn-danger"
                      onClick={() => void onDelete(p.id, p.name)}
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
    </div>
  )
}
