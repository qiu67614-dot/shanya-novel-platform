-- Production bootstrap for a new, empty MySQL 8 database. Run once.
-- No DROP statements or sample novels are included.
CREATE DATABASE IF NOT EXISTS `novel`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `novel`;
SET NAMES utf8mb4;

CREATE TABLE `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号，建议手机号',
  `password` VARCHAR(100) NOT NULL COMMENT '密码哈希',
  `nick_name` VARCHAR(64) NOT NULL DEFAULT '书友' COMMENT '昵称',
  `user_photo` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `user_sex` TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0保密，1男，2女',
  `user_birthday` VARCHAR(20) DEFAULT NULL COMMENT '生日，格式 yyyy-MM-dd',
  `user_phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  `user_email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `balance` INT NOT NULL DEFAULT 0 COMMENT '兼容旧字段，阅读端不使用余额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常，1禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='正式用户表';

CREATE TABLE `book_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `direction` TINYINT NOT NULL DEFAULT 0 COMMENT '频道方向：0男生，1女生',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0启用，1停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_category_direction_name` (`direction`, `category_name`),
  KEY `idx_book_category_sort` (`direction`, `status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说分类表';

CREATE TABLE `book_info` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '书籍ID',
  `book_name` VARCHAR(128) NOT NULL COMMENT '书名',
  `author_name` VARCHAR(64) NOT NULL COMMENT '作者名',
  `category_id` BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
  `pic_url` VARCHAR(255) DEFAULT NULL COMMENT '封面相对路径或完整URL',
  `book_desc` TEXT COMMENT '作品简介',
  `book_status` TINYINT NOT NULL DEFAULT 0 COMMENT '作品状态：0连载中，1已完结',
  `word_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总字数',
  `visit_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点击量/阅读量',
  `chapter_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '章节数',
  `first_chapter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '第一章ID',
  `last_chapter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最新章节ID',
  `last_chapter_name` VARCHAR(128) DEFAULT NULL COMMENT '最新章节名',
  `last_chapter_update_time` DATETIME DEFAULT NULL COMMENT '最新章节更新时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0上架，1下架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_info_category` (`category_id`, `status`, `book_status`),
  KEY `idx_book_info_visit` (`status`, `visit_count` DESC),
  KEY `idx_book_info_update` (`status`, `last_chapter_update_time` DESC),
  KEY `idx_book_info_create` (`status`, `create_time` DESC),
  KEY `idx_book_info_name` (`book_name`),
  KEY `idx_book_info_author` (`author_name`),
  CONSTRAINT `fk_book_info_category` FOREIGN KEY (`category_id`) REFERENCES `book_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说信息表';

CREATE TABLE `book_chapter` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `book_id` BIGINT UNSIGNED NOT NULL COMMENT '书籍ID',
  `chapter_num` INT UNSIGNED NOT NULL COMMENT '章节序号，从1开始',
  `chapter_name` VARCHAR(128) NOT NULL COMMENT '章节名',
  `word_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '章节字数',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '章节更新时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_chapter_num` (`book_id`, `chapter_num`),
  KEY `idx_book_chapter_book_id_id` (`book_id`, `id`),
  CONSTRAINT `fk_book_chapter_book` FOREIGN KEY (`book_id`) REFERENCES `book_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说章节表';

CREATE TABLE `book_content` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '正文ID',
  `chapter_id` BIGINT UNSIGNED NOT NULL COMMENT '章节ID',
  `content` LONGTEXT NOT NULL COMMENT '章节正文',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_content_chapter` (`chapter_id`),
  CONSTRAINT `fk_book_content_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `book_chapter` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说正文表';

CREATE TABLE `user_bookshelf` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '书架ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `book_id` BIGINT UNSIGNED NOT NULL COMMENT '书籍ID',
  `last_chapter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最近阅读章节ID',
  `last_chapter_name` VARCHAR(128) DEFAULT NULL COMMENT '最近阅读章节名',
  `last_read_at` DATETIME DEFAULT NULL COMMENT '最近阅读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入书架时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_bookshelf_user_book` (`user_id`, `book_id`),
  KEY `idx_user_bookshelf_user_time` (`user_id`, `last_read_at` DESC),
  KEY `idx_user_bookshelf_book` (`book_id`),
  KEY `idx_user_bookshelf_chapter` (`last_chapter_id`),
  CONSTRAINT `fk_user_bookshelf_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_bookshelf_book` FOREIGN KEY (`book_id`) REFERENCES `book_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_bookshelf_chapter` FOREIGN KEY (`last_chapter_id`) REFERENCES `book_chapter` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户书架表';

CREATE TABLE `user_read_history` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `book_id` BIGINT UNSIGNED NOT NULL COMMENT '书籍ID',
  `chapter_id` BIGINT UNSIGNED NOT NULL COMMENT '最近阅读章节ID',
  `chapter_name` VARCHAR(128) NOT NULL COMMENT '最近阅读章节名',
  `read_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近阅读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_read_history_user_book` (`user_id`, `book_id`),
  KEY `idx_user_read_history_user_time` (`user_id`, `read_at` DESC),
  KEY `idx_user_read_history_book` (`book_id`),
  KEY `idx_user_read_history_chapter` (`chapter_id`),
  CONSTRAINT `fk_user_read_history_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_read_history_book` FOREIGN KEY (`book_id`) REFERENCES `book_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_read_history_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `book_chapter` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户阅读历史表';

CREATE TABLE `book_comment` (
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

CREATE TABLE `user_book_follow` (
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

-- Category names are required for browsing; this is not sample book content.
INSERT INTO `book_category` (`id`, `category_name`, `direction`, `sort`, `status`) VALUES
  (1, '玄幻', 0, 10, 0),
  (2, '都市', 0, 20, 0),
  (3, '仙侠', 0, 30, 0),
  (4, '历史', 0, 40, 0),
  (5, '科幻', 0, 50, 0),
  (6, '悬疑', 0, 60, 0),
  (7, '古言', 1, 10, 0),
  (8, '现言', 1, 20, 0),
  (9, '幻想言情', 1, 30, 0),
  (10, '青春校园', 1, 40, 0);
