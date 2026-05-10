import { Link, Outlet } from 'react-router-dom'
import { useI18n } from '../i18n'

export function Layout() {
  const { language, setLanguage, t } = useI18n()

  return (
    <div className="layout">
      <header className="header">
        <Link to="/projects" className="logo">
          {t('app.logo')}
        </Link>
        <span className="tag">MVP</span>
        <div className="language-switcher" aria-label="Language switcher">
          <button
            type="button"
            className={language === 'ru' ? 'active' : ''}
            onClick={() => setLanguage('ru')}
          >
            RU
          </button>
          <button
            type="button"
            className={language === 'en' ? 'active' : ''}
            onClick={() => setLanguage('en')}
          >
            EN
          </button>
        </div>
      </header>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
