-- Apply after reader-publication-migration.mysql.sql.
USE `novel`;

ALTER TABLE `user_novel` ADD COLUMN `cover_url` VARCHAR(255) DEFAULT NULL COMMENT 'OSS cover URL';
ALTER TABLE `novel_submission` ADD COLUMN `cover_url` VARCHAR(255) DEFAULT NULL COMMENT 'Reviewed cover URL snapshot';
