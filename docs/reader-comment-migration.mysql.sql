-- 作品评论增量迁移脚本
-- 适用于已经执行 reader-database.mysql.sql 的 novel 数据库。
USE `novel`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `book_comment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `book_id` BIGINT UNSIGNED NOT NULL COMMENT '书籍ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
  `parent_comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '主评论ID，NULL表示主评论',
  `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常，1已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_comment_thread` (`book_id`, `parent_comment_id`, `status`, `create_time`, `id`),
  KEY `idx_book_comment_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_book_comment_book` FOREIGN KEY (`book_id`) REFERENCES `book_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_book_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_book_comment_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `book_comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品评论表';

SET FOREIGN_KEY_CHECKS = 1;
