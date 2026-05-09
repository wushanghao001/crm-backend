-- 添加月度目标字段到用户表
ALTER TABLE sys_user ADD COLUMN month_target DECIMAL(15,2) DEFAULT NULL COMMENT '月度销售目标' AFTER status;
