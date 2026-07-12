let csrf = null;
let unauthorizedHandler = null;

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export class ApiRequestError extends Error {
  constructor(body, status) {
    super(body?.message || `Ошибка запроса (${status})`);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = body?.code;
    this.details = body?.details ?? [];
    this.body = body;
  }
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

export function clearCsrf() {
  csrf = null;
}

export async function refreshCsrf() {
  const response = await fetch('/api/auth/csrf', {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new ApiRequestError(await parseBody(response), response.status);
  csrf = await response.json();
  return csrf;
}

async function ensureCsrf() {
  return csrf ?? refreshCsrf();
}

async function parseBody(response) {
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) return response.json();
  const text = await response.text();
  return text ? { message: text } : null;
}

/** @param {string} path @param {RequestInit & {body?:unknown, skipUnauthorizedHandler?:boolean, retryCsrf?:boolean}} options */
export async function apiFetch(path, options = {}) {
  const method = (options.method ?? 'GET').toUpperCase();
  const headers = new Headers(options.headers ?? {});
  headers.set('Accept', 'application/json');

  if (!SAFE_METHODS.has(method)) {
    const token = await ensureCsrf();
    headers.set(token.headerName ?? 'X-CSRF-TOKEN', token.token);
  }

  let body = options.body;
  if (body != null && !(body instanceof FormData) && typeof body !== 'string') {
    headers.set('Content-Type', 'application/json');
    body = JSON.stringify(body);
  }

  const response = await fetch(path, {
    ...options,
    method,
    headers,
    body,
    credentials: 'include',
  });

  const payload = await parseBody(response);
  if (response.ok) return payload;

  if (response.status === 403 && !options.retryCsrf && ['CSRF_INVALID', 'CSRF_MISSING'].includes(payload?.code)) {
    clearCsrf();
    await refreshCsrf();
    return apiFetch(path, { ...options, retryCsrf: true });
  }

  if (!options.skipUnauthorizedHandler && (
    response.status === 401 || (response.status === 403 && payload?.code === 'ACCESS_DENIED')
  )) {
    await unauthorizedHandler?.({ status: response.status, code: payload?.code });
  }
  throw new ApiRequestError(payload, response.status);
}

export function getErrorMessage(error) {
  if (error instanceof ApiRequestError) {
    return [error.message, ...error.details].filter(Boolean).join(' · ');
  }
  return error?.message || 'Не удалось выполнить запрос';
}
