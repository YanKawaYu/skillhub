-- 组织派生命名空间：成员来源分层（synced 同步基线 / manual 人工叠加）。
-- 现有 TEAM 库的人工成员一律视为 manual，reconcile 永不触碰，确保收编现有库（如 szxiuxian）时旧成员不被清除。
ALTER TABLE namespace_member
    ADD COLUMN source VARCHAR(32) NOT NULL DEFAULT 'MANUAL';

CREATE INDEX idx_namespace_member_source ON namespace_member(source);
