import { getCurrentUserId } from '../stores/currentUser'

const BASE_PATH = '/api/v1'

/** An error carrying the backend's error code, so views can react to it. */
export class ApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
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
  try {
    response = await fetch(BASE_PATH + path, { ...options, headers })
  } catch {
    throw new ApiError(
      'NETWORK_ERROR',
      '无法连接后端服务，请确认后端已在 http://localhost:8080 启动。',
      0,
    )
  }

  const raw = await response.text()
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
      response.status,
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
