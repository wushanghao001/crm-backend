-- 创建用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role_id INT,
    role VARCHAR(20) DEFAULT 'user',
    user_type VARCHAR(20) DEFAULT 'normal' COMMENT 'admin-管理员, normal-普通用户',
    permissions TEXT,
    status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    permissions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建客户表
CREATE TABLE IF NOT EXISTS customer (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(500),
    industry VARCHAR(50),
    scale VARCHAR(20),
    source VARCHAR(50),
    status VARCHAR(20) DEFAULT 'active',
    total_amount DECIMAL(12, 2) DEFAULT 0,
    last_contact_time TIMESTAMP NULL,
    creator_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建联系人表
CREATE TABLE IF NOT EXISTS contact (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    position VARCHAR(50),
    email VARCHAR(100),
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建交互记录表
CREATE TABLE IF NOT EXISTS interaction (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    contact_id INT,
    type VARCHAR(20),
    content TEXT,
    interaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    operator_id INT,
    operator VARCHAR(50),
    status VARCHAR(20) DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    FOREIGN KEY (contact_id) REFERENCES contact(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建订单表
CREATE TABLE IF NOT EXISTS `order` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    product_id INT,
    quantity INT DEFAULT 1,
    unit_price DECIMAL(10, 2),
    total_amount DECIMAL(12, 2),
    status VARCHAR(20) DEFAULT 'pending',
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建销售机会表
CREATE TABLE IF NOT EXISTS opportunity (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    stage VARCHAR(20) DEFAULT 'lead',
    amount DECIMAL(12, 2),
    probability DECIMAL(5, 2),
    expected_close_date TIMESTAMP NULL,
    description TEXT,
    owner_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建服务工单表
CREATE TABLE IF NOT EXISTS service_ticket (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    priority VARCHAR(20) DEFAULT 'normal',
    status VARCHAR(20) DEFAULT 'pending',
    assignee_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建产品表
CREATE TABLE IF NOT EXISTS product (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    code VARCHAR(50) UNIQUE,
    price DECIMAL(10, 2),
    stock INT DEFAULT 0,
    description TEXT,
    status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入默认角色
INSERT IGNORE INTO sys_role (id, name, description, permissions) VALUES 
(1, 'admin', '系统管理员', 'all'),
(2, 'user', '普通用户', 'customer:view,customer:edit,opportunity:view,opportunity:edit,service:view,service:edit');

-- 插入默认管理员用户 (密码: admin123)
INSERT IGNORE INTO sys_user (id, username, password, email, role_id, role, permissions, status) VALUES 
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'admin@crm.com', 1, 'admin', 'all', 1);

-- 插入测试用户 (密码: user123)
INSERT IGNORE INTO sys_user (id, username, password, email, role_id, role, permissions, status) VALUES 
(2, 'user', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'user@crm.com', 2, 'user', 'customer:view,customer:edit,opportunity:view,opportunity:edit,service:view,service:edit', 1);

-- 插入测试客户数据
INSERT IGNORE INTO customer (id, name, phone, email, address, industry, scale, source, status, total_amount) VALUES 
(1, '北京科技有限公司', '13800138001', 'contact@bjtech.com', '北京市海淀区中关村科技园', '科技', '大型', '官网', 'active', 500000.00),
(2, '上海数据科技有限公司', '13800138002', 'contact@shdata.com', '上海市浦东新区张江高科技园区', '数据服务', '中型', '客户推荐', 'active', 320000.00),
(3, '深圳创新科技集团', '13800138003', 'contact@sznova.com', '深圳市南山区科技园', '电子', '大型', '展会', 'active', 880000.00),
(4, '广州智能制造有限公司', '13800138004', 'contact@gzsmart.com', '广州市天河区软件园', '制造业', '中型', '线上广告', 'active', 260000.00),
(5, '杭州电商科技有限公司', '13800138005', 'contact@hzebiz.com', '杭州市西湖区文三路', '电子商务', '小型', 'SEO', 'active', 150000.00);

-- 插入测试产品数据
INSERT IGNORE INTO product (id, name, category, code, price, stock, description) VALUES 
(1, '企业版套餐', '套餐', 'P001', 120000.00, 100, '适合大型企业使用，包含全部功能'),
(2, '专业版套餐', '套餐', 'P002', 68000.00, 200, '适合中型企业使用，包含核心功能'),
(3, '标准版套餐', '套餐', 'P003', 35000.00, 500, '适合小型企业使用，基础功能'),
(4, '高级版套餐', '套餐', 'P004', 85000.00, 150, '适合成长型企业使用，进阶功能');

-- 插入测试销售机会数据
INSERT IGNORE INTO opportunity (id, customer_id, name, stage, amount, probability) VALUES 
(1, 1, '年度续约项目', 'negotiation', 500000.00, 0.7),
(2, 3, '新产品采购', 'proposal', 880000.00, 0.5),
(3, 2, '服务升级', 'qualified', 150000.00, 0.8),
(4, 4, '系统集成项目', 'lead', 320000.00, 0.3);

-- 插入测试服务工单数据
INSERT IGNORE INTO service_ticket (id, customer_id, title, description, priority, status) VALUES
(1, 1, '系统登录问题', '部分用户无法正常登录系统', 'high', 'pending'),
(2, 2, '报表导出异常', 'Excel报表导出格式有问题', 'normal', 'processing'),
(3, 3, '数据同步失败', '客户数据同步不及时', 'high', 'pending'),
(4, 5, '功能咨询', '询问如何使用新的数据分析功能', 'low', 'completed');

-- 创建客户跟进记录表
CREATE TABLE IF NOT EXISTS customer_follow (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    follow_type VARCHAR(20) NOT NULL COMMENT '跟进方式: phone/wechat/email/meeting/sms/video/other',
    follow_result VARCHAR(20) NOT NULL COMMENT '跟进结果: initial_contact/requirement/quotation/negotiation/pending_deal/closed/lost/contact_lost',
    intent_level VARCHAR(10) DEFAULT 'medium' COMMENT '客户意向度: high/medium/low',
    content TEXT COMMENT '跟进内容',
    remark TEXT COMMENT '跟进备注',
    next_follow_time TIMESTAMP NULL COMMENT '下次跟进时间',
    attachment VARCHAR(500) COMMENT '附件地址，多个用逗号分隔',
    remind_flag TINYINT(1) DEFAULT 0 COMMENT '是否提醒',
    follow_user_id INT COMMENT '跟进人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    FOREIGN KEY (follow_user_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建待办任务表
CREATE TABLE IF NOT EXISTS task (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    task_type VARCHAR(20) DEFAULT 'follow' COMMENT '任务类型: follow/other',
    related_customer_id INT COMMENT '关联客户ID',
    related_follow_id INT COMMENT '关联跟进记录ID',
    due_date TIMESTAMP NULL COMMENT '到期时间',
    priority VARCHAR(10) DEFAULT 'medium' COMMENT '优先级: high/medium/low',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/in_progress/completed/cancelled',
    assignee_id INT COMMENT '负责人ID',
    creator_id INT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (related_customer_id) REFERENCES customer(id) ON DELETE SET NULL,
    FOREIGN KEY (assignee_id) REFERENCES sys_user(id) ON DELETE SET NULL,
    FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    operator VARCHAR(50),
    operator_id VARCHAR(50),
    type VARCHAR(50),
    content TEXT,
    ip VARCHAR(50),
    params TEXT,
    result TEXT,
    status INT,
    error_message TEXT,
    module VARCHAR(50),
    target_id INT,
    target_name VARCHAR(200),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 修改客户表，添加跟进相关字段
ALTER TABLE customer ADD COLUMN IF NOT EXISTS last_follow_time TIMESTAMP NULL COMMENT '最近跟进时间' AFTER total_amount;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS follow_count INT DEFAULT 0 COMMENT '跟进次数' AFTER last_follow_time;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS customer_level VARCHAR(10) DEFAULT 'C' COMMENT '客户等级: A/B/C/D' AFTER status;

-- 插入测试跟进数据
INSERT IGNORE INTO customer_follow (id, customer_id, follow_type, follow_result, intent_level, content, next_follow_time, follow_user_id) VALUES
(1, 1, 'phone', 'quotation', 'high', '电话沟通了客户需求，客户表示有明确采购意向，需要提供详细方案', '2026-05-15 10:00:00', 2),
(2, 1, 'meeting', 'negotiation', 'high', '面谈讨论了商务条款，客户对价格比较敏感，需要再协商', '2026-05-20 14:00:00', 2),
(3, 2, 'wechat', 'requirement', 'medium', '微信沟通需求细节，客户表示需要内部讨论后决定', '2026-05-18 09:00:00', 2),
(4, 3, 'email', 'initial_contact', 'low', '发送了公司介绍和产品资料，客户暂无回应', '2026-05-25 10:00:00', 2),
(5, 4, 'phone', 'lost', 'low', '电话无人接听，发送短信也未回复，标记为客户失联', NULL, 2);

-- 插入测试待办任务
INSERT IGNORE INTO task (id, title, content, task_type, related_customer_id, related_follow_id, due_date, priority, status, assignee_id, creator_id) VALUES
(1, '请及时跟进客户：北京科技有限公司', '请及时跟进客户：北京科技有限公司', 'follow', 1, 1, '2026-05-15 10:00:00', 'high', 'pending', 2, 2),
(2, '请及时跟进客户：北京科技有限公司', '请及时跟进客户：北京科技有限公司', 'follow', 1, 2, '2026-05-20 14:00:00', 'high', 'pending', 2, 2),
(3, '请及时跟进客户：上海数据科技有限公司', '请及时跟进客户：上海数据科技有限公司', 'follow', 2, 3, '2026-05-18 09:00:00', 'medium', 'pending', 2, 2);

-- 插入测试操作日志数据
INSERT IGNORE INTO operation_log (id, operator, operator_id, type, content, ip, module, target_id, target_name, status, created_at) VALUES
(1, 'admin', '1', 'create', '创建新用户：user', '192.168.1.100', 'user', 2, 'user', 1, '2026-05-01 09:30:00'),
(2, 'admin', '1', 'create', '创建管理员角色', '192.168.1.100', 'role', 1, 'admin', 1, '2026-05-01 09:35:00'),
(3, 'user', '2', 'create', '添加新客户：北京科技有限公司', '192.168.1.101', 'customer', 1, '北京科技有限公司', 1, '2026-05-02 10:15:00'),
(4, 'user', '2', 'update', '编辑客户信息：更新联系方式', '192.168.1.101', 'customer', 1, '北京科技有限公司', 1, '2026-05-03 14:20:00'),
(5, 'user', '2', 'create', '为客户北京科技有限公司添加联系人张三', '192.168.1.101', 'contact', 1, '张三', 1, '2026-05-03 14:30:00'),
(6, 'user', '2', 'create', '创建销售机会：年度续约项目', '192.168.1.101', 'opportunity', 1, '年度续约项目', 1, '2026-05-04 11:00:00'),
(7, 'user', '2', 'create', '创建服务工单：系统登录问题', '192.168.1.101', 'service', 1, '系统登录问题', 1, '2026-05-05 15:45:00'),
(8, 'admin', '1', 'create', '添加新产品：企业版套餐', '192.168.1.100', 'product', 1, '企业版套餐', 1, '2026-05-06 09:00:00'),
(9, 'user', '2', 'delete', '删除客户：杭州电商科技有限公司', '192.168.1.101', 'customer', 5, '杭州电商科技有限公司', 1, '2026-05-06 16:20:00'),
(10, 'user', '2', 'create', '添加客户交互记录：电话沟通', '192.168.1.101', 'interaction', 1, '电话沟通', 1, '2026-05-07 10:30:00'),
(11, 'admin', '1', 'update', '将用户user状态设为禁用', '192.168.1.100', 'user', 2, 'user', 1, '2026-05-07 11:00:00'),
(12, 'admin', '1', 'update', '将用户user状态设为启用', '192.168.1.100', 'user', 2, 'user', 1, '2026-05-07 11:05:00'),
(13, 'user', '2', 'create', '添加客户跟进记录：电话跟进', '192.168.1.101', 'customer_follow', 1, '电话跟进', 1, '2026-05-08 09:15:00'),
(14, 'user', '2', 'update', '销售阶段从proposal更新为negotiation', '192.168.1.101', 'opportunity', 1, '年度续约项目', 1, '2026-05-08 14:30:00'),
(15, 'admin', '1', 'update', '为user角色分配客户查看权限', '192.168.1.100', 'role', 2, 'user', 1, '2026-05-08 16:00:00');
