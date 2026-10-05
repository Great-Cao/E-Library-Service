import { getCurrentUserId } from '../stores/currentUser'

const BASE_PATH = '/api/v1'
const TIMEOUT_MS = 10000

/** An error carrying the backend's error code, so views can react to it. */
export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

async function request(path, options = {}) {
  const headers = {
    Accept: 'application/json',
    ...(options.headers ?? {}),
    // Identity simulation: every call is made on behalf of the selected user.
    'X-User-Id': String(getCurrentUserId()),
  }

  let response
  let raw
  try {
    response = await fetch(BASE_PATH + path, {
      ...options,
      headers,
      signal: AbortSignal.timeout(TIMEOUT_MS),
    })
    // Read the body inside the try so a stalled or broken response also becomes
    // a clean ApiError instead of a raw fetch/TypeError leaking to the view.
    raw = await response.text()
  } catch {
    throw new ApiError(
      'NETWORK_ERROR',
      '请求超时或无法连接后端，请确认后端已在 http://localhost:8080 启动。',
    )
  }

  let body = null
  if (raw) {
    try {
      body = JSON.parse(raw)
    } catch {
      body = null
    }
  }

  if (!response.ok) {
    // Every backend error uses the same envelope: { error: { code, message } }.
    const error = body?.error
    throw new ApiError(
      error?.code ?? 'UNKNOWN_ERROR',
      error?.message ?? `请求失败（HTTP ${response.status}）`,
    )
  }

  return body
}

export const api = {
  listBooks: (page, size) => request(`/books?page=${page}&size=${size}`),
  getBook: (bookId) => request(`/books/${bookId}`),
  borrowBook: (bookId) => request(`/books/${bookId}/loans`, { method: 'POST' }),
  returnLoan: (loanId) => request(`/loans/${loanId}/return`, { method: 'POST' }),
  currentLoans: (page, size) => request(`/users/me/loans?status=active&page=${page}&size=${size}`),
}
