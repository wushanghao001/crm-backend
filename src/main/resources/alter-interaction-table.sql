-- 修复interaction表结构，添加缺失的字段

-- 检查并添加contact_id字段
ALTER TABLE interaction 
ADD COLUMN IF NOT EXISTS contact_id INT NULL,
ADD COLUMN IF NOT EXISTS operator VARCHAR(50) NULL,
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'completed';

-- 添加外键约束（如果不存在）
-- 注意：如果外键已存在，这可能会报错，但字段添加会成功