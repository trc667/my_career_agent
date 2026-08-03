# AI 职规大师 / 超级智能体 · 前端

Vue 3 + TypeScript + Vite + Element Plus。

## 新电脑克隆后运行

```bash
cd ai-love-master-web
npm install
```

创建本地环境变量（仓库不提交 `.env.*`，避免泄露接口地址）：

```bash
# 开发：对接本地后端（默认 8080）
copy .env.development.example .env.development
# macOS/Linux: cp .env.development.example .env.development

# 生产构建：填写实际 API 根地址（不要末尾 /api）
copy .env.production.example .env.production
# 编辑 .env.production 中的 VITE_API_BASE_URL
```

```bash
npm run dev
# 默认 http://localhost:5175
```

生产打包：

```bash
npm run build
# 产物在 dist/，含 public/_redirects 供 Netlify 等 SPA 回退
```

## 技术栈

- Vue 3、Vue Router（History）、Pinia、Axios
- Element Plus

