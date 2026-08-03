-- 新电脑拉取代码后首次运行会自动执行（dev 环境）
-- 若已存在表则跳过，需手动创建数据库：CREATE DATABASE ai_love_master;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
