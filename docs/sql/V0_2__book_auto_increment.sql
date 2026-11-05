-- V0.2 · book 主键自增（对应 PRD v0.2 交付物）
-- 执行方式：按版本顺序手动执行一次，执行后本文件留存作为 DDL 记录，不重复执行。
-- 背景：AddBookRequest / UpdateBookRequest 移除 id 字段后，POST /api/books 依赖数据库自增生成主键。
-- 前提：book 表已存在（init.sql 建表并含 5 条示例数据，id 1~5），本语句保留全部存量数据。
-- 执行记录：2026-11-06 已在本机 MySQL（Windows 原生服务 localhost:3306，库 jdbc_demo）执行一次；
--           同批经用户确认 TRUNCATE 清除测试残留并重灌 init.sql 示例数据（id 1~5 完好）。

USE jdbc_demo;

ALTER TABLE book MODIFY id INT NOT NULL AUTO_INCREMENT;
