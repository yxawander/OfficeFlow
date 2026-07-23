CREATE DATABASE IF NOT EXISTS officeflow
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

SET NAMES utf8mb4;
USE officeflow;

-- =========================================================
-- 1. 用户、组织、RBAC 权限模块：user-service
-- =========================================================

CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示根部门',
    dept_name VARCHAR(64) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(64) NOT NULL COMMENT '部门编码',
    leader_id BIGINT DEFAULT NULL COMMENT '部门负责人用户ID',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(128) DEFAULT NULL COMMENT '部门邮箱',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    created_by BIGINT DEFAULT NULL,
    updated_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_dept_code (dept_code),
    KEY idx_sys_dept_parent (parent_id),
    KEY idx_sys_dept_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE IF NOT EXISTS sys_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '岗位ID',
    post_name VARCHAR(64) NOT NULL COMMENT '岗位名称',
    post_code VARCHAR(64) NOT NULL COMMENT '岗位编码',
    base_salary DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '岗位基础薪资',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    created_by BIGINT DEFAULT NULL,
    updated_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_post_code (post_code),
    KEY idx_sys_post_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(128) NOT NULL COMMENT '登录密码，开发阶段可先明文，后续改BCrypt',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    gender TINYINT DEFAULT 0 COMMENT '性别：0未知，1男，2女',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
    post_id BIGINT DEFAULT NULL COMMENT '岗位ID',
    manager_id BIGINT DEFAULT NULL COMMENT '直属领导用户ID，用于一级审批',
    hire_date DATE DEFAULT NULL COMMENT '入职日期',
    user_type VARCHAR(32) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '用户类型：ADMIN/EMPLOYEE/MANAGER',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    created_by BIGINT DEFAULT NULL,
    updated_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_phone (phone),
    KEY idx_sys_user_dept (dept_id),
    KEY idx_sys_user_post (post_id),
    KEY idx_sys_user_manager (manager_id),
    KEY idx_sys_user_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    created_by BIGINT DEFAULT NULL,
    updated_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_role_code (role_code),
    KEY idx_sys_role_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    menu_type VARCHAR(16) NOT NULL DEFAULT 'MENU' COMMENT '菜单类型：CATALOG/MENU/BUTTON',
    path VARCHAR(128) DEFAULT NULL COMMENT '前端路由路径',
    component VARCHAR(128) DEFAULT NULL COMMENT '前端组件路径',
    permission VARCHAR(128) DEFAULT NULL COMMENT '前端按钮权限标识',
    icon VARCHAR(64) DEFAULT NULL COMMENT '图标',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：1显示，0隐藏',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    created_by BIGINT DEFAULT NULL,
    updated_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sys_menu_parent (parent_id),
    KEY idx_sys_menu_permission (permission),
    KEY idx_sys_menu_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

CREATE TABLE IF NOT EXISTS sys_api_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '接口权限ID',
    permission_name VARCHAR(64) NOT NULL COMMENT '接口权限名称',
    permission_code VARCHAR(128) NOT NULL COMMENT '接口权限编码',
    service_name VARCHAR(64) NOT NULL COMMENT '所属服务',
    request_method VARCHAR(16) NOT NULL COMMENT 'HTTP方法',
    request_path VARCHAR(255) NOT NULL COMMENT '接口路径',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_api_permission_code (permission_code),
    KEY idx_sys_api_permission_service (service_name),
    KEY idx_sys_api_permission_path (request_method, request_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_sys_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_role_api_permission (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    api_permission_id BIGINT NOT NULL COMMENT '接口权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, api_permission_id),
    KEY idx_sys_role_api_permission_api (api_permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色接口权限关联表';

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键',
    config_value VARCHAR(512) NOT NULL COMMENT '配置值',
    config_group VARCHAR(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '配置分组',
    remark VARCHAR(255) DEFAULT NULL COMMENT '说明',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表，本地兜底配置，主要配置仍放Nacos';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '登录日志ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    login_ip VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '浏览器UA',
    login_status VARCHAR(32) NOT NULL COMMENT '登录状态：SUCCESS/FAIL',
    message VARCHAR(255) DEFAULT NULL COMMENT '登录结果说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sys_login_log_user (user_id),
    KEY idx_sys_login_log_username (username),
    KEY idx_sys_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作日志ID',
    user_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '操作账号',
    module_name VARCHAR(64) NOT NULL COMMENT '业务模块',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/APPROVE/EXPORT',
    request_method VARCHAR(16) DEFAULT NULL COMMENT 'HTTP方法',
    request_path VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
    request_params TEXT DEFAULT NULL COMMENT '请求参数',
    response_code INT DEFAULT NULL COMMENT '响应码',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：1成功，0失败',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    cost_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    ip VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sys_operation_log_user (user_id),
    KEY idx_sys_operation_log_module (module_name),
    KEY idx_sys_operation_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基础操作日志表';

CREATE TABLE IF NOT EXISTS sys_user_salary (
    user_id BIGINT PRIMARY KEY COMMENT '员工ID',
    base_salary DECIMAL(10,2) NOT NULL COMMENT '基本工资',
    allowance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '补贴/津贴',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工薪资档案';

CREATE TABLE IF NOT EXISTS salary_monthly_statement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '员工ID',
    settle_month CHAR(7) NOT NULL COMMENT '结算月份 YYYY-MM',
    base_salary DECIMAL(10,2) NOT NULL COMMENT '基本工资',
    overtime_pay DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '加班费',
    allowance DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '各种津贴',
    late_deduction DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '迟到早退扣款',
    absent_deduction DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '旷工扣款',
    leave_deduction DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '请假扣款',
    actual_salary DECIMAL(10,2) NOT NULL COMMENT '实发工资',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT, PUBLISHED',
    daily_wage DECIMAL(10,2) COMMENT '日薪',
    hourly_wage DECIMAL(10,2) COMMENT '时薪',
    overtime_hours DECIMAL(5,1) COMMENT '加班小时数',
    off_work_hours DECIMAL(5,1) COMMENT '迟到/早退/缺卡折算扣除总小时数',
    absent_days DECIMAL(5,1) COMMENT '旷工天数',
    leave_days DECIMAL(5,1) COMMENT '请假天数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_salary_statement_user_month (user_id, settle_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度工资结算表';

-- =========================================================
-- 2. 考勤模块：attendance-service
-- =========================================================

CREATE TABLE IF NOT EXISTS attendance_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤规则ID',
    rule_name VARCHAR(64) NOT NULL COMMENT '规则名称',
    work_start_time TIME NOT NULL DEFAULT '09:00:00' COMMENT '上班时间',
    work_end_time TIME NOT NULL DEFAULT '18:00:00' COMMENT '下班时间',
    late_threshold_minutes INT NOT NULL DEFAULT 10 COMMENT '迟到阈值分钟',
    early_leave_threshold_minutes INT NOT NULL DEFAULT 10 COMMENT '早退阈值分钟',
    absent_threshold_minutes INT NOT NULL DEFAULT 240 COMMENT '旷工阈值分钟',
    check_lock_seconds INT NOT NULL DEFAULT 10 COMMENT '打卡分布式锁过期秒数',
    location_required TINYINT NOT NULL DEFAULT 0 COMMENT '是否强制定位打卡：1是，0否',
    office_location_name VARCHAR(100) NOT NULL DEFAULT '默认办公点' COMMENT '办公地点名称',
    office_address VARCHAR(255) DEFAULT NULL COMMENT '办公地点地址',
    office_latitude DECIMAL(10,7) DEFAULT NULL COMMENT '办公地点纬度',
    office_longitude DECIMAL(10,7) DEFAULT NULL COMMENT '办公地点经度',
    allowed_radius_meters INT NOT NULL DEFAULT 1000 COMMENT '允许打卡半径，单位米',
    accuracy_threshold_meters INT NOT NULL DEFAULT 1000 COMMENT '定位精度阈值，单位米',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_attendance_rule_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤规则表';

CREATE TABLE IF NOT EXISTS attendance_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤组ID',
    group_name VARCHAR(64) NOT NULL COMMENT '考勤组名称',
    rule_id BIGINT NOT NULL COMMENT '考勤规则ID',
    dept_id BIGINT DEFAULT NULL COMMENT '绑定部门ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_attendance_group_rule (rule_id),
    KEY idx_attendance_group_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组表';

CREATE TABLE IF NOT EXISTS attendance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤记录ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    dept_id BIGINT DEFAULT NULL COMMENT '打卡时所属部门ID',
    work_date DATE NOT NULL COMMENT '考勤日期',
    check_in_time DATETIME DEFAULT NULL COMMENT '上班打卡时间',
    check_in_ip VARCHAR(64) DEFAULT NULL COMMENT '上班打卡IP',
    check_in_remark VARCHAR(255) DEFAULT NULL COMMENT '上班打卡备注',
    check_in_latitude DECIMAL(10,7) DEFAULT NULL COMMENT '上班打卡纬度',
    check_in_longitude DECIMAL(10,7) DEFAULT NULL COMMENT '上班打卡经度',
    check_in_accuracy_meters DECIMAL(10,2) DEFAULT NULL COMMENT '上班打卡定位精度，单位米',
    check_in_distance_meters INT DEFAULT NULL COMMENT '上班打卡距离办公点距离，单位米',
    check_in_location_name VARCHAR(100) DEFAULT NULL COMMENT '上班打卡命中办公地点',
    check_out_time DATETIME DEFAULT NULL COMMENT '下班打卡时间',
    check_out_ip VARCHAR(64) DEFAULT NULL COMMENT '下班打卡IP',
    check_out_remark VARCHAR(255) DEFAULT NULL COMMENT '下班打卡备注',
    check_out_latitude DECIMAL(10,7) DEFAULT NULL COMMENT '下班打卡纬度',
    check_out_longitude DECIMAL(10,7) DEFAULT NULL COMMENT '下班打卡经度',
    check_out_accuracy_meters DECIMAL(10,2) DEFAULT NULL COMMENT '下班打卡定位精度，单位米',
    check_out_distance_meters INT DEFAULT NULL COMMENT '下班打卡距离办公点距离，单位米',
    check_out_location_name VARCHAR(100) DEFAULT NULL COMMENT '下班打卡命中办公地点',
    work_minutes INT NOT NULL DEFAULT 0 COMMENT '实际工作分钟数',
    late_minutes INT NOT NULL DEFAULT 0 COMMENT '迟到分钟数',
    early_leave_minutes INT NOT NULL DEFAULT 0 COMMENT '早退分钟数',
    overtime_minutes INT NOT NULL DEFAULT 0 COMMENT '加班分钟数',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL(正常打卡), RECHECKED(已补卡), ON_LEAVE(休假中), LATE(迟到), EARLY_LEAVE(早退), LATE_AND_EARLY(迟到且早退), ABSENT(旷工), MISSING_CARD(缺卡)',
    source VARCHAR(32) NOT NULL DEFAULT 'USER_CHECK' COMMENT '来源：USER_CHECK/MANUAL/RECALCULATE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_attendance_record_user_date (user_id, work_date),
    KEY idx_attendance_record_dept_date (dept_id, work_date),
    KEY idx_attendance_record_status (status),
    KEY idx_attendance_record_work_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日考勤记录表';

CREATE TABLE IF NOT EXISTS attendance_correction_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '补卡申请ID',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    attendance_record_id BIGINT DEFAULT NULL COMMENT '关联考勤记录ID',
    correction_type VARCHAR(32) NOT NULL COMMENT '补卡类型：CHECK_IN/CHECK_OUT',
    correction_time DATETIME NOT NULL COMMENT '申请补卡时间',
    reason VARCHAR(500) NOT NULL COMMENT '补卡原因',
    flow_apply_id BIGINT DEFAULT NULL COMMENT '关联审批单ID',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/CANCELED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_attendance_correction_user (user_id),
    KEY idx_attendance_correction_flow (flow_apply_id),
    KEY idx_attendance_correction_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表，作为拓展审批场景预留';

CREATE TABLE IF NOT EXISTS attendance_monthly_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '月度考勤报表ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
    report_month CHAR(7) NOT NULL COMMENT '统计月份，格式YYYY-MM',
    should_work_days INT NOT NULL DEFAULT 0 COMMENT '应出勤天数',
    actual_work_days INT NOT NULL DEFAULT 0 COMMENT '实际出勤天数',
    late_count INT NOT NULL DEFAULT 0 COMMENT '迟到次数',
    early_leave_count INT NOT NULL DEFAULT 0 COMMENT '早退次数',
    absent_count INT NOT NULL DEFAULT 0 COMMENT '旷工次数',
    missing_card_count INT NOT NULL DEFAULT 0 COMMENT '缺卡次数',
    leave_days DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '请假天数',
    overtime_hours DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '加班小时',
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计生成时间',
    UNIQUE KEY uk_attendance_monthly_user_month (user_id, report_month),
    KEY idx_attendance_monthly_dept_month (dept_id, report_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度考勤统计表，用于大屏和Excel导出';

-- =========================================================
-- 3. 审批模块：flow-service
-- =========================================================

CREATE TABLE IF NOT EXISTS flow_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批单ID',
    apply_no VARCHAR(64) NOT NULL COMMENT '申请单号',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    applicant_dept_id BIGINT DEFAULT NULL COMMENT '申请人部门ID',
    approver_id BIGINT NOT NULL COMMENT '直属审批人ID',
    apply_type VARCHAR(32) NOT NULL COMMENT '申请类型：LEAVE/OVERTIME/CORRECTION',
    title VARCHAR(128) NOT NULL COMMENT '申请标题',
    reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    duration_hours DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '时长小时',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/CANCELED',
    current_node VARCHAR(64) DEFAULT 'DIRECT_MANAGER' COMMENT '当前节点',
    approved_at DATETIME DEFAULT NULL COMMENT '最终审批时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_flow_apply_no (apply_no),
    KEY idx_flow_apply_applicant (applicant_id),
    KEY idx_flow_apply_approver_status (approver_id, status),
    KEY idx_flow_apply_type_status (apply_type, status),
    KEY idx_flow_apply_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批申请主表';

CREATE TABLE IF NOT EXISTS flow_approve_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批记录ID',
    flow_apply_id BIGINT NOT NULL COMMENT '审批单ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    action VARCHAR(32) NOT NULL COMMENT '动作：APPROVE/REJECT/CANCEL/SUBMIT',
    comment VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    approved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_flow_approve_record_apply (flow_apply_id),
    KEY idx_flow_approve_record_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流转记录表';

CREATE TABLE IF NOT EXISTS flow_cc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '抄送ID',
    flow_apply_id BIGINT NOT NULL COMMENT '审批单ID',
    user_id BIGINT NOT NULL COMMENT '抄送人ID',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读，1已读',
    read_at DATETIME DEFAULT NULL COMMENT '阅读时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_flow_cc_apply_user (flow_apply_id, user_id),
    KEY idx_flow_cc_user_read (user_id, read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批抄送表，拓展工作流预留';

CREATE TABLE IF NOT EXISTS flow_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    flow_apply_id BIGINT NOT NULL COMMENT '审批单ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_url VARCHAR(500) NOT NULL COMMENT '文件访问地址',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小字节',
    file_type VARCHAR(64) DEFAULT NULL COMMENT '文件MIME类型，如 image/png, application/pdf',
    uploaded_by BIGINT NOT NULL COMMENT '上传人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_flow_attachment_apply (flow_apply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批附件表，拓展上传材料预留';

-- =========================================================
-- 4. 公告通知模块：notice-service
-- =========================================================

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title VARCHAR(128) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    notice_type VARCHAR(32) NOT NULL DEFAULT 'COMPANY' COMMENT '类型：COMPANY/DEPT/SYSTEM',
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：NORMAL/IMPORTANT/URGENT',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publisher_name VARCHAR(64) DEFAULT NULL COMMENT '发布人姓名缓存',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
    read_count INT NOT NULL DEFAULT 0 COMMENT '已读人数',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数（每次查看详情+1）',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_notice_status_publish (status, publish_time),
    KEY idx_notice_publisher (publisher_id),
    FULLTEXT KEY ft_notice_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

CREATE TABLE IF NOT EXISTS notice_scope (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告可见范围ID',
    notice_id BIGINT NOT NULL COMMENT '公告ID',
    scope_type VARCHAR(32) NOT NULL COMMENT '范围类型：ALL/DEPT/USER/ROLE',
    scope_id BIGINT DEFAULT NULL COMMENT '范围ID，ALL时为空',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notice_scope_notice (notice_id),
    KEY idx_notice_scope_type_id (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告可见范围表';

-- ======================================
-- 优化后的 notice_read 表
-- ======================================
CREATE TABLE IF NOT EXISTS notice_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '阅读记录ID',
    notice_id BIGINT NOT NULL COMMENT '公告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读，1已读',
    read_at DATETIME DEFAULT NULL COMMENT '阅读时间',
    read_ip VARCHAR(64) DEFAULT NULL COMMENT '阅读IP',
    read_duration_seconds INT DEFAULT 0 COMMENT '阅读时长秒数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notice_read_notice_user (notice_id, user_id),
    KEY idx_notice_read_user (user_id),
    KEY idx_notice_read_at (read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告阅读记录表（优化版）';


-- =========================================================
-- 5. 数据大屏 / 报表模块：report-service
-- =========================================================

CREATE TABLE IF NOT EXISTS report_dashboard_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '快照ID',
    snapshot_date DATE NOT NULL COMMENT '快照日期',
    total_user_count INT NOT NULL DEFAULT 0 COMMENT '员工总数',
    active_user_count INT NOT NULL DEFAULT 0 COMMENT '启用员工数',
    today_check_count INT NOT NULL DEFAULT 0 COMMENT '今日打卡人数',
    today_late_count INT NOT NULL DEFAULT 0 COMMENT '今日迟到人数',
    pending_flow_count INT NOT NULL DEFAULT 0 COMMENT '待审批数量',
    published_notice_count INT NOT NULL DEFAULT 0 COMMENT '已发布公告数量',
    notice_read_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '公告阅读率',
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    UNIQUE KEY uk_report_dashboard_snapshot_date (snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页数据大屏快照表';

-- =========================================================
-- 6. 初始化测试数据
-- =========================================================

INSERT IGNORE INTO sys_dept (id, parent_id, dept_name, dept_code, leader_id, sort_order, status)
VALUES
    (1, 0, '总经办', 'HQ', 1, 1, 1),
    (2, 1, '研发部', 'RD', 2, 2, 1),
    (3, 1, '人事部', 'HR', 3, 3, 1),
    (4, 1, '行政部', 'ADMIN_DEPT', 4, 4, 1);

INSERT IGNORE INTO sys_post (id, post_name, post_code, base_salary, sort_order, status)
VALUES
    (1, '系统管理员', 'SYS_ADMIN', 15000.00, 1, 1),
    (2, '部门主管', 'DEPT_MANAGER', 25000.00, 2, 1),
    (3, '后端工程师', 'BACKEND_DEV', 18000.00, 3, 1),
    (4, '前端工程师', 'FRONTEND_DEV', 16000.00, 4, 1),
    (5, '人事专员', 'HR_SPECIALIST', 10000.00, 5, 1);

INSERT IGNORE INTO sys_user (id, username, password, real_name, gender, phone, email, dept_id, post_id, manager_id, hire_date, user_type, status)
VALUES
    (1, 'admin', '123456', '系统管理员', 0, '13800000001', 'admin@officeflow.local', 1, 1, NULL, '2026-07-01', 'ADMIN', 1),
    (2, 'manager', '123456', '研发主管', 1, '13800000002', 'manager@officeflow.local', 2, 2, 1, '2026-07-01', 'MANAGER', 1),
    (3, 'hr', '123456', '人事专员', 2, '13800000003', 'hr@officeflow.local', 3, 5, 1, '2026-07-01', 'EMPLOYEE', 1),
    (4, 'employee', '123456', '普通员工', 1, '13800000004', 'employee@officeflow.local', 2, 3, 2, '2026-07-01', 'EMPLOYEE', 1),
    (5, 'hr_manager', '123456', '人事主管', 2, '13800000005', 'hrmanager@officeflow.local', 3, 2, 1, '2026-07-01', 'MANAGER', 1),
    (6, 'admin_manager', '123456', '行政主管', 1, '13800000006', 'adminmanager@officeflow.local', 4, 2, 1, '2026-07-01', 'MANAGER', 1),
    (7, 'dev_zhang', '123456', '张伟(后端)', 1, '13800000007', 'zhangwei@officeflow.local', 2, 3, 2, '2026-07-01', 'EMPLOYEE', 1),
    (8, 'dev_li', '123456', '李娜(前端)', 2, '13800000008', 'lina@officeflow.local', 2, 4, 2, '2026-07-01', 'EMPLOYEE', 1),
    (9, 'hr_wang', '123456', '王芳(人事)', 2, '13800000009', 'wangfang@officeflow.local', 3, 5, 5, '2026-07-01', 'EMPLOYEE', 1),
    (10, 'admin_zhao', '123456', '赵强(行政)', 1, '13800000010', 'zhaoqiang@officeflow.local', 4, 5, 6, '2026-07-01', 'EMPLOYEE', 1);

INSERT IGNORE INTO sys_role (id, role_name, role_code, data_scope, sort_order, status)
VALUES
    (1, '系统管理员', 'ADMIN', 'ALL', 1, 1),
    (2, '部门主管', 'MANAGER', 'DEPT_AND_CHILD', 2, 1),
    (3, '普通员工', 'EMPLOYEE', 'SELF', 3, 1);

INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, visible, sort_order, status)
VALUES
    (1, 0, '数据大屏', 'MENU', '/dashboard', 'views/dashboard/DashboardView.vue', 'dashboard:view', 'DataAnalysis', 1, 1, 1),
    (2, 0, '系统管理', 'CATALOG', '/system', NULL, 'system:view', 'Setting', 1, 2, 1),
    (3, 2, '员工管理', 'MENU', '/system/users', 'views/system/UserView.vue', 'system:user:view', 'User', 1, 1, 1),
    (4, 2, '角色权限', 'MENU', '/system/roles', 'views/system/RoleView.vue', 'system:role:view', 'Lock', 1, 2, 1),
    (5, 0, '考勤打卡', 'CATALOG', '/attendance', NULL, 'attendance:view', 'Calendar', 1, 3, 1),
    (6, 0, '审批中心', 'MENU', '/flow', 'views/flow/FlowView.vue', 'flow:view', 'Tickets', 1, 4, 1),
    (7, 0, '公告通知', 'MENU', '/notice', 'views/notice/NoticeView.vue', 'notice:view', 'Bell', 1, 5, 1),
    (8, 3, '新增员工', 'BUTTON', NULL, NULL, 'system:user:create', NULL, 0, 1, 1),
    (9, 3, '编辑员工', 'BUTTON', NULL, NULL, 'system:user:update', NULL, 0, 2, 1),
    (10, 3, '删除员工', 'BUTTON', NULL, NULL, 'system:user:delete', NULL, 0, 3, 1),
    (11, 6, '审批同意', 'BUTTON', NULL, NULL, 'flow:approve', NULL, 0, 1, 1),
    (12, 6, '审批驳回', 'BUTTON', NULL, NULL, 'flow:reject', NULL, 0, 2, 1),
    (13, 0, '月度报表', 'MENU', '/report', 'views/report/ReportView.vue', 'report:view', 'DataAnalysis', 1, 6, 1),
    (14, 0, '工资结算', 'MENU', '/salary', 'views/salary/SalaryView.vue', 'salary:view', 'Money', 1, 7, 1),
    (15, 0, 'AI 问答', 'MENU', '/ai-chat', 'views/ai/AiChatView.vue', 'ai:view', 'ChatDotRound', 1, 8, 1),
    (16, 5, '个人打卡工作台', 'MENU', '/attendance/my', 'views/attendance/AttendanceView.vue', 'attendance:view', 'Clock', 1, 1, 1),
    (17, 5, '部门今日考勤实时监控', 'MENU', '/attendance/dept', 'views/attendance/AttendanceView.vue', 'attendance:dept:view', 'DataAnalysis', 1, 2, 1),
    (18, 5, '考勤规则与部门绑定', 'MENU', '/attendance/rule', 'views/attendance/AttendanceView.vue', 'attendance:rule:view', 'Setting', 1, 3, 1);

INSERT IGNORE INTO sys_api_permission (id, permission_name, permission_code, service_name, request_method, request_path, status)
VALUES
    (1, '用户登录', 'api:user:login', 'user-service', 'POST', '/api/user/login', 1),
    (2, '员工查询', 'api:user:list', 'user-service', 'GET', '/api/user/**', 1),
    (3, '员工维护', 'api:user:write', 'user-service', 'POST', '/api/user/**', 1),
    (4, '考勤查询', 'api:attendance:list', 'attendance-service', 'GET', '/api/attendance/**', 1),
    (5, '上班打卡', 'api:attendance:checkin', 'attendance-service', 'POST', '/api/attendance/check-in', 1),
    (6, '审批查询', 'api:flow:list', 'flow-service', 'GET', '/api/flow/**', 1),
    (7, '审批处理', 'api:flow:write', 'flow-service', 'POST', '/api/flow/**', 1),
    (8, '公告查询', 'api:notice:list', 'notice-service', 'GET', '/api/notice/**', 1),
    (9, '公告发布', 'api:notice:write', 'notice-service', 'POST', '/api/notice/**', 1),
    (10, '报表查询', 'api:report:list', 'report-service', 'GET', '/api/report/**', 1),
    (11, '考勤规则维护', 'api:attendance:rule:write', 'attendance-service', 'PUT', '/api/attendance/rules/**', 1),
    (12, '考勤规则创建', 'api:attendance:rule:create', 'attendance-service', 'POST', '/api/attendance/rules', 1),
    (13, '考勤组维护', 'api:attendance:group:write', 'attendance-service', 'PUT', '/api/attendance/groups/**', 1),
    (14, '考勤组创建', 'api:attendance:group:create', 'attendance-service', 'POST', '/api/attendance/groups', 1),
    (15, '下班打卡', 'api:attendance:checkout', 'attendance-service', 'POST', '/api/attendance/check-out', 1),
    (16, '补卡申请', 'api:attendance:recheck', 'attendance-service', 'POST', '/api/attendance/recheck', 1),
    (17, '员工维护', 'api:user:update', 'user-service', 'PUT', '/api/user/**', 1),
    (18, '员工删除', 'api:user:delete', 'user-service', 'DELETE', '/api/user/**', 1),
    (19, '提交审批申请', 'api:flow:apply:create', 'flow-service', 'POST', '/api/flow/applies', 1),
    (20, '维护本人审批申请', 'api:flow:apply:update', 'flow-service', 'PUT', '/api/flow/applies/**', 1),
    (21, '删除本人审批申请', 'api:flow:apply:delete', 'flow-service', 'DELETE', '/api/flow/applies/**', 1),
    (22, '审批管理查询', 'api:flow:admin:list', 'flow-service', 'GET', '/api/flow/admin/**', 1),
    (23, '审批同意驳回', 'api:flow:admin:approve', 'flow-service', 'POST', '/api/flow/admin/applies/**', 1),
    (24, '公告已读维护', 'api:notice:read-status', 'notice-service', 'POST', '/api/notice/notices/**', 1),
    (25, '公告管理发布', 'api:notice:admin:create', 'notice-service', 'POST', '/api/notice/admin/**', 1),
    (26, '公告管理修改', 'api:notice:admin:update', 'notice-service', 'PUT', '/api/notice/admin/**', 1),
    (27, '公告管理删除', 'api:notice:admin:delete', 'notice-service', 'DELETE', '/api/notice/admin/**', 1),
    (28, '月度考勤报表查询', 'api:attendance:monthly-report:list', 'attendance-service', 'GET', '/api/attendance/monthly-reports', 1),
    (29, '月度考勤报表生成', 'api:attendance:monthly-report:generate', 'attendance-service', 'POST', '/api/attendance/monthly-reports/generate', 1),
    (30, '工资结算管理查询', 'api:attendance:salary:list', 'attendance-service', 'GET', '/api/attendance/salary/statements', 1),
    (31, '个人工资查询', 'api:attendance:salary:my', 'attendance-service', 'GET', '/api/attendance/salary/my', 1),
    (32, '工资结算生成', 'api:attendance:salary:generate', 'attendance-service', 'POST', '/api/attendance/salary/generate', 1),
    (33, '部门考勤监控', 'api:attendance:dept-today', 'attendance-service', 'GET', '/api/attendance/dept-today', 1),
    (34, '考勤规则查询', 'api:attendance:rule:list', 'attendance-service', 'GET', '/api/attendance/rules', 1),
    (35, '考勤组查询', 'api:attendance:group:list', 'attendance-service', 'GET', '/api/attendance/groups', 1),
    (36, 'AI 问答查询', 'api:ai:rag:query', 'ai-service', 'GET', '/api/ai/rag/query', 1),
    (37, 'AI 知识库状态', 'api:ai:rag:status', 'ai-service', 'GET', '/api/ai/rag/status', 1),
    (38, 'AI 测试对话', 'api:ai:chat', 'ai-service', 'GET', '/api/ai/chat', 1),
    (39, 'AI 知识库上传', 'api:ai:rag:upload', 'ai-service', 'POST', '/api/ai/rag/upload', 1),
    (40, 'AI 知识库清空', 'api:ai:rag:clear', 'ai-service', 'DELETE', '/api/ai/rag/knowledge', 1);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 3),
    (5, 2),
    (6, 2),
    (7, 3),
    (8, 3),
    (9, 3),
    (10, 3);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
VALUES
    (2, 1), (2, 5), (2, 16), (2, 17), (2, 6), (2, 7), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15),
    (3, 1), (3, 5), (3, 16), (3, 6), (3, 7), (3, 15);

INSERT IGNORE INTO sys_role_api_permission (role_id, api_permission_id)
SELECT 1, id FROM sys_api_permission;

INSERT IGNORE INTO sys_role_api_permission (role_id, api_permission_id)
VALUES
    (2, 2), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 10), (2, 15), (2, 16),
    (2, 19), (2, 20), (2, 21), (2, 22), (2, 23), (2, 24), (2, 28), (2, 29), (2, 30), (2, 31), (2, 32), (2, 33), (2, 34), (2, 35),
    (2, 36), (2, 37), (2, 38), (2, 39),
    (3, 4), (3, 5), (3, 6), (3, 8), (3, 10), (3, 15), (3, 16),
    (3, 19), (3, 20), (3, 21), (3, 24), (3, 31), (3, 36), (3, 37);

INSERT IGNORE INTO sys_config (config_key, config_value, config_group, remark, status)
VALUES
    ('attendance.late-threshold-minutes', '10', 'ATTENDANCE', '迟到阈值分钟，正式以Nacos配置为准', 1),
    ('attendance.check-lock-seconds', '10', 'ATTENDANCE', '打卡分布式锁过期秒数，正式以Nacos配置为准', 1),
    ('jwt.expire-seconds', '86400', 'SECURITY', 'JWT过期秒数，正式以Nacos配置为准', 1);

INSERT IGNORE INTO attendance_rule (
    id, rule_name, work_start_time, work_end_time, late_threshold_minutes, early_leave_threshold_minutes,
    absent_threshold_minutes, check_lock_seconds, location_required, office_location_name, office_address,
    office_latitude, office_longitude, allowed_radius_meters, accuracy_threshold_meters, status
)
VALUES
    (1, '默认工作日考勤规则', '09:00:00', '18:00:00', 10, 10, 240, 10, 0, 'OfficeFlow 办公点', '请在考勤规则中设置真实办公地址', NULL, NULL, 1000, 1000, 1);

INSERT IGNORE INTO attendance_group (id, group_name, rule_id, dept_id, status)
VALUES
    (1, '研发部考勤组', 1, 2, 1),
    (2, '人事行政考勤组', 1, 3, 1);

INSERT IGNORE INTO notice (id, title, content, notice_type, priority, publisher_id, publish_time, status)
VALUES
    (1, 'OfficeFlow 项目启动通知', '请各成员按照分工完成对应模块开发，并保持每日提交。', 'COMPANY', 'IMPORTANT', 1, NOW(), 'PUBLISHED');

INSERT IGNORE INTO notice_scope (notice_id, scope_type, scope_id)
VALUES
    (1, 'ALL', NULL);

-- 初始化考勤多维度全状态模拟测试数据 (涵盖 NORMAL, RECHECKED, ON_LEAVE, LATE, EARLY_LEAVE, MISSING_CARD, ABSENT)
INSERT IGNORE INTO attendance_record (user_id, dept_id, work_date, check_in_time, check_in_ip, check_in_remark, check_out_time, check_out_ip, check_out_remark, work_minutes, late_minutes, early_leave_minutes, status, source)
VALUES
    -- 2026-07-22 (今日)
    (1, 1, '2026-07-22', '2026-07-22 08:52:10', '127.0.0.1', '管理员例行打卡', '2026-07-22 18:05:30', '127.0.0.1', '正常下班', 553, 0, 0, 'NORMAL', 'USER_CHECK'),
    (2, 2, '2026-07-22', '2026-07-22 08:58:00', '127.0.0.1', '研发主管打卡', '2026-07-22 18:10:00', '127.0.0.1', '正常下班', 552, 0, 0, 'NORMAL', 'USER_CHECK'),
    (4, 2, '2026-07-22', '2026-07-22 09:25:00', '127.0.0.1', '交通拥堵迟到', NULL, NULL, NULL, 0, 25, 0, 'LATE', 'USER_CHECK'),
    (7, 2, '2026-07-22', '2026-07-22 08:45:00', '127.0.0.1', '张伟上班打卡', NULL, NULL, NULL, 0, 0, 0, 'NORMAL', 'USER_CHECK'),
    -- 2026-07-21 (已补卡 RECHECKED)
    (4, 2, '2026-07-21', '2026-07-21 09:00:00', '127.0.0.1', '补打卡成功修正', '2026-07-21 18:00:00', '127.0.0.1', '正常下班', 540, 0, 0, 'RECHECKED', 'MANUAL'),
    (1, 1, '2026-07-21', '2026-07-21 08:50:00', '127.0.0.1', '正常出勤', '2026-07-21 18:15:00', '127.0.0.1', '正常下班', 565, 0, 0, 'NORMAL', 'USER_CHECK'),
    -- 2026-07-20 (休假中 ON_LEAVE)
    (4, 2, '2026-07-20', NULL, NULL, '因事休假已审批', NULL, NULL, '休假免打卡', 0, 0, 0, 'ON_LEAVE', 'MANUAL'),
    (7, 2, '2026-07-20', '2026-07-20 08:52:00', '127.0.0.1', '正常出勤', '2026-07-20 18:02:00', '127.0.0.1', '正常下班', 550, 0, 0, 'NORMAL', 'USER_CHECK'),
    -- 2026-07-19 (迟到 LATE)
    (4, 2, '2026-07-19', '2026-07-19 09:35:00', '127.0.0.1', '地铁故障迟到', '2026-07-19 18:00:00', '127.0.0.1', '正常下班', 505, 35, 0, 'LATE', 'USER_CHECK'),
    -- 2026-07-18 (早退 EARLY_LEAVE)
    (4, 2, '2026-07-18', '2026-07-18 08:55:00', '127.0.0.1', '正常上班', '2026-07-18 17:15:00', '127.0.0.1', '提前离岗', 495, 0, 45, 'EARLY_LEAVE', 'USER_CHECK'),
    -- 2026-07-17 (缺卡 MISSING_CARD)
    (4, 2, '2026-07-17', '2026-07-17 08:58:00', '127.0.0.1', '准时上班', NULL, NULL, '忘记下班打卡', 0, 0, 0, 'MISSING_CARD', 'USER_CHECK'),
    -- 2026-07-16 (旷工 ABSENT)
    (4, 2, '2026-07-16', NULL, NULL, '全天未打卡', NULL, NULL, '全天未打卡', 0, 0, 0, 'ABSENT', 'SYSTEM'),
    -- 2026-07-15 (迟到且早退 LATE_AND_EARLY)
    (4, 2, '2026-07-15', '2026-07-15 09:20:00', '127.0.0.1', '晚到', '2026-07-15 17:30:00', '127.0.0.1', '早走', 490, 20, 30, 'LATE_AND_EARLY', 'USER_CHECK');

-- 初始化员工薪资档案测试数据
INSERT IGNORE INTO sys_user_salary (user_id, base_salary, allowance)
VALUES
    (1, 15000.00, 1000.00),
    (2, 28000.00, 2000.00),
    (3, 12000.00, 500.00),
    (4, 10000.00, 500.00),
    (5, 20000.00, 1500.00),
    (6, 18000.00, 1500.00),
    (7, 19000.00, 800.00),
    (8, 17000.00, 800.00),
    (9, 11000.00, 500.00),
    (10, 9000.00, 500.00);

-- 初始化待审批测试单据 (供研发主管登录测试审批功能)
INSERT IGNORE INTO flow_apply (id, apply_no, applicant_id, applicant_dept_id, apply_type, title, reason, start_time, end_time, duration_hours, approver_id, status)
VALUES
    (101, 'LEAVE2026072201', 4, 2, 'LEAVE', '普通员工事假申请', '家中急事需请假半天', '2026-07-23 09:00:00', '2026-07-23 13:00:00', 4.0, 2, 'PENDING'),
    (102, 'OT2026072201', 4, 2, 'OVERTIME', '项目紧急冲刺加班', '后端接口性能调优上线', '2026-07-22 18:30:00', '2026-07-22 21:30:00', 3.0, 2, 'PENDING');

