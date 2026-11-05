-- V0.3 · 用户与鉴权（对应 PRD v0.3 交付物，建表语句以 PRD 内为准）
-- 执行方式：按版本顺序手动执行一次，执行后本文件留存作为 DDL 记录，不重复执行。
--           （语句以 PRD 内为准、不含 USE，执行时需指定库：mysql ... jdbc_demo < 本文件）
-- 背景：AuthController / AuthService / AdminInitializer 依赖 app_user 表；
--       密码只存 BCrypt 哈希（password_hash VARCHAR(100)，BCrypt 输出 60 字符）。
-- 执行记录：2026-11-06 已在本机 MySQL（Windows 原生服务 localhost:3306，库 jdbc_demo）执行一次。

CREATE TABLE app_user (
    id            INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'reader',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
