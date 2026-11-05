-- 创建数据库（如果不存在），不影响已有的 fastapi_demo 库
CREATE DATABASE IF NOT EXISTS jdbc_demo DEFAULT CHARSET=utf8mb4;

USE jdbc_demo;

-- 建表（如果已存在不会删表、不丢数据）
CREATE TABLE IF NOT EXISTS book (
    id          INT          NOT NULL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    author      VARCHAR(100) NOT NULL,
    is_borrowed TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入示例数据（id 已存在则什么都不做，不覆盖现有数据）
INSERT INTO book (id, title, author, is_borrowed) VALUES
    (1, '三体', '刘慈欣', 0),
    (2, '活着', '余华', 0),
    (3, '百年孤独', '加西亚·马尔克斯', 0),
    (4, '围城', '钱钟书', 0),
    (5, '平凡的世界', '路遥', 0)
ON DUPLICATE KEY UPDATE id = id;
