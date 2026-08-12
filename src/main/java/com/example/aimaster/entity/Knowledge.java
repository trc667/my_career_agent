package com.example.aimaster.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库条目：RAG 检索/八股练习/超级智能体的统一事实源。
 * 首次启动由 KnowledgeService 从 career-tips.txt 导入，之后以本表为唯一事实源，
 * 增删改后触发索引全量重建（pgvector + BM25 + 八股内存缓存）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge")
public class Knowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类（复用 autoTag：后端/前端/算法/面试/校招流程/实习/升学/软技能/综合） */
    @TableField("category")
    private String category;

    /** 知识段正文（一段一条，检索与八股共用） */
    @TableField("content")
    private String content;

    /** 八股随机题改写后的疑问句（落库持久化：改写一次永久生效，随机题零 LLM 调用） */
    @TableField("question")
    private String question;

    /** 1=启用参与检索，0=停用（停用段不进索引，前端八股列表同步隐藏） */
    @TableField("enabled")
    private Integer enabled;

    @TableField("created_at")
    private LocalDateTime createTime;

    @TableField("updated_at")
    private LocalDateTime updateTime;
}
