-- ============================================================
-- DocuAgent · 建表脚本
-- ------------------------------------------------------------
-- 约定（沿用 CampusMart）：
--   1. 只写 CREATE TABLE，不写 ALTER —— 保证脚本可重复执行
--   2. 每张表都带 create_time / update_time / deleted（配合 MyBatis-Plus 全局逻辑删除）
--   3. 字段名用下划线，每个字段都写 COMMENT
-- 用法：mysql -uroot -p < sql/init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS docuagent
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE docuagent;

-- ============================================================
-- ① course · 课程
-- ============================================================
CREATE TABLE IF NOT EXISTS course (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课程ID',
    name        VARCHAR(100) NOT NULL                COMMENT '课程名称',
    description VARCHAR(500)                         COMMENT '课程介绍',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '课程表';

-- ============================================================
-- ② document · 课件
-- ============================================================
CREATE TABLE IF NOT EXISTS document (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '课件ID',
    course_id      BIGINT        NOT NULL                COMMENT '所属课程ID',
    original_name  VARCHAR(255)  NOT NULL                COMMENT '原始文件名',
    storage_path   VARCHAR(500)  NOT NULL                COMMENT '存储路径（相对路径）',
    file_type      VARCHAR(20)   NOT NULL                COMMENT '文件类型：pdf / ppt / pptx / docx',
    file_size      BIGINT        NOT NULL                COMMENT '文件大小（字节）',
    status         VARCHAR(20)   NOT NULL DEFAULT 'UPLOADED'
                   COMMENT '状态：UPLOADED 已上传 / PARSING 解析中 / INDEXING 向量化中 / DONE 完成 / FAILED 失败',
    failure_reason VARCHAR(1000)                         COMMENT '失败原因（仅 FAILED 时有值）',
    char_count     INT                                   COMMENT '提取出的字符数（解析成功后填）',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    INDEX idx_document_course_id (course_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '课件表';

-- ============================================================
-- ③ chunk · 分块（检索的最小单位）
-- ============================================================
CREATE TABLE IF NOT EXISTS chunk (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '分块ID',
    document_id BIGINT   NOT NULL                COMMENT '所属课件ID',
    chunk_index INT      NOT NULL                COMMENT '在原文中的序号（从 0 开始）',
    content     TEXT     NOT NULL                COMMENT '分块原文',
    char_count  INT      NOT NULL                COMMENT '字符数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    INDEX idx_chunk_document_id (document_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '课件分块表';

-- ============================================================
-- ④ question · 题目
-- ============================================================
CREATE TABLE IF NOT EXISTS question (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    course_id       BIGINT        NOT NULL                COMMENT '所属课程ID',
    type            VARCHAR(20)   NOT NULL                COMMENT '题型：single_choice 单选 / short_answer 简答',
    stem            TEXT          NOT NULL                COMMENT '题干',
    options         VARCHAR(1000)                         COMMENT '选项（JSON 数组字符串，仅选择题有值）',
    answer          VARCHAR(500)  NOT NULL                COMMENT '参考答案',
    key_points      VARCHAR(1000)                         COMMENT '评分要点（批改时逐条比对用）',
    source_chunk_id BIGINT                                COMMENT '来源分块ID（批改引用的依据）',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    INDEX idx_question_course_id (course_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '题目表';

-- ============================================================
-- ⑤ answer_record · 答题记录
-- ============================================================
CREATE TABLE IF NOT EXISTS answer_record (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '答题记录ID',
    question_id BIGINT        NOT NULL                COMMENT '题目ID',
    user_answer TEXT                                  COMMENT '用户提交的答案',
    score       DECIMAL(5,2)                          COMMENT '得分',
    max_score   DECIMAL(5,2)                          COMMENT '满分',
    is_correct  TINYINT       NOT NULL DEFAULT 0      COMMENT '是否正确：0否 1是',
    comment     VARCHAR(1000)                         COMMENT '批改评语',
    duration_ms INT                                   COMMENT '答题耗时（毫秒）',
    request_id  VARCHAR(64)                           COMMENT '请求ID（幂等去重用）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    INDEX idx_answer_record_question_id (question_id),
    UNIQUE KEY uk_answer_record_request_id (request_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '答题记录表';

-- ============================================================
-- ⑥ wrong_question · 错题本
-- ============================================================
CREATE TABLE IF NOT EXISTS wrong_question (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '错题记录ID',
    question_id      BIGINT      NOT NULL                COMMENT '题目ID',
    wrong_count      INT         NOT NULL DEFAULT 0      COMMENT '答错次数（仅 auto 来源累加）',
    source           VARCHAR(20) NOT NULL DEFAULT 'auto' COMMENT '来源：auto 批改自动入库 / manual 手动收藏',
    first_wrong_time DATETIME                            COMMENT '首次答错时间',
    last_wrong_time  DATETIME                            COMMENT '最近一次答错时间',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wrong_question_question_id (question_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '错题本表';
