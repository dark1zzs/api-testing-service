/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

export type Language = 'ru' | 'en'

type DictionaryKey =
  | 'actions.cancel'
  | 'actions.create'
  | 'actions.delete'
  | 'actions.dismiss'
  | 'actions.edit'
  | 'actions.newTest'
  | 'actions.run'
  | 'actions.runAll'
  | 'actions.runNow'
  | 'actions.running'
  | 'actions.save'
  | 'app.logo'
  | 'common.error'
  | 'common.http'
  | 'common.loading'
  | 'common.ms'
  | 'common.no'
  | 'common.status'
  | 'common.yes'
  | 'errors.createProject'
  | 'errors.deleteProject'
  | 'errors.deleteTest'
  | 'errors.invalidIds'
  | 'errors.invalidProjectId'
  | 'errors.loadProject'
  | 'errors.loadProjects'
  | 'errors.loadReport'
  | 'errors.loadTest'
  | 'errors.openApiGeneration'
  | 'errors.runAll'
  | 'errors.runTest'
  | 'errors.saveTest'
  | 'errors.updateProject'
  | 'form.captureJsonPath'
  | 'form.captureVariableName'
  | 'form.description'
  | 'form.endpoint'
  | 'form.expectedBody'
  | 'form.expectedHeaderName'
  | 'form.expectedHeaderValue'
  | 'form.expectedJsonPath'
  | 'form.expectedJsonValue'
  | 'form.expectedStatus'
  | 'form.maxResponseTime'
  | 'form.method'
  | 'form.name'
  | 'form.feature'
  | 'form.requestBody'
  | 'form.requestHeaders'
  | 'form.runOrder'
  | 'form.story'
  | 'form.testKey'
  | 'nav.project'
  | 'nav.projects'
  | 'nav.report'
  | 'nav.test'
  | 'projects.all'
  | 'projects.baseUrl'
  | 'projects.confirmDelete'
  | 'projects.edit'
  | 'projects.empty'
  | 'projects.generated'
  | 'projects.generateFromOpenApi'
  | 'projects.new'
  | 'projects.openApiUrl'
  | 'projects.title'
  | 'report.avgResponse'
  | 'report.currentStatusHint'
  | 'report.date'
  | 'report.executiveSummary'
  | 'report.executiveSummaryText'
  | 'report.failed'
  | 'report.lastRun'
  | 'report.lastRunDuration'
  | 'report.noData'
  | 'report.noRuns'
  | 'report.notRun'
  | 'report.passed'
  | 'report.perTest'
  | 'report.recentRuns'
  | 'report.resultSplit'
  | 'report.savedRuns'
  | 'report.successRate'
  | 'report.testDuration'
  | 'report.testDurationHint'
  | 'report.tests'
  | 'report.title'
  | 'report.total'
  | 'report.totalDuration'
  | 'report.trend'
  | 'report.trendHint'
  | 'test.definition'
  | 'test.expect'
  | 'test.history'
  | 'test.lastRunResult'
  | 'test.newTitle'
  | 'test.editTitle'
  | 'test.noRuns'
  | 'test.success'
  | 'tests.empty'
  | 'tests.expected'
  | 'tests.feature'
  | 'tests.order'
  | 'tests.story'
  | 'tests.title'
  | 'tests.withoutFeature'
  | 'tests.withoutStory'

type Dictionary = Record<DictionaryKey, string>

const dictionaries: Record<Language, Dictionary> = {
  ru: {
    'actions.cancel': 'Отмена',
    'actions.create': 'Создать',
    'actions.delete': 'Удалить',
    'actions.dismiss': 'Закрыть',
    'actions.edit': 'Изменить',
    'actions.newTest': 'Новый тест',
    'actions.run': 'Запустить',
    'actions.runAll': 'Запустить все',
    'actions.runNow': 'Запустить сейчас',
    'actions.running': 'Запуск...',
    'actions.save': 'Сохранить',
    'app.logo': 'API Testing',
    'common.error': 'Ошибка',
    'common.http': 'HTTP',
    'common.loading': 'Загрузка...',
    'common.ms': 'мс',
    'common.no': 'нет',
    'common.status': 'Статус',
    'common.yes': 'да',
    'errors.createProject': 'Не удалось создать проект',
    'errors.deleteProject': 'Не удалось удалить проект',
    'errors.deleteTest': 'Не удалось удалить тест',
    'errors.invalidIds': 'Некорректные id.',
    'errors.invalidProjectId': 'Некорректный id проекта.',
    'errors.loadProject': 'Не удалось загрузить проект',
    'errors.loadProjects': 'Не удалось загрузить проекты',
    'errors.loadReport': 'Не удалось загрузить отчет',
    'errors.loadTest': 'Не удалось загрузить тест',
    'errors.openApiGeneration': 'Не удалось сгенерировать тесты по OpenAPI',
    'errors.runAll': 'Не удалось запустить тесты',
    'errors.runTest': 'Не удалось запустить тест',
    'errors.saveTest': 'Не удалось сохранить тест',
    'errors.updateProject': 'Не удалось обновить проект',
    'form.captureJsonPath': 'Capture JSONPath (после успешного ответа)',
    'form.captureVariableName': 'Имя переменной',
    'form.description': 'Описание',
    'form.endpoint': 'Endpoint',
    'form.expectedBody': 'Ожидаемое тело ответа (подстрока)',
    'form.expectedHeaderName': 'Ожидаемый заголовок',
    'form.expectedHeaderValue': 'Ожидаемое значение заголовка',
    'form.expectedJsonPath': 'JSONPath',
    'form.expectedJsonValue': 'Ожидаемое JSON-значение',
    'form.expectedStatus': 'Ожидаемый HTTP-статус *',
    'form.maxResponseTime': 'Максимальное время ответа (мс)',
    'form.method': 'Метод *',
    'form.name': 'Название *',
    'form.feature': 'Feature',
    'form.requestBody': 'Тело запроса',
    'form.requestHeaders': 'Заголовки запроса (JSON)',
    'form.runOrder': 'Порядок запуска (меньшее значение запускается раньше)',
    'form.story': 'Story',
    'form.testKey': 'Ключ теста',
    'nav.project': 'Проект',
    'nav.projects': 'Проекты',
    'nav.report': 'Отчет',
    'nav.test': 'Тест',
    'projects.all': 'Все проекты',
    'projects.baseUrl': 'Base URL',
    'projects.confirmDelete': 'Удалить проект',
    'projects.edit': 'Редактирование проекта',
    'projects.empty': 'Проектов пока нет.',
    'projects.generated': 'Сгенерировано тестов',
    'projects.generateFromOpenApi': 'Создать проект из OpenAPI',
    'projects.new': 'Новый проект',
    'projects.openApiUrl': 'OpenAPI / Swagger URL',
    'projects.title': 'Проекты',
    'report.avgResponse': 'Среднее время',
    'report.currentStatusHint': 'Текущий статус по последнему запуску каждого теста',
    'report.date': 'Дата',
    'report.executiveSummary': 'Краткая сводка',
    'report.executiveSummaryText':
      'В проекте {{total}} тестов, текущая успешность {{rate}}%. Последний полный прогон занял {{duration}}, среднее время ответа {{avg}}.',
    'report.failed': 'Провалено',
    'report.lastRun': 'последний запуск',
    'report.lastRunDuration': 'Последний прогон',
    'report.noData': 'Данных о запусках пока нет.',
    'report.noRuns': 'Запусков пока нет.',
    'report.notRun': 'Не запускались',
    'report.passed': 'Успешно',
    'report.perTest': 'По тестам',
    'report.recentRuns': 'Последние прогоны',
    'report.resultSplit': 'Распределение результатов',
    'report.savedRuns': 'сохранено запусков',
    'report.successRate': 'Успешность',
    'report.testDuration': 'Длительность тестов',
    'report.testDurationHint': 'Последнее время ответа по каждому тесту',
    'report.tests': 'Тесты',
    'report.title': 'Отчет по проекту',
    'report.total': 'Всего',
    'report.totalDuration': 'Общая длительность',
    'report.trend': 'Динамика длительности прогонов',
    'report.trendHint': 'Суммарное сохраненное время ответа по датам',
    'test.definition': 'Описание теста',
    'test.expect': 'ожидаем',
    'test.history': 'История',
    'test.lastRunResult': 'Результат последнего запуска',
    'test.newTitle': 'Новый API-тест',
    'test.editTitle': 'Редактирование API-теста',
    'test.noRuns': 'Запусков пока нет.',
    'test.success': 'Успех',
    'tests.empty': 'Тестов пока нет.',
    'tests.expected': 'Ожидаемый статус',
    'tests.feature': 'Feature',
    'tests.order': 'Порядок',
    'tests.story': 'Story',
    'tests.title': 'Тесты',
    'tests.withoutFeature': 'Без feature',
    'tests.withoutStory': 'Без story',
  },
  en: {
    'actions.cancel': 'Cancel',
    'actions.create': 'Create',
    'actions.delete': 'Delete',
    'actions.dismiss': 'Dismiss',
    'actions.edit': 'Edit',
    'actions.newTest': 'New test',
    'actions.run': 'Run',
    'actions.runAll': 'Run all tests',
    'actions.runNow': 'Run now',
    'actions.running': 'Running...',
    'actions.save': 'Save',
    'app.logo': 'API Testing',
    'common.error': 'Error',
    'common.http': 'HTTP',
    'common.loading': 'Loading...',
    'common.ms': 'ms',
    'common.no': 'no',
    'common.status': 'Status',
    'common.yes': 'yes',
    'errors.createProject': 'Create failed',
    'errors.deleteProject': 'Delete failed',
    'errors.deleteTest': 'Delete failed',
    'errors.invalidIds': 'Invalid ids.',
    'errors.invalidProjectId': 'Invalid project id.',
    'errors.loadProject': 'Failed to load project',
    'errors.loadProjects': 'Failed to load projects',
    'errors.loadReport': 'Failed to load report',
    'errors.loadTest': 'Failed to load test',
    'errors.openApiGeneration': 'Failed to generate tests from OpenAPI',
    'errors.runAll': 'Run all failed',
    'errors.runTest': 'Run failed',
    'errors.saveTest': 'Save failed',
    'errors.updateProject': 'Update failed',
    'form.captureJsonPath': 'Capture JSONPath (after success)',
    'form.captureVariableName': 'Capture variable name',
    'form.description': 'Description',
    'form.endpoint': 'Endpoint path *',
    'form.expectedBody': 'Expected response body (substring)',
    'form.expectedHeaderName': 'Expected header name',
    'form.expectedHeaderValue': 'Expected header value (substring)',
    'form.expectedJsonPath': 'JSONPath',
    'form.expectedJsonValue': 'Expected JSON value',
    'form.expectedStatus': 'Expected HTTP status *',
    'form.maxResponseTime': 'Max response time (ms)',
    'form.method': 'Method *',
    'form.name': 'Name *',
    'form.feature': 'Feature',
    'form.requestBody': 'Request body',
    'form.requestHeaders': 'Request headers (JSON)',
    'form.runOrder': 'Run order (lower runs first)',
    'form.story': 'Story',
    'form.testKey': 'Test key',
    'nav.project': 'Project',
    'nav.projects': 'Projects',
    'nav.report': 'Report',
    'nav.test': 'Test',
    'projects.all': 'All projects',
    'projects.baseUrl': 'Base URL',
    'projects.confirmDelete': 'Delete project',
    'projects.edit': 'Edit project',
    'projects.empty': 'No projects yet.',
    'projects.generated': 'Generated tests',
    'projects.generateFromOpenApi': 'Create project from OpenAPI',
    'projects.new': 'New project',
    'projects.openApiUrl': 'OpenAPI / Swagger URL',
    'projects.title': 'Projects',
    'report.avgResponse': 'Avg response',
    'report.currentStatusHint': 'Current status by latest run per test',
    'report.date': 'Date',
    'report.executiveSummary': 'Executive summary',
    'report.executiveSummaryText':
      'Project has {{total}} tests, current success rate is {{rate}}%. The latest full run duration is {{duration}}, with average response time {{avg}}.',
    'report.failed': 'Failed',
    'report.lastRun': 'last run',
    'report.lastRunDuration': 'Last run duration',
    'report.noData': 'No execution data yet.',
    'report.noRuns': 'No runs yet.',
    'report.notRun': 'Not run',
    'report.passed': 'Passed',
    'report.perTest': 'Per test',
    'report.recentRuns': 'Recent runs',
    'report.resultSplit': 'Result split',
    'report.savedRuns': 'saved runs',
    'report.successRate': 'Success rate',
    'report.testDuration': 'Test duration',
    'report.testDurationHint': 'Latest response time for each test',
    'report.tests': 'Tests',
    'report.title': 'Project report',
    'report.total': 'Total',
    'report.totalDuration': 'Total duration',
    'report.trend': 'Run duration trend',
    'report.trendHint': 'Total saved response time by date',
    'test.definition': 'Definition',
    'test.expect': 'expect',
    'test.history': 'History',
    'test.lastRunResult': 'Last run result',
    'test.newTitle': 'New API test',
    'test.editTitle': 'Edit API test',
    'test.noRuns': 'No runs yet.',
    'test.success': 'Success',
    'tests.empty': 'No tests yet.',
    'tests.expected': 'Expected',
    'tests.feature': 'Feature',
    'tests.order': 'Order',
    'tests.story': 'Story',
    'tests.title': 'Tests',
    'tests.withoutFeature': 'No feature',
    'tests.withoutStory': 'No story',
  },
}

type I18nContextValue = {
  language: Language
  setLanguage: (language: Language) => void
  t: (key: DictionaryKey, params?: Record<string, string | number>) => string
}

const I18nContext = createContext<I18nContextValue | null>(null)

function readInitialLanguage(): Language {
  const saved = localStorage.getItem('api-testing-language')
  return saved === 'en' || saved === 'ru' ? saved : 'ru'
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<Language>(readInitialLanguage)

  useEffect(() => {
    localStorage.setItem('api-testing-language', language)
  }, [language])

  const value = useMemo<I18nContextValue>(
    () => ({
      language,
      setLanguage,
      t: (key, params) => {
        let text = dictionaries[language][key]
        if (params) {
          Object.entries(params).forEach(([name, value]) => {
            text = text.replaceAll(`{{${name}}}`, String(value))
          })
        }
        return text
      },
    }),
    [language],
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n() {
  const value = useContext(I18nContext)
  if (!value) {
    throw new Error('useI18n must be used inside I18nProvider')
  }
  return value
}
