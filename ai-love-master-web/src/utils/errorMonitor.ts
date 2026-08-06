/**
 * 前端错误监控：全局捕获 error / unhandledrejection + 手动上报。
 * 上报走原生 fetch（不经过 axios 拦截器，避免与 http.ts 的错误上报互相循环）。
 */

const API_BASE =
  import.meta.env.VITE_STREAM_BASE_URL ||
  import.meta.env.VITE_API_BASE_URL ||
  '';

export interface ErrorReportPayload {
  source?: 'frontend' | 'backend';
  level?: 'ERROR' | 'WARN';
  message: string;
  stackTrace?: string;
  uri?: string;
  method?: string;
}

/** 简单去重：同一 message 短时间内只上报一次（防抖窗口 30s，上限 50 条） */
const recent = new Map<string, number>();
const DEDUP_WINDOW = 30_000;

function dedup(message: string): boolean {
  const now = Date.now();
  if (recent.has(message) && now - (recent.get(message) ?? 0) < DEDUP_WINDOW) {
    return false;
  }
  recent.set(message, now);
  if (recent.size > 50) {
    const oldest = recent.keys().next().value;
    if (oldest !== undefined) recent.delete(oldest);
  }
  return true;
}

/** 上报错误（静默失败，不影响业务） */
export function reportError(payload: ErrorReportPayload) {
  const msg = (payload.message || 'unknown error').slice(0, 2000);
  if (!dedup(msg)) return;
  try {
    fetch(`${API_BASE}/api/monitor/report`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        source: payload.source ?? 'frontend',
        level: payload.level ?? 'ERROR',
        message: msg,
        stackTrace: (payload.stackTrace ?? '').slice(0, 6000),
        uri: (payload.uri ?? window.location.pathname).slice(0, 512),
        method: (payload.method ?? '').slice(0, 16),
      }),
    }).catch(() => {
      /* 上报失败静默 */
    });
  } catch {
    /* 忽略 */
  }
}

/** 注册全局错误捕获（main.ts 调用一次） */
export function initErrorMonitor() {
  if (typeof window === 'undefined') return;

  window.addEventListener('error', (e: ErrorEvent) => {
    const msg = e.message || 'Uncaught Error';
    const stack = e.error?.stack || (e.filename ? `${e.filename}:${e.lineno}:${e.colno}` : '');
    reportError({ source: 'frontend', level: 'ERROR', message: msg, stackTrace: stack });
  });

  window.addEventListener('unhandledrejection', (e: PromiseRejectionEvent) => {
    const reason = e.reason;
    const msg =
      reason instanceof Error ? reason.message : typeof reason === 'string' ? reason : 'Unhandled Promise Rejection';
    const stack = reason instanceof Error ? (reason.stack ?? '') : '';
    reportError({ source: 'frontend', level: 'ERROR', message: msg, stackTrace: stack });
  });
}
