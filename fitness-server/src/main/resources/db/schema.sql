CREATE DATABASE IF NOT EXISTS fitness_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fitness_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt 编码',
    `height_cm` DECIMAL(5,1) DEFAULT NULL COMMENT '身高(cm)',
    `target_weight_kg` DECIMAL(5,1) DEFAULT NULL COMMENT '目标体重(kg)',
    `goal` VARCHAR(20) DEFAULT NULL COMMENT 'gain/cut/maintain',
    `refresh_token` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 训练记录主表
CREATE TABLE IF NOT EXISTS `workout_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `session_date` DATE NOT NULL,
    `body_parts` JSON DEFAULT NULL COMMENT '["chest","shoulder"]',
    `total_sets` INT NOT NULL DEFAULT 0 COMMENT '组数总和',
    `feel_rating` TINYINT DEFAULT NULL COMMENT '1-5 评分',
    `notes` VARCHAR(500) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_date` (`user_id`, `session_date`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练记录主表';

-- 动作明细表
CREATE TABLE IF NOT EXISTS `workout_exercise` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL,
    `exercise_name` VARCHAR(100) NOT NULL,
    `set_number` TINYINT NOT NULL COMMENT '第几组',
    `weight_kg` DECIMAL(5,1) DEFAULT NULL,
    `reps` TINYINT DEFAULT NULL,
    `rpe` DECIMAL(2,1) DEFAULT NULL COMMENT '1.0-10.0',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_session_id` (`session_id`),
    FOREIGN KEY (`session_id`) REFERENCES `workout_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作明细表';

-- 动作库表
CREATE TABLE IF NOT EXISTS `exercise_library` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT DEFAULT NULL COMMENT 'NULL=系统内置，非NULL=用户自定义',
    `name` VARCHAR(100) NOT NULL,
    `target_muscle` VARCHAR(20) NOT NULL COMMENT 'chest/back/shoulder/arms/legs/other',
    `equipment` VARCHAR(20) NOT NULL COMMENT 'barbell/dumbbell/machine/cable/bodyweight',
    `type` VARCHAR(10) NOT NULL COMMENT 'compound/isolation',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作库表';

-- 体重记录表
CREATE TABLE IF NOT EXISTS `weight_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `weight_kg` DECIMAL(5,1) NOT NULL,
    `record_date` DATE NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体重记录表';

-- 反馈表
CREATE TABLE IF NOT EXISTS `feedback` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈表';
