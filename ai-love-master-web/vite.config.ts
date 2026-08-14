import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入：自动 import 模板中用到的组件（JS tree-shaking，减小首屏体积）
    // importStyle: false 表示不自动引入组件级样式，样式仍由 main.ts 的全量 CSS 提供，
    // 避免 ElMessage/ElMessageBox 等函数式组件的样式缺失问题。
    Components({
      resolvers: [ElementPlusResolver({ importStyle: false })],
    }),
  ],
  server: {
    port: 5175,
    // 允许外部访问（cpolar 隧道转发需要）：监听所有网卡 + 放行 cpolar 域名 Host
    // 否则 Vite 默认拒绝非 localhost 的 Host 请求，经隧道访问页面会返回 403
    host: true,
    allowedHosts: ['.cpolar.top', '.cpolar.cn', '.cpolar.com', '.vip.cpolar.cn'],
    // 本地开发代理：/api 转发到后端（生产由 Netlify _redirects 代理）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
