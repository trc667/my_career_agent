-- 新电脑拉取代码后首次运行会自动执行（dev 环境）
-- 若已存在表则跳过，需手动创建数据库：CREATE DATABASE ai_love_master;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    email VARCHAR(128) DEFAULT NULL,
    role VARCHAR(16) DEFAULT 'USER',
    avatar VARCHAR(512) DEFAULT NULL,
    points INT DEFAULT 0 COMMENT '积分余额',
    level VARCHAR(16) DEFAULT 'FREE' COMMENT '会员等级: FREE/VIP',
    vip_expire_at DATETIME DEFAULT NULL COMMENT 'VIP 到期时间',
    inviter_id BIGINT DEFAULT NULL COMMENT '邀请人用户ID（分享裂变）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兼容已存在的 app_user 表：若缺少 email 列则补充（MySQL 5.7 动态 DDL）
SET @has_email := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'email');
SET @ddl := IF(@has_email = 0,
    'ALTER TABLE app_user ADD COLUMN email VARCHAR(128) DEFAULT NULL, ADD UNIQUE KEY uk_user_email (email)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 兼容已存在的 app_user 表：若缺少 role 列则补充
SET @has_role := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'role');
SET @ddl2 := IF(@has_role = 0,
    'ALTER TABLE app_user ADD COLUMN role VARCHAR(16) DEFAULT ''USER''',
    'SELECT 1');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 兼容已存在的 app_user 表：若缺少 avatar 列则补充（头像 URL）
SET @has_avatar := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'avatar');
SET @ddl3 := IF(@has_avatar = 0,
    'ALTER TABLE app_user ADD COLUMN avatar VARCHAR(512) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt3 FROM @ddl3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- 兼容已存在的 app_user 表：若缺少积分/等级/VIP 到期列则补充（积分会员体系）
SET @has_points := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'points');
SET @ddl4 := IF(@has_points = 0,
    'ALTER TABLE app_user ADD COLUMN points INT DEFAULT 0',
    'SELECT 1');
PREPARE stmt4 FROM @ddl4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

SET @has_level := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'level');
SET @ddl5 := IF(@has_level = 0,
    'ALTER TABLE app_user ADD COLUMN level VARCHAR(16) DEFAULT ''FREE''',
    'SELECT 1');
PREPARE stmt5 FROM @ddl5;
EXECUTE stmt5;
DEALLOCATE PREPARE stmt5;

SET @has_vip := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'vip_expire_at');
SET @ddl6 := IF(@has_vip = 0,
    'ALTER TABLE app_user ADD COLUMN vip_expire_at DATETIME DEFAULT NULL',
    'SELECT 1');
PREPARE stmt6 FROM @ddl6;
EXECUTE stmt6;
DEALLOCATE PREPARE stmt6;

-- 兼容已存在的 app_user 表：若缺少邀请人列则补充（分享裂变）
SET @has_inviter := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'inviter_id');
SET @ddl7 := IF(@has_inviter = 0,
    'ALTER TABLE app_user ADD COLUMN inviter_id BIGINT DEFAULT NULL',
    'SELECT 1');
PREPARE stmt7 FROM @ddl7;
EXECUTE stmt7;
DEALLOCATE PREPARE stmt7;

-- 积分流水表：可审计（谁/何时/为何/变了几），积分变更一律落此表
CREATE TABLE IF NOT EXISTS point_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    change_points INT NOT NULL COMMENT '正数增加/负数扣减',
    reason VARCHAR(128) NOT NULL COMMENT '如: 每日签到/聊天点赞/邀请奖励',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_point_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每日签到表：同用户同日唯一（幂等），points 为当日获得积分
CREATE TABLE IF NOT EXISTS sign_in (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sign_date DATE NOT NULL,
    points INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_signin_user_date (user_id, sign_date),
    KEY idx_signin_date (sign_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 邀请奖励表：分享裂变防刷（每个邀请人-被邀人组合只奖励一次）
CREATE TABLE IF NOT EXISTS invite_reward (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inviter_id BIGINT NOT NULL COMMENT '邀请人',
    invitee_id BIGINT NOT NULL COMMENT '被邀人（完成首聊后触发）',
    points INT NOT NULL DEFAULT 0 COMMENT '奖励积分',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invite_pair (inviter_id, invitee_id),
    KEY idx_invite_inviter (inviter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 意见反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    contact VARCHAR(128) DEFAULT '',
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 公告表
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 首次启动插入一条示例公告（仅当表为空时）
INSERT INTO announcement (title, content)
SELECT '欢迎使用 AI 职规助手', '欢迎使用 AI 职规助手！这里为你提供职业规划、学习路线、校招备战、面试辅导等服务。\n\n有问题可以直接和「AI 职规大师」对话，或让「AI 超级智能体」帮你规划任务。祝学习顺利！'
WHERE NOT EXISTS (SELECT 1 FROM announcement);

-- 对话会话表（跨设备同步聊天历史）：conversation_id 为后端生成的 UUID，同时作为记忆存储的会话主键
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    title VARCHAR(128) DEFAULT '新的职规咨询',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_conv_id (conversation_id),
    KEY idx_conversation_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对话消息表（完整历史落库）：role 为 user/assistant
CREATE TABLE IF NOT EXISTS conversation_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_cmsg_conversation (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 简历评分记录表：detail_json 存完整评分明细（维度/亮点/不足/优化版简历）
CREATE TABLE IF NOT EXISTS resume_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_position VARCHAR(128) DEFAULT '',
    resume_text TEXT NOT NULL,
    total_score INT NOT NULL,
    detail_json TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_resume_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 错误日志表（自建监控上报：后端 GlobalExceptionHandler 自动入库 + 前端全局捕获上报）
CREATE TABLE IF NOT EXISTS error_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level VARCHAR(16) DEFAULT 'ERROR',
    source VARCHAR(16) DEFAULT 'backend',
    message VARCHAR(2000) NOT NULL,
    stack_trace TEXT,
    uri VARCHAR(512) DEFAULT '',
    method VARCHAR(16) DEFAULT '',
    username VARCHAR(64) DEFAULT '',
    user_agent VARCHAR(512) DEFAULT '',
    ip VARCHAR(64) DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_error_created (created_at),
    KEY idx_error_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 八股错题本：question_id 为题目内容 hash，同一用户同一题唯一（重复答错累计次数）
CREATE TABLE IF NOT EXISTS bagu_wrong (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id VARCHAR(32) NOT NULL,
    question_content TEXT NOT NULL,
    category VARCHAR(32) DEFAULT '',
    wrong_count INT DEFAULT 1,
    last_wrong_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    mastered TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wrong_user_q (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 八股每日打卡：同用户同日期唯一（幂等）
CREATE TABLE IF NOT EXISTS bagu_checkin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    checkin_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_checkin_user_date (user_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 聊天问答反馈：message_id 为前端消息 uuid，同用户同消息唯一（赞/踩可切换），沉淀供 RAG 优化
CREATE TABLE IF NOT EXISTS chat_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NOT NULL,
    feedback_type VARCHAR(8) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fb_user_msg (user_id, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库管理表：RAG 检索/八股练习/超级智能体的统一事实源（首次启动从 career-tips.txt 导入）
CREATE TABLE IF NOT EXISTS knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(32) DEFAULT '综合',
    content TEXT NOT NULL,
    enabled TINYINT DEFAULT 1 COMMENT '1=启用参与检索,0=停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_knowledge_category (category),
    KEY idx_knowledge_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
