// 若后端出现 "No static resource api/auth/login" 错误，在 WebMvc 配置中按下面方式修改
// 文件位置示例：src/main/java/com/example/aimaster/config/WebMvcConfig.java

package com.example.aimaster.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ❌ 错误：不要用 "/**" 会拦截所有请求包括 /api/xxx
        // registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");

        // ✅ 正确：只映射前端静态资源，不包含 /api
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");
    }
}
