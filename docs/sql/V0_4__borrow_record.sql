-- V0.4 · 借阅闭环（对应 PRD v0.4 交付物，建表语句以 PRD 内为准）
-- 执行方式：按版本顺序手动执行一次，执行后本文件留存作为 DDL 记录，不重复执行。
--           （语句以 PRD 内为准、不含 USE，执行时需指定库：mysql ... jdbc_demo < 本文件）
-- 背景：BorrowService 借/还事务依赖本表；借阅记录只改状态、禁止物理删除；
--       逾期为派生态（status='active' AND due_at < NOW()），不落库、不加字段（PRD 2.3）。
-- 执行记录：2026-11-06 已在本机 MySQL（Windows 原生服务 localhost:3306，库 jdbc_demo）执行一次。

CREATE TABLE borrow_record (
    id           INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id      INT NOT NULL,
    user_id      INT NOT NULL,
    borrowed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at       DATETIME NOT NULL,
    returned_at  DATETIME NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'active',
    KEY idx_book (book_id),
    KEY idx_user (user_id),
    KEY idx_due (status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
