/**
 * SSE 流式接口（使用 fetch + ReadableStream，因 EventSource 不支持 POST）
 */
// 本地 dev 由 .env.development 指定；生产部署留空走同域（Netlify _redirects 代理 /api）
// 重要：生产环境流式接口优先使用 VITE_STREAM_BASE_URL 直连后端，
// 因为 Netlify 的 _redirects 代理会缓冲 SSE 响应，导致内容晚到甚至被截断丢失。
const API_BASE =
  import.meta.env.VITE_STREAM_BASE_URL ||
  import.meta.env.VITE_API_BASE_URL ||
  '';

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('love_master_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/** 页面 1：RAG 流式对话 */
export async function postChatStream(
  body: { message: string; conversationId: string },
  onChunk: (text: string) => void,
  onConversationId?: (id: string) => void,
  onError?: (err: string) => void,
): Promise<void> {
  const res = await fetch(`${API_BASE}/api/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const errText = await res.text();
    onError?.(errText || `HTTP ${res.status}`);
    throw new Error(errText || `HTTP ${res.status}`);
  }

  const reader = res.body?.getReader();
  if (!reader) {
    onError?.('无法读取响应流');
    throw new Error('无法读取响应流');
  }

  const decoder = new TextDecoder();
  let buf = '';
  let currentEvent = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buf += decoder.decode(value, { stream: true });
    const lines = buf.split('\n');
    buf = lines.pop() ?? '';

    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        const data = line.slice(5).trim();
        if (currentEvent === 'conversationId' && data && onConversationId) {
          onConversationId(data);
        } else if (data) {
          // data 可能是纯文本或 JSON，尝试解析
          const text = parseStreamData(data);
          if (text) onChunk(text);
        }
        currentEvent = '';
      } else if (line.trim()) {
        // 非 event/data 的文本行，当作内容直接传递
        onChunk(line);
      }
    }
  }

  const remainder = buf.trim();
  if (remainder) onChunk(parseStreamData(remainder) || remainder);
}

/** 解析 data 内容：纯文本直接返回，JSON 提取 content/delta/text */
function parseStreamData(data: string): string {
  const t = data.trim();
  if (!t) return '';
  if (t.startsWith('{')) {
    try {
      const j = JSON.parse(t) as Record<string, unknown>;
      return (j.content ?? j.delta ?? j.text ?? '') as string;
    } catch {
      return t;
    }
  }
  return t;
}

/** 页面 2：ReAct 流式 JSON 事件 */
export interface ReactStreamEvent {
  type: 'conv' | 'thought' | 'tool_call' | 'tool_result' | 'reply' | 'error';
  conversationId?: string;
  content?: string;
  data?: string;
  text?: string;
  delta?: string;
  toolName?: string;
  toolInput?: string;
}

export async function postChatReactStream(
  body: { message: string; conversationId: string },
  maxSteps: number,
  onEvent: (ev: ReactStreamEvent) => void,
  onError?: (err: string) => void,
): Promise<void> {
  const res = await fetch(
    `${API_BASE}/api/chat/react/stream?maxSteps=${maxSteps}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...getAuthHeader(),
      },
      body: JSON.stringify(body),
    },
  );

  if (!res.ok) {
    const errText = await res.text();
    onError?.(errText || `HTTP ${res.status}`);
    throw new Error(errText || `HTTP ${res.status}`);
  }

  const reader = res.body?.getReader();
  if (!reader) {
    onError?.('无法读取响应流');
    throw new Error('无法读取响应流');
  }

  const decoder = new TextDecoder();
  let buf = '';
  let currentEvent = '';

  function emitData(data: string) {
    if (!data.trim()) return;
    const s = data.trim();
    // 尝试解析为 JSON 事件
    const objects = parseJsonObjects(s);
    if (objects.length > 0) {
      for (const obj of objects) {
        try {
          const ev = JSON.parse(obj) as ReactStreamEvent;
          onEvent(ev);
        } catch {
          // 忽略
        }
      }
    } else {
      // 纯文本：作为 reply，或根据 event 类型处理
      if (currentEvent === 'conversationId' && s) {
        onEvent({ type: 'conv', conversationId: s });
      } else if (s && !s.startsWith('event:')) {
        onEvent({ type: 'reply', content: s });
      }
    }
    currentEvent = '';
  }

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buf += decoder.decode(value, { stream: true });
    const lines = buf.split('\n');
    buf = lines.pop() ?? '';

    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        emitData(line.slice(5));
      } else if (line.trim()) {
        emitData(line);
      }
    }
    // 无换行时解析 buf 中的完整 JSON（解析后移除已处理部分）
    const objects = parseJsonObjects(buf.trim());
    if (objects.length > 0) {
      for (const obj of objects) {
        try {
          const ev = JSON.parse(obj) as ReactStreamEvent;
          onEvent(ev);
        } catch {
          break;
        }
      }
      let consumed = 0;
      for (const obj of objects) {
        const idx = buf.indexOf(obj, consumed);
        if (idx >= 0) consumed = idx + obj.length;
      }
      buf = buf.slice(consumed);
    }
  }

  if (buf.trim()) {
    const parts = buf.split('\n');
    for (const line of parts) {
      if (line.startsWith('event:')) currentEvent = line.slice(6).trim();
      else if (line.startsWith('data:')) emitData(line.slice(5));
      else if (line.trim()) emitData(line);
    }
  }
}

/** 从字符串中解析多个连续的 JSON 对象（兼容无换行拼接格式） */
function parseJsonObjects(s: string): string[] {
  const results: string[] = [];
  let i = 0;
  while (i < s.length) {
    const start = s.indexOf('{', i);
    if (start < 0) break;
    let depth = 0;
    let j = start;
    while (j < s.length) {
      if (s[j] === '{') depth++;
      else if (s[j] === '}') {
        depth--;
        if (depth === 0) {
          results.push(s.slice(start, j + 1));
          i = j + 1;
          break;
        }
      }
      j++;
    }
    if (depth !== 0) break;
  }
  return results;
}
