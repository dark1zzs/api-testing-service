import { Link, Outlet } from 'react-router-dom'

export function Layout() {
  return (
    <div className="layout">
      <header className="header">
        <Link to="/projects" className="logo">
          API Testing
        </Link>
        <span className="tag">MVP</span>
      </header>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
