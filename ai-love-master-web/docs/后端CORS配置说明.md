# 后端 CORS 配置说明

前端会从不同地址访问后端 API，需在后端配置 `CORS_ALLOWED_ORIGINS` 允许这些来源。

---

## 一、配置方式

在你的后端项目（如 `application-prod.yml` 或 `.env`）中设置：

```yaml
# application-prod.yml 示例
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:}
```

或用环境变量：

```
CORS_ALLOWED_ORIGINS=允许的来源，多个用逗号分隔
```

---

## 二、根据场景填写

### 1. 测试 / 本地开发（尚未部署前端）

```env
CORS_ALLOWED_ORIGINS=*
```

允许所有来源，便于调试。**正式上线前建议改为具体域名**。

---

### 2. 本地开发（前端跑在 localhost）

本项目前端默认端口为 **5175**：

```env
CORS_ALLOWED_ORIGINS=http://localhost:5175,http://127.0.0.1:5175
```

若还用到其他端口，可追加：

```env
CORS_ALLOWED_ORIGINS=http://localhost:5175,http://127.0.0.1:5175,http://localhost:5173,http://localhost:3000
```

---

### 3. 腾讯云静态托管部署后

前端部署到腾讯云后，域名为：

```
https://你的环境ID.tcloudbaseapp.com
```

**写法一：仅生产前端**

```env
CORS_ALLOWED_ORIGINS=https://你的环境ID.tcloudbaseapp.com
```

**写法二：本地 + 生产同时支持**

```env
CORS_ALLOWED_ORIGINS=http://localhost:5175,http://127.0.0.1:5175,https://你的环境ID.tcloudbaseapp.com
```

> 把 `你的环境ID` 换成云开发控制台中的实际环境 ID。

---

### 4. 已备案的自定义域名

若为静态托管绑定了自定义域名（如 `https://ai.yourdomain.com`）：

```env
CORS_ALLOWED_ORIGINS=https://ai.yourdomain.com
```

---

## 三、未配置时的表现

- 不配置或为空：容易出现 CORS 错误，前端请求会被浏览器拦截
- 控制台可能报错：`Access to fetch at 'xxx' from origin 'yyy' has been blocked by CORS policy`

---

## 四、示例汇总

| 阶段 | CORS_ALLOWED_ORIGINS 示例 |
|------|---------------------------|
| 本地开发调试 | `http://localhost:5175,http://127.0.0.1:5175` |
| 快速测试 | `*` |
| 腾讯云部署后 | `https://xxxxx.tcloudbaseapp.com` |
| 本地 + 腾讯云 | `http://localhost:5175,https://xxxxx.tcloudbaseapp.com` |
