-- hnust-exam-ai 数据库初始化脚本
-- 适用于 MySQL 8.0+
-- 每次启动会 CREATE TABLE IF NOT EXISTS，幂等安全

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`      VARCHAR(50)     NOT NULL COMMENT '用户名',
    `password`      VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt）',
    `nickname`      VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    `email`         VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    `avatar_url`    VARCHAR(500)    DEFAULT NULL COMMENT '头像URL',
    `role`          TINYINT         NOT NULL DEFAULT 0 COMMENT '角色 0:普通用户 1:管理员',
    `is_active`     TINYINT         NOT NULL DEFAULT 1 COMMENT '是否启用',
    `study_streak`  INT             NOT NULL DEFAULT 0 COMMENT '连续打卡天数',
    `last_login_at` DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
-- Phase 2 升级注意：若数据库已存在(Phase 1 创建)，请手动执行：
-- ALTER TABLE t_user ADD COLUMN email VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER nickname;
-- ALTER TABLE t_user ADD COLUMN study_streak INT NOT NULL DEFAULT 0 COMMENT '连续打卡天数' AFTER is_active;

-- ----------------------------
-- 2. 科目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_subject` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '科目ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '科目名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `is_active`   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科目表';

-- ----------------------------
-- 3. 知识点表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_knowledge_point` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识点ID',
    `subject_id`  BIGINT       NOT NULL COMMENT '所属科目',
    `name`        VARCHAR(100) NOT NULL COMMENT '知识点名称',
    `description` TEXT         DEFAULT NULL COMMENT '描述',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_subject_id` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点表';

-- ----------------------------
-- 4. 题目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_question` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `subject_id`      BIGINT       NOT NULL COMMENT '科目ID',
    `knowledge_point_id` BIGINT    DEFAULT NULL COMMENT '知识点ID',
    `type`            TINYINT      NOT NULL DEFAULT 1 COMMENT '题型 1:单选 2:多选 3:判断 4:填空',
    `difficulty`      TINYINT      NOT NULL DEFAULT 2 COMMENT '难度 1:简单 2:中等 3:困难',
    `content`         TEXT         NOT NULL COMMENT '题干',
    `options`         JSON         DEFAULT NULL COMMENT '选项（JSON数组，填空题为null）',
    `answer`          VARCHAR(200) NOT NULL COMMENT '正确答案',
    `explanation`     TEXT         DEFAULT NULL COMMENT 'AI解析',
    `source`          TINYINT      NOT NULL DEFAULT 1 COMMENT '来源 1:AI生成 2:管理员录入',
    `ai_cache_key`    VARCHAR(200) DEFAULT NULL COMMENT 'AI缓存键',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_subject_id` (`subject_id`),
    KEY `idx_knowledge_point_id` (`knowledge_point_id`),
    KEY `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- ----------------------------
-- 5. 用户答题记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_user_answer` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `question_id`   BIGINT       NOT NULL COMMENT '题目ID',
    `session_id`    VARCHAR(64)  DEFAULT NULL COMMENT '答题会话ID',
    `user_answer`   TEXT         NOT NULL COMMENT '用户答案',
    `is_correct`    TINYINT      NOT NULL COMMENT '是否正确',
    `time_spent_s`  INT          DEFAULT NULL COMMENT '作答耗时（秒）',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户答题记录';

-- ----------------------------
-- 6. 错题本表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_error_book` (
    `id`                    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`               BIGINT   NOT NULL COMMENT '用户ID',
    `question_id`           BIGINT   NOT NULL COMMENT '题目ID',
    `error_reason`          TINYINT  DEFAULT NULL COMMENT '错因标签 1概念不清/2审题失误/3计算失误/4方法不会/5时间不足/6粗心',
    `is_mastered`           TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已掌握',
    `review_count`          INT      NOT NULL DEFAULT 0 COMMENT '复习次数',
    `consecutive_correct`   INT      NOT NULL DEFAULT 0 COMMENT '连续正确次数',
    `review_interval_days`  INT      NOT NULL DEFAULT 1 COMMENT '当前间隔天数',
    `next_review_at`        DATETIME DEFAULT NULL COMMENT '下次复习时间（SRS）',
    `created_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_next_review_at` (`next_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- ----------------------------
-- 7. 笔记表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_note` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `subject_id`    BIGINT       DEFAULT NULL COMMENT '关联科目',
    `title`         VARCHAR(200) NOT NULL COMMENT '标题',
    `content`       LONGTEXT     DEFAULT NULL COMMENT '内容（Markdown）',
    `image_url`     VARCHAR(500) DEFAULT NULL COMMENT '原始图片URL（OCR笔记）',
    `ocr_text`      LONGTEXT     DEFAULT NULL COMMENT 'OCR识别文本',
    `source`        TINYINT      NOT NULL DEFAULT 1 COMMENT '来源 1:手动 2:OCR 3:AI整理',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_subject_id` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

-- ----------------------------
-- 8. 复习提纲表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_review_outline` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '提纲ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `title`         VARCHAR(200) NOT NULL COMMENT '提纲标题',
    `content`       LONGTEXT     NOT NULL COMMENT '提纲内容（Markdown）',
    `note_ids`      JSON         DEFAULT NULL COMMENT '来源笔记ID列表',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复习提纲';

-- ----------------------------
-- 9. 学习统计汇总表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_study_stat` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `stat_date`       DATE   NOT NULL COMMENT '统计日期',
    `answer_count`    INT    NOT NULL DEFAULT 0 COMMENT '答题总数',
    `correct_count`   INT    NOT NULL DEFAULT 0 COMMENT '正确数',
    `study_minutes`   INT    NOT NULL DEFAULT 0 COMMENT '学习时长（分钟）',
    `checkin`         TINYINT NOT NULL DEFAULT 0 COMMENT '是否打卡',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习统计汇总';

-- ----------------------------
-- 10. 模考记录表（Phase 8）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_mock_exam` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模考ID',
    `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
    `subject_id`      BIGINT       DEFAULT NULL COMMENT '科目ID',
    `title`           VARCHAR(100) DEFAULT NULL COMMENT '模考标题',
    `time_limit_min`  INT          NOT NULL COMMENT '考试时长（分钟）',
    `total_questions` INT          NOT NULL COMMENT '题目总数',
    `total_correct`   INT          NOT NULL DEFAULT 0 COMMENT '正确数',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0:进行中 1:已完成 2:超时',
    `started_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    `submitted_at`    DATETIME     DEFAULT NULL COMMENT '提交时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模考记录';

-- ----------------------------
-- 11. 模考题目明细表（Phase 8）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_mock_exam_question` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `exam_id`     BIGINT   NOT NULL COMMENT '模考ID',
    `question_id` BIGINT   NOT NULL COMMENT '题目ID',
    `sort_order`  INT      NOT NULL COMMENT '序号',
    `is_flagged`  TINYINT  NOT NULL DEFAULT 0 COMMENT '是否标记',
    `user_answer` TEXT     DEFAULT NULL COMMENT '用户答案',
    `is_correct`  TINYINT  DEFAULT NULL COMMENT '是否正确',
    PRIMARY KEY (`id`),
    KEY `idx_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模考题目明细';

-- ----------------------------
-- 12. 抽认卡组（Phase 9）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flashcard_deck` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '卡组ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `note_id`     BIGINT       DEFAULT NULL COMMENT '来源笔记',
    `title`       VARCHAR(200) NOT NULL COMMENT '卡组标题',
    `card_count`  INT          NOT NULL DEFAULT 0 COMMENT '卡片数量',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽认卡组';

-- ----------------------------
-- 13. 抽认卡（Phase 9）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flashcard` (
    `id`                    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '卡片ID',
    `deck_id`               BIGINT   NOT NULL COMMENT '卡组ID',
    `user_id`               BIGINT   NOT NULL COMMENT '用户ID',
    `front`                 TEXT     NOT NULL COMMENT '问题面',
    `back`                  TEXT     NOT NULL COMMENT '答案面',
    `sort_order`            INT      NOT NULL DEFAULT 0 COMMENT '排序',
    `next_review_at`        DATETIME DEFAULT NULL COMMENT '下次复习时间',
    `review_interval_days`  INT      NOT NULL DEFAULT 1 COMMENT '当前间隔天数',
    `review_count`          INT      NOT NULL DEFAULT 0 COMMENT '总复习次数',
    `consecutive_correct`   INT      NOT NULL DEFAULT 0 COMMENT '连续正确次数',
    `created_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_deck_id` (`deck_id`),
    KEY `idx_next_review_at` (`next_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽认卡';

-- ----------------------------
-- 14. 题目纠错反馈（Phase 10）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_question_feedback` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `question_id`    BIGINT       NOT NULL COMMENT '题目ID',
    `type`           TINYINT      NOT NULL COMMENT '类型 1答案错误/2题干歧义/3解析不清/4排版问题',
    `description`    TEXT         DEFAULT NULL COMMENT '描述',
    `screenshot_url` VARCHAR(500) DEFAULT NULL COMMENT '截图URL',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待处理/1已采纳/2已驳回/3已修复',
    `admin_id`       BIGINT       DEFAULT NULL COMMENT '处理管理员',
    `admin_reply`    TEXT         DEFAULT NULL COMMENT '管理员回复',
    `replied_at`     DATETIME     DEFAULT NULL COMMENT '回复时间',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目纠错反馈';

SET FOREIGN_KEY_CHECKS = 1;
