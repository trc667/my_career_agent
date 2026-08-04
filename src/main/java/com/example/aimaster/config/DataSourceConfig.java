package com.example.aimaster.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 双数据源配置：
 * - 主数据源（@Primary）：MySQL，业务数据（用户/公告/反馈），MyBatis-Plus 使用
 * - 向量数据源：PostgreSQL + pgvector，RAG 知识库向量存取（PgVectorStore 专用）
 *
 * 连接参数：
 * - 主数据源：绑定 spring.datasource.*
 * - 向量数据源：绑定 app.vector-datasource.*（新增配置）
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource primaryDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("app.vector-datasource")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource vectorDataSource(
            @Qualifier("vectorDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }
}
