-- TopLinks 分享功能优化迁移
-- 日期：2026-03-17
-- 说明：添加审计日志表，优化现有分享表性能

-- ============================================
-- 1. 创建分享审计日志表
-- ============================================
CREATE TABLE IF NOT EXISTS `TLK_SHARE_AUDIT_LOG` (
    `id` VARCHAR(64) PRIMARY KEY,
    `share_id` VARCHAR(64) NOT NULL COMMENT '分享 ID',
    `share_token` VARCHAR(64) NOT NULL COMMENT '分享 Token',
    `visitor_ip` VARCHAR(45) COMMENT '访问者 IP',
    `action_type` VARCHAR(32) NOT NULL COMMENT '访问类型：view/download/password_attempt',
    `is_success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
    `failure_reason` VARCHAR(255) COMMENT '失败原因',
    `user_agent` VARCHAR(512) COMMENT 'User-Agent',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_share_id` (`share_id`),
    INDEX `idx_share_token` (`share_token`),
    INDEX `idx_visitor_ip` (`visitor_ip`),
    INDEX `idx_action_type` (`action_type`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享访问审计日志表';

-- ============================================
-- 2. 优化 TLK_SHARE 表索引
-- ============================================
-- 添加复合索引加速清理操作
CREATE INDEX IF NOT EXISTS `idx_status_expire` ON `TLK_SHARE` (`status`, `expire_time`);
CREATE INDEX IF NOT EXISTS `idx_status_downloads` ON `TLK_SHARE` (`status`, `max_downloads`, `download_count`);

-- ============================================
-- 3. 添加分享统计视图（可选）
-- ============================================
CREATE OR REPLACE VIEW `TLK_SHARE_STATS` AS
SELECT 
    s.id AS share_id,
    s.share_token,
    s.file_id,
    s.download_count,
    s.max_downloads,
    s.expire_time,
    s.status,
    COUNT(CASE WHEN a.action_type = 'view' THEN 1 END) AS view_count,
    COUNT(CASE WHEN a.action_type = 'download' THEN 1 END) AS audit_download_count,
    COUNT(CASE WHEN a.success = 0 THEN 1 END) AS failed_attempts,
    MAX(a.create_time) AS last_access_time
FROM TLK_SHARE s
LEFT JOIN TLK_SHARE_AUDIT_LOG a ON s.id = a.share_id
GROUP BY s.id, s.share_token, s.file_id, s.download_count, s.max_downloads, 
         s.expire_time, s.status;

-- ============================================
-- 4. 数据清理策略（可选手动执行）
-- ============================================
-- 清理 30 天前的审计日志（保留最近一个月的详细日志）
-- DELETE FROM TLK_SHARE_AUDIT_LOG WHERE create_time < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- ============================================
-- 迁移完成
-- ============================================
