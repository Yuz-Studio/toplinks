-- ============================================
-- TopLinks 双链接系统数据库迁移
-- 版本：v1.1.0
-- 日期：2026-03-14
-- 说明：添加文件分享功能支持
-- ============================================

-- 1. 在 TLK_FILE 表中增加公开标记
ALTER TABLE TLK_FILE 
ADD COLUMN public_visible TINYINT(1) DEFAULT 0 COMMENT '是否公开可见' AFTER cloud_url;

-- 2. 创建 TLK_SHARE 表（文件分享表）
CREATE TABLE IF NOT EXISTS TLK_SHARE (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键 ID',
    file_id VARCHAR(64) NOT NULL COMMENT '关联文件 ID',
    share_token VARCHAR(64) NOT NULL UNIQUE COMMENT '分享 token（32 位随机）',
    share_password VARCHAR(255) DEFAULT NULL COMMENT '分享密码（加密存储）',
    require_password TINYINT(1) DEFAULT 0 COMMENT '是否需要密码',
    max_downloads INT DEFAULT NULL COMMENT '最大下载次数',
    download_count INT DEFAULT 0 COMMENT '已下载次数',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    created_by VARCHAR(64) NOT NULL COMMENT '创建者 ID',
    description VARCHAR(255) DEFAULT NULL COMMENT '分享描述',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active/inactive',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_share_token (share_token),
    INDEX idx_file_id (file_id),
    INDEX idx_created_by (created_by),
    INDEX idx_expire_time (expire_time),
    INDEX idx_status (status),
    FOREIGN KEY (file_id) REFERENCES TLK_FILE(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';

-- 3. 验证迁移结果
SELECT 'TLK_FILE 表结构验证' AS check_item;
DESCRIBE TLK_FILE;

SELECT 'TLK_SHARE 表创建验证' AS check_item;
DESCRIBE TLK_SHARE;

-- 4. 回滚脚本（如需回滚请谨慎使用）
/*
-- 删除 TLK_SHARE 表
DROP TABLE IF EXISTS TLK_SHARE;

-- 删除 TLK_FILE 表的新增字段
ALTER TABLE TLK_FILE DROP COLUMN public_visible;
*/

-- ============================================
-- 迁移完成
-- ============================================
