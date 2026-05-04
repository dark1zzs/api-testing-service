const baseUrl = () =>
  (import.meta.env.VITE_API_BASE as string | undefined)?.replace(/\/$/, '') ??
  'http://localhost:8080'

export class ApiError extends Error {
  status: number
  body: string

  constructor(message: string, status: number, body: string) {
    super(message)
    this.status = status
    this.body = body
  }
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit & { parseJson?: boolean }
): Promise<T> {
  const url = `${baseUrl()}${path.startsWith('/') ? path : `/${path}`}`
  const headers = new Headers(init?.headers)
  if (init?.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const res = await fetch(url, { ...init, headers })

  if (res.status === 204) {
    return undefined as T
  }

  const text = await res.text()

  if (!res.ok) {
    let message = text || res.statusText
    try {
      const err = JSON.parse(text) as { message?: string }
      if (err?.message) message = err.message
    } catch {
      /* keep raw text */
    }
    throw new ApiError(message, res.status, text)
  }

  if (!text) {
    return undefined as T
  }

  return JSON.parse(text) as T
}
