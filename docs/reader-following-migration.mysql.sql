-- 已有 novel 数据库的增量迁移脚本
-- 执行前请确认当前数据库已经包含 sys_user、book_info、book_chapter 表。

USE `novel`;

CREATE TABLE IF NOT EXISTS `user_book_follow` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关注记录ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `book_id` BIGINT UNSIGNED NOT NULL COMMENT '书籍ID',
  `last_notified_chapter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '用户已确认的最新章节ID',
  `last_notified_at` DATETIME DEFAULT NULL COMMENT '最近确认提醒时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_book_follow_user_book` (`user_id`, `book_id`),
  KEY `idx_user_book_follow_user_time` (`user_id`, `create_time` DESC),
  KEY `idx_user_book_follow_book` (`book_id`),
  KEY `idx_user_book_follow_chapter` (`last_notified_chapter_id`),
  CONSTRAINT `fk_user_book_follow_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_book_follow_book` FOREIGN KEY (`book_id`) REFERENCES `book_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_book_follow_chapter` FOREIGN KEY (`last_notified_chapter_id`) REFERENCES `book_chapter` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注作品表';
