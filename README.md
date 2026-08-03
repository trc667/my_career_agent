# AI 计算机学生职规大师智能体

基于 **Spring Boot**、**Spring AI** 与阿里云 **DashScope（通义千问）** 的计算机学生职业规划与学习顾问智能体，提供单轮对话与流式回复接口。

## 功能

- **人设**：务实、专业的计算机学生职规与学习顾问，熟悉校招、实习、面试、技术路线、时间管理；可推荐附近图书馆/自习室（高德地图）、查资料、记学习笔记。
- **控制台交互**：在运行应用的终端里直接输入问题，AI 在同一终端回复（推荐）。
- **同步对话**：`POST /api/chat`，一次返回完整回复。
- **流式对话**：`POST /api/chat/stream`，以 SSE 流式返回，适合前端逐字展示。
- **健康检查**：`GET /api/health`。

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+（登录/注册用，纯对话可暂时不建库）

## 新电脑拉取后快速运行

仓库**不提交** `application-dev.yml`（避免 API Key 进 Git）。模板在 **`application-dev.yml.example`**，与本地 dev 配置项一致（含 MCP、`LANG`/`LC_ALL`、首次建表的 `spring.sql.init`）。

在新电脑上执行：

```bash
# 1. 克隆
git clone https://github.com/trc667/my_career_agent.git
cd my_career_agent

# 2. 创建 MySQL 数据库（若本机有 MySQL）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ai_love_master CHARACTER SET utf8mb4;"

# 3. 生成本地 dev 配置（从示例复制，只需做一次）
# Windows（推荐）：
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev-yml.ps1
# Linux / macOS：
# bash scripts/setup-dev-yml.sh
# 或手动：cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml

# 4. 编辑 application-dev.yml：将 REPLACE_WITH_YOUR_DASHSCOPE_KEY / REPLACE_WITH_YOUR_AMAP_WEB_KEY 换成真实 Key
#    也可不改文件，改为设置环境变量 DASHSCOPE_API_KEY、AMAP_MCP_KEY（文件中已用 ${...} 引用）
# Windows: set DASHSCOPE_API_KEY=sk-你的密钥
# Linux/Mac: export DASHSCOPE_API_KEY=sk-你的密钥

# 5. 修改数据库连接（若 MySQL 不是 root/123456/3306）
# 编辑 application.yml 的 spring.datasource，或设 SPRING_DATASOURCE_* 环境变量

# 6. 启动（默认 profile 已是 dev，也可显式带上）
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

首次启动（dev）会执行 `schema.sql` 建表。**MySQL 为必选**（登录/注册依赖）。

**MCP（高德、联网搜索等）**：需安装 **Node.js**（`npx` 可用）。示例里 `command: npx` 在部分 Windows 上若失败，把对应项改成本机路径，例如 `D:\nodejs\npx.cmd`。不需要 MCP 时可在生成的 `application-dev.yml` 里将 `spring.ai.mcp.client.enabled` 设为 `false`。

## 配置

1. 获取 [阿里云灵积（DashScope）API Key](https://dashscope.console.aliyun.com/apiKey)。
2. 配置 API Key（任选其一）：
   - **推荐**：运行 `scripts/setup-dev-yml.ps1`（或 `setup-dev-yml.sh`）生成 `application-dev.yml`，再改占位符或设环境变量。
   - 环境变量：`DASHSCOPE_API_KEY=sk-xxx`（可与上面的 dev 文件一起用，文件内已 `${DASHSCOPE_API_KEY:...}`）。
   - 高德 MCP：`AMAP_MCP_KEY` 或编辑 `application-dev.yml` 里 `amap.mcp.key`。
   - 勿把含真实密钥的 `application-dev.yml` 提交到仓库。

3. 可选：在 `application.yml` 中调整模型与参数：
   - `spring.ai.dashscope.chat.options.model`：`qwen-turbo`（快）/ `qwen-plus` / `qwen-max`
   - `temperature`、`max-tokens` 等。

## 运行

**控制台交互模式**（在终端里输入问题，AI 在终端回复，推荐）：

**用脚本启动（推荐，中文不乱码）：**
- **双击 run-console.bat**（不依赖 PowerShell 策略），或
- 在项目根目录执行：
```powershell
# 若提示“禁止运行脚本”，用下面这句（临时绕过策略）：
powershell -ExecutionPolicy Bypass -File .\run-console.ps1
# 或直接运行 bat：
.\run-console.bat
```

启动后会看到 `========== AI 计算机学生职规大师 (Console) ==========`，在 `You>` 后输入问题回车即可；输入 `exit` 或 `quit` 退出（Web 服务仍会继续运行）。AI 的回复是中文，用脚本启动可正常显示。

**仅 Web 接口**（不启用控制台）：

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

**PowerShell 注意**：`-D` 参数必须用双引号包住整段，否则会报 “Unknown lifecycle phase” 错误。  
若控制台中文乱码，请先执行 `chcp 65001`。  
服务默认端口 **8080**。

**常见问题：**
- **BUILD FAILURE / Port 8080 was already in use**：说明上次进程还在跑，先关掉占用 8080 的窗口或进程，或改 `application.yml` 里 `server.port` 再启动。
- **在 Cursor/VS Code 里点运行报 “SpringApplication cannot be resolved”**：不要直接点 main 方法旁的运行，改用 **运行 → 启动调试 (F5)**，在列表里选 **「AI 计算机学生职规大师 (Spring Boot)」**；或直接在终端用上面的 `mvn spring-boot:run` 命令。

## 接口示例

### 同步对话

**Linux / Mac / CMD（真实 curl）：**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"校招前应该如何准备算法和八股？\"}"
```

**PowerShell**（系统里 `curl` 实为 `Invoke-WebRequest`，语法不同；中文必须整段一起执行，否则会乱码）：
```powershell
# 整段复制执行：先设 UTF-8，再请求并打印回复
chcp 65001 | Out-Null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$r = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" -Method POST -ContentType "application/json; charset=utf-8" -Body '{"message": "附近有什么图书馆可以自习？"}'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$r.reply
```

**若 PowerShell 里中文仍乱码**：用项目里的脚本，会把 AI 回复写入 UTF-8 文件并自动打开：
```powershell
cd c:\Users\admin\Desktop\my_ai
.\scripts\test-chat.ps1
# 或带问题：.\scripts\test-chat.ps1 "秋招简历应该怎么写？"
```
回复会保存在 `reply.txt` 并用默认程序打开，可正常看中文。

响应示例：

```json
{
  "reply": "校招算法建议先刷 LeetCode 100～200 题...",
  "usageTokens": 256
}
```

### 流式对话（SSE）

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"附近有什么图书馆？\"}" \
  -N
```

前端可使用 `EventSource` 或 `fetch` 消费 SSE 流。

## 项目结构

```
src/main/java/com/example/aimaster/
├── AiLoveMasterApplication.java    # 启动类
├── config/CareerMasterPrompt.java  # 职规大师系统提示词
├── controller/CareerMasterController.java
├── dto/ChatRequest.java, ChatResponse.java
└── service/CareerMasterService.java
```

## 技术栈

- Spring Boot 3.4
- Spring AI Alibaba DashScope Starter（通义千问）
- Lombok、Jakarta Validation

## 许可证

MIT
