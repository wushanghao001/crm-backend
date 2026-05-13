-- 创建用户会话表
CREATE TABLE IF NOT EXISTS user_session (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT '用户ID',
    session_token TEXT NOT NULL COMMENT '会话Token（存储JWT Token）',
    login_ip VARCHAR(50) COMMENT '登录IP',
    device_info VARCHAR(100) COMMENT '设备信息',
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    last_access_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间',
    status INT DEFAULT 1 COMMENT '状态：1-有效，0-无效',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
