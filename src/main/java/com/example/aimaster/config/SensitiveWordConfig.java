package com.example.aimaster.config;

import com.example.aimaster.filter.SensitiveWordFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 敏感词过滤配置：根据 app.sensitive-word 创建 Filter Bean。
 * enabled=false 时返回 NoOp 实现（直接返回原文本）。
 */
@Configuration
public class SensitiveWordConfig {

    @Value("${app.sensitive-word.enabled:true}")
    private boolean enabled;

    @Value("${app.sensitive-word.words-file:classpath:sensitive-words.txt}")
    private String wordsFile;

    @Value("${app.sensitive-word.replacement:***}")
    private String replacement;

    @Bean
    public SensitiveWordFilter sensitiveWordFilter() {
        SensitiveWordFilter filter = new SensitiveWordFilter();
        filter.setReplacement(replacement);

        if (!enabled) {
            return filter;  // 不加载词库，filter() 会因 root 为空而直接返回原文本
        }

        if (wordsFile.startsWith("classpath:")) {
            String path = wordsFile.substring("classpath:".length());
            if (!path.startsWith("/")) path = "/" + path;
            filter.loadFromClasspath(path);
        } else {
            Path p = Paths.get(wordsFile);
            filter.loadFromFile(p);
        }

        return filter;
    }
}
