-- Private TXT imports. Run against the existing novel database once.
USE `novel`;

CREATE TABLE IF NOT EXISTS `user_novel` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `book_name` VARCHAR(200) NOT NULL,
  `original_file_name` VARCHAR(255) NOT NULL,
  `encoding` VARCHAR(20) NOT NULL,
  `file_size` BIGINT UNSIGNED NOT NULL,
  `word_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `chapter_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `first_chapter_id` BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_novel_owner_time` (`user_id`, `create_time` DESC, `id` DESC),
  CONSTRAINT `fk_user_novel_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_novel_chapter` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `novel_id` BIGINT UNSIGNED NOT NULL,
  `chapter_num` INT UNSIGNED NOT NULL,
  `chapter_name` VARCHAR(200) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `word_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_novel_chapter_num` (`novel_id`, `chapter_num`),
  CONSTRAINT `fk_user_novel_chapter_novel` FOREIGN KEY (`novel_id`) REFERENCES `user_novel` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_novel_progress` (
  `novel_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `chapter_id` BIGINT UNSIGNED NOT NULL,
  `read_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`novel_id`),
  KEY `idx_user_novel_progress_owner` (`user_id`, `read_at` DESC),
  CONSTRAINT `fk_user_novel_progress_novel` FOREIGN KEY (`novel_id`) REFERENCES `user_novel` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
