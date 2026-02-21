-- TopLinks 数据库建表脚本
-- 使用 utf8mb4 编码以支持 emoji 等扩展字符

-- 用户表
CREATE TABLE IF NOT EXISTS `SYS_USER` (
  `id`          VARCHAR(36)  NOT NULL PRIMARY KEY COMMENT '主键 UUID',
  `username`    VARCHAR(100)          COMMENT '用户名',
  `email`       VARCHAR(255) NOT NULL COMMENT '邮箱（唯一）',
  `password`    VARCHAR(255)          COMMENT 'BCrypt 加密密码，OAuth 用户可为空',
  `google_id`   VARCHAR(255)          COMMENT 'Google 账号 sub',
  `avatar`      VARCHAR(500)          COMMENT '头像 URL',
  `nickname`    VARCHAR(255)          COMMENT '昵称',
  `status`      INT      DEFAULT 1    COMMENT '状态：1 启用 2 禁用',
  `deleted`     INT      DEFAULT 0    COMMENT '软删除：0 正常 1 已删除',
  `create_by`   VARCHAR(36)           COMMENT '创建人',
  `create_time` DATETIME              COMMENT '创建时间',
  `update_by`   VARCHAR(36)           COMMENT '修改人',
  `update_time` DATETIME              COMMENT '修改时间',
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分类表
CREATE TABLE IF NOT EXISTS `TLK_CATEGORY` (
  `id`          VARCHAR(36)  NOT NULL PRIMARY KEY COMMENT '主键 UUID',
  `name`        VARCHAR(100) NOT NULL              COMMENT '分类名称',
  `description` VARCHAR(500)                       COMMENT '分类描述',
  `icon`        VARCHAR(100)                       COMMENT 'Bootstrap icon class，如 bi-image',
  `sort_order`  INT      DEFAULT 0                 COMMENT '排序权重，越小越靠前',
  `status`      INT      DEFAULT 1                 COMMENT '状态：1 启用 2 禁用',
  `deleted`     INT      DEFAULT 0                 COMMENT '软删除',
  `create_by`   VARCHAR(36)                        COMMENT '创建人',
  `create_time` DATETIME                           COMMENT '创建时间',
  `update_by`   VARCHAR(36)                        COMMENT '修改人',
  `update_time` DATETIME                           COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分类表';

-- 文件表
CREATE TABLE IF NOT EXISTS `TLK_FILE` (
  `id`          VARCHAR(36)   NOT NULL PRIMARY KEY COMMENT '主键 UUID',
  `name`        VARCHAR(500)  NOT NULL              COMMENT '原始文件名',
  `path`        VARCHAR(500)                        COMMENT '存储对象键（R2 key 或本地路径）',
  `uid`         VARCHAR(10)   NOT NULL              COMMENT '6 位唯一访问码',
  `ext`         VARCHAR(20)                         COMMENT '扩展名（不含点）',
  `size`        BIGINT                              COMMENT '文件大小（字节）',
  `hash`        VARCHAR(64)                         COMMENT '文件 MD5 哈希值',
  `create_ip`   VARCHAR(50)                         COMMENT '上传者 IP',
  `user_id`     VARCHAR(36)                         COMMENT '上传用户 ID',
  `category_id` VARCHAR(36)                         COMMENT '所属分类 ID',
  `cloud_url`   VARCHAR(1000)                       COMMENT 'Cloudflare R2 公开访问地址',
  `status`      INT      DEFAULT 1                  COMMENT '状态：1 启用 2 禁用',
  `deleted`     INT      DEFAULT 0                  COMMENT '软删除',
  `create_by`   VARCHAR(36)                         COMMENT '创建人',
  `create_time` DATETIME                            COMMENT '创建时间',
  `update_by`   VARCHAR(36)                         COMMENT '修改人',
  `update_time` DATETIME                            COMMENT '修改时间',
  UNIQUE KEY `uk_uid` (`uid`),
  KEY `idx_category` (`category_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件存储表';

-- 示例分类数据
INSERT IGNORE INTO `TLK_CATEGORY` (`id`, `name`, `description`, `icon`, `sort_order`, `status`, `deleted`, `create_time`) VALUES
  ('cat-001', '图片',   '图片类文件',   'bi-image',               1, 1, 0, NOW()),
  ('cat-002', '文档',   '文档类文件',   'bi-file-earmark-word',   2, 1, 0, NOW()),
  ('cat-003', 'PDF',    'PDF 文件',     'bi-file-earmark-pdf',    3, 1, 0, NOW()),
  ('cat-004', '音频',   '音频类文件',   'bi-music-note-beamed',   4, 1, 0, NOW()),
  ('cat-005', '视频',   '视频类文件',   'bi-play-circle',         5, 1, 0, NOW()),
  ('cat-006', '其他',   '其他类型文件', 'bi-file-earmark',        6, 1, 0, NOW());
