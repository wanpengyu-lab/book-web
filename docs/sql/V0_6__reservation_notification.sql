-- V0.6 · 预约与共借推荐接口（建表语句以 PRD v0.6 内为准）
-- 执行方式：按版本顺序手动执行一次，执行后本文件留存作为 DDL 记录，不重复执行。
--           （语句以 PRD 内为准、不含 USE，执行时需指定库：mysql ... jdbc_demo < 本文件）
-- 背景：预约状态机 waiting → notified → fulfilled，取消为 cancelled（PRD 2.3）；
--       预约通知在还书事务内生成（禁止异步补通知）；通知只做站内（notification 表），禁止邮件/短信/MQ。
-- 执行记录：2026-11-06 已在本机 MySQL（Windows 原生服务 localhost:3306，库 jdbc_demo）执行一次。

CREATE TABLE reservation (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id    INT NOT NULL,
    user_id    INT NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'waiting',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_book_status (book_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    content    VARCHAR(500) NOT NULL,
    is_read    TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
