-- Public submission and admin review migration.
-- Run against the existing novel database after reader-private-import-migration.mysql.sql.
USE `novel`;

CREATE TABLE IF NOT EXISTS `admin_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(100) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 disabled',
  `last_login_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_username` (`username`),
  KEY `idx_admin_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `novel_submission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_novel_id` BIGINT UNSIGNED DEFAULT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `visibility` VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE or PUBLIC',
  `book_name` VARCHAR(128) NOT NULL,
  `author_name` VARCHAR(64) NOT NULL,
  `category_id` BIGINT UNSIGNED NOT NULL,
  `book_desc` TEXT,
  `rights_confirmed` TINYINT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, REJECTED, APPROVED, CANCELLED',
  `rejection_reason` VARCHAR(500) DEFAULT NULL,
  `public_book_id` BIGINT UNSIGNED DEFAULT NULL,
  `public_status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, WITHDRAWN, TAKEN_DOWN',
  `reviewed_by` BIGINT UNSIGNED DEFAULT NULL,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_novel_submission_novel` (`user_novel_id`),
  KEY `idx_novel_submission_status_time` (`status`, `submitted_at` DESC),
  KEY `idx_novel_submission_user` (`user_id`, `status`),
  KEY `idx_novel_submission_public_book` (`public_book_id`),
  CONSTRAINT `fk_novel_submission_novel` FOREIGN KEY (`user_novel_id`) REFERENCES `user_novel` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_novel_submission_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_novel_submission_category` FOREIGN KEY (`category_id`) REFERENCES `book_category` (`id`),
  CONSTRAINT `fk_novel_submission_admin` FOREIGN KEY (`reviewed_by`) REFERENCES `admin_user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_novel_submission_book` FOREIGN KEY (`public_book_id`) REFERENCES `book_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `novel_withdrawal_request` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_novel_id` BIGINT UNSIGNED DEFAULT NULL,
  `public_book_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `reason` VARCHAR(500) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, CLOSED',
  `reviewed_by` BIGINT UNSIGNED DEFAULT NULL,
  `reviewed_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_withdrawal_status_time` (`status`, `create_time` DESC),
  KEY `idx_withdrawal_user` (`user_id`, `status`),
  CONSTRAINT `fk_withdrawal_novel` FOREIGN KEY (`user_novel_id`) REFERENCES `user_novel` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_withdrawal_book` FOREIGN KEY (`public_book_id`) REFERENCES `book_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_withdrawal_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_withdrawal_admin` FOREIGN KEY (`reviewed_by`) REFERENCES `admin_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `admin_audit_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT UNSIGNED NOT NULL,
  `target_type` VARCHAR(32) NOT NULL,
  `target_id` BIGINT UNSIGNED NOT NULL,
  `action` VARCHAR(32) NOT NULL,
  `reason` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_audit_target` (`target_type`, `target_id`, `create_time` DESC),
  KEY `idx_admin_audit_admin` (`admin_id`, `create_time` DESC),
  CONSTRAINT `fk_admin_audit_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
