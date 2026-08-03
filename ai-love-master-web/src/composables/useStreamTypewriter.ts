const SPEED_MS = 18;

/**
 * 将 SSE 流式内容以打字机效果展示到 store
 * @param onAppend 接收新 chunk 时调用，返回当前已接收的完整内容
 * @param onDisplay 将需要显示的内容写入 UI（通常是 store.replaceLastAssistantMessage）
 * @returns feed(chunk) 供流式回调调用，stop() 供结束或取消时调用
 */
export function useStreamTypewriter(
  onDisplay: (content: string) => void,
) {
  let receivedContent = '';
  let displayedContent = '';
  let timer: ReturnType<typeof setInterval> | null = null;

  function tick() {
    if (displayedContent.length < receivedContent.length) {
      displayedContent += receivedContent[displayedContent.length];
      onDisplay(displayedContent);
    } else {
      stop();
    }
  }

  function start() {
    if (timer) return;
    timer = setInterval(tick, SPEED_MS);
  }

  /** 逐块追加 content，实现打字机效果；每次调用累加，不使用替换 */
  function feed(chunk: string | number) {
    const s = chunk != null && chunk !== '' ? String(chunk) : '';
    if (!s) return;
    receivedContent += s; // 累加而非替换
    if (displayedContent.length < receivedContent.length && !timer) {
      tick();
    }
    start();
  }

  function stop() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
    if (displayedContent.length < receivedContent.length) {
      displayedContent = receivedContent;
      onDisplay(displayedContent);
    }
  }

  function reset() {
    stop();
    receivedContent = '';
    displayedContent = '';
  }

  return { feed, stop, reset };
}
