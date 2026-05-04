# Frontend (React)

Vite + React + TypeScript + React Router. Consumes the Spring Boot API.

## Setup

```bash
npm install
```

Optional: copy `.env.example` to `.env.development` and adjust `VITE_API_BASE` if the API is not on `http://localhost:8080`.

## Scripts

- `npm run dev` — dev server (needs backend running for data)
- `npm run build` — production bundle to `dist/`
- `npm run preview` — serve `dist/` locally

## Routes

- `/projects` — list/create/delete projects
- `/projects/:projectId` — tests, run all, links
- `/projects/:projectId/report` — project report
- `/projects/:projectId/tests/new` — create test
- `/projects/:projectId/tests/:testId` — test detail, run, history
- `/projects/:projectId/tests/:testId/edit` — edit test
