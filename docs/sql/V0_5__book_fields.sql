-- V0.5 · 图书扩展与看板接口（对应 PRD v0.5 交付物）
-- 执行方式：按版本顺序手动执行一次，执行后本文件留存作为 DDL 记录，不重复执行。
--           （不含 USE，执行时需指定库：mysql ... jdbc_demo < 本文件）
-- 背景：详情聚合与 /api/dashboard 依赖新字段；旧数据 category 由 DEFAULT '未分类' 回填（PRD v0.5 强制规范）；
--       total_copies 仅存储展示，禁止副本级借用逻辑（PRD v0.5 禁止项）；逾期口径不变（派生，不落库）。
-- 执行记录：2026-11-06 已在本机 MySQL（Windows 原生服务 localhost:3306，库 jdbc_demo）执行一次。

ALTER TABLE book
    ADD COLUMN category     VARCHAR(50)  NOT NULL DEFAULT '未分类',
    ADD COLUMN publisher    VARCHAR(100) NULL,
    ADD COLUMN isbn         VARCHAR(20)  NULL,
    ADD COLUMN cover_url    VARCHAR(500) NULL,
    ADD COLUMN description  TEXT         NULL,
    ADD COLUMN total_copies INT NOT NULL DEFAULT 1;
