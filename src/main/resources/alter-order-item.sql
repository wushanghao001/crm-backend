-- 为订单商品表添加缺失的字段
ALTER TABLE `order_item`
  ADD COLUMN `product_id` BIGINT COMMENT '商品ID' AFTER `order_id`,
  ADD COLUMN `project_code_id` BIGINT COMMENT '项目号ID' AFTER `product_code`,
  ADD COLUMN `project_code_name` VARCHAR(100) COMMENT '项目号名称' AFTER `project_code_id`,
  ADD COLUMN `material_code_id` BIGINT COMMENT '料号ID' AFTER `project_code_name`,
  ADD COLUMN `material_code_name` VARCHAR(100) COMMENT '料号名称' AFTER `material_code_id`,
  ADD COLUMN `brand_code_id` BIGINT COMMENT '牌号ID' AFTER `material_code_name`,
  ADD COLUMN `brand_code_name` VARCHAR(100) COMMENT '牌号名称' AFTER `brand_code_id`;