/**
 * Markdown 渲染工具：把 AI 回复中的 Markdown 转为安全的 HTML，
 * 避免界面直接显示 **加粗**、*斜体* 等原始标记符号。
 */
import { marked } from 'marked';
import DOMPurify from 'dompurify';

marked.setOptions({
  gfm: true, // 支持表格、删除线等 GitHub 风格语法
  breaks: true, // 单个换行转为 <br>，保证 AI 回复的行距自然
});

/**
 * 将 Markdown 文本渲染为安全的 HTML 字符串（供 v-html 使用）。
 * 使用 DOMPurify 过滤危险标签/脚本，防止 XSS。
 */
export function renderMarkdown(text: string): string {
  if (!text) return '';
  const raw = marked.parse(text) as string;
  return DOMPurify.sanitize(raw);
}
