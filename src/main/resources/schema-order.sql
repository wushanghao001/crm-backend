-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '订单编号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `customer_name` VARCHAR(255) NOT NULL COMMENT '客户名称',
  `contact_name` VARCHAR(100) COMMENT '联系人',
  `phone` VARCHAR(20) COMMENT '联系电话',
  `email` VARCHAR(100) COMMENT '客户邮箱',
  `total_amount` DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '优惠金额',
  `paid_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '实付金额',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '订单状态：pending-待付款，paid-已付款，completed-已完成，cancelled-已取消',
  `pay_status` VARCHAR(20) DEFAULT 'unpaid' COMMENT '支付状态：unpaid-未支付，paid-已支付，refunding-退款中，refunded-已退款',
  `payment_method` VARCHAR(50) COMMENT '支付方式',
  `paid_at` DATETIME COMMENT '支付时间',
  `transaction_no` VARCHAR(64) COMMENT '交易流水号',
  `remark` TEXT COMMENT '订单备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_customer_id` (`customer_id`),
  INDEX `idx_order_no` (`order_no`),
  INDEX `idx_status` (`status`),
  INDEX `idx_pay_status` (`pay_status`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单商品表
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `product_id` BIGINT COMMENT '商品ID',
  `product_name` VARCHAR(255) NOT NULL COMMENT '商品名称',
  `product_code` VARCHAR(64) COMMENT '商品编码',
  `project_code_id` BIGINT COMMENT '项目号ID',
  `project_code_name` VARCHAR(100) COMMENT '项目号名称',
  `material_code_id` BIGINT COMMENT '料号ID',
  `material_code_name` VARCHAR(100) COMMENT '料号名称',
  `brand_code_id` BIGINT COMMENT '牌号ID',
  `brand_code_name` VARCHAR(100) COMMENT '牌号名称',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `subtotal` DECIMAL(12,2) NOT NULL COMMENT '小计金额',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_order_id` (`order_id`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `order`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单商品表';

-- 插入测试数据
INSERT INTO `order` (`order_no`, `customer_id`, `customer_name`, `contact_name`, `phone`, `email`, `total_amount`, `discount_amount`, `paid_amount`, `status`, `pay_status`, `payment_method`, `paid_at`, `transaction_no`, `remark`, `created_at`, `updated_at`) VALUES
('ORD20240115001', 1, '北京科技有限公司', '张先生', '13800138001', 'zhang@example.com', 12800.00, 500.00, 12300.00, 'completed', 'paid', '支付宝', '2024-01-15 10:30:00', '202401152837465', '企业版年度套餐', '2024-01-15 10:20:00', '2024-01-15 14:00:00'),
('ORD20240114002', 2, '上海贸易集团', '李女士', '13900139002', 'li@example.com', 8600.00, 0.00, 8600.00, 'completed', 'paid', '银行转账', '2024-01-14 15:45:00', 'TR20240114782635', '', '2024-01-14 14:30:00', '2024-01-14 16:00:00'),
('ORD20240113003', 3, '广州制造公司', '王经理', '13700137003', 'wang@example.com', 3200.00, 200.00, 3000.00, 'paid', 'paid', '微信支付', '2024-01-13 09:20:00', 'WX20240113987654', '季度套餐', '2024-01-13 09:00:00', '2024-01-13 09:30:00'),
('ORD20240112004', 4, '深圳科技有限公司', '陈总', '13600136004', 'chen@example.com', 12800.00, 0.00, 0.00, 'pending', 'unpaid', '', NULL, '', '等待财务审批', '2024-01-12 16:45:00', '2024-01-12 16:45:00'),
('ORD20240111005', 5, '杭州互联网公司', '赵主管', '13500135005', 'zhao@example.com', 8600.00, 0.00, 8600.00, 'cancelled', 'refunded', '支付宝', '2024-01-11 11:00:00', '202401119876543', '客户取消订单', '2024-01-11 10:30:00', '2024-01-11 14:00:00'),
('ORD20240110006', 1, '北京科技有限公司', '张先生', '13800138001', 'zhang@example.com', 5800.00, 300.00, 5500.00, 'paid', 'paid', '微信支付', '2024-01-10 14:20:00', 'WX20240110567890', '追加购买', '2024-01-10 14:00:00', '2024-01-10 14:30:00'),
('ORD20240109007', 6, '成都软件公司', '刘经理', '13400134006', 'liu@example.com', 15800.00, 800.00, 15000.00, 'completed', 'paid', '银行转账', '2024-01-09 16:00:00', 'TR20240109123456', '企业版升级', '2024-01-09 15:30:00', '2024-01-09 17:00:00'),
('ORD20240108008', 7, '武汉制造业集团', '孙总', '13300133007', 'sun@example.com', 26800.00, 1800.00, 25000.00, 'pending', 'unpaid', '', NULL, '', '年度采购计划', '2024-01-08 09:00:00', '2024-01-08 09:00:00');

INSERT INTO `order_item` (`order_id`, `product_name`, `product_code`, `unit_price`, `quantity`, `subtotal`, `created_at`) VALUES
(1, '企业版套餐', 'PROD001', 12800.00, 1, 12800.00, '2024-01-15 10:20:00'),
(2, '专业版套餐', 'PROD002', 8600.00, 1, 8600.00, '2024-01-14 14:30:00'),
(3, '标准版套餐', 'PROD003', 3200.00, 1, 3200.00, '2024-01-13 09:00:00'),
(4, '企业版套餐', 'PROD001', 12800.00, 1, 12800.00, '2024-01-12 16:45:00'),
(5, '专业版套餐', 'PROD002', 8600.00, 1, 8600.00, '2024-01-11 10:30:00'),
(6, '增值服务包', 'PROD004', 5800.00, 1, 5800.00, '2024-01-10 14:00:00'),
(7, '企业版套餐', 'PROD001', 12800.00, 1, 12800.00, '2024-01-09 15:30:00'),
(7, '数据迁移服务', 'PROD005', 3000.00, 1, 3000.00, '2024-01-09 15:30:00'),
(8, '企业版套餐', 'PROD001', 12800.00, 2, 25600.00, '2024-01-08 09:00:00'),
(8, '年度维护服务', 'PROD006', 1200.00, 1, 1200.00, '2024-01-08 09:00:00');