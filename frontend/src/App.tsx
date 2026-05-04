import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProjectsPage } from './pages/ProjectsPage'
import { ProjectPage } from './pages/ProjectPage'
import { ReportPage } from './pages/ReportPage'
import { TestFormPage } from './pages/TestFormPage'
import { TestDetailPage } from './pages/TestDetailPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/projects" replace />} />
          <Route path="projects" element={<ProjectsPage />} />
          <Route path="projects/:projectId" element={<ProjectPage />} />
          <Route path="projects/:projectId/report" element={<ReportPage />} />
          <Route path="projects/:projectId/tests/new" element={<TestFormPage />} />
          <Route path="projects/:projectId/tests/:testId/edit" element={<TestFormPage />} />
          <Route path="projects/:projectId/tests/:testId" element={<TestDetailPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
