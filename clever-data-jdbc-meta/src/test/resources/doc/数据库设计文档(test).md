# 数据库设计文档

**数据库名：** test

**文档版本：** 1.0.0

| 序号 | 表名 | 说明 |
| :--- | :--- | :--- |
| 1 | [auto_increment_id](#auto_increment_id) | 自增长id表 |
| 2 | [biz_code](#biz_code) | 业务编码表 |
| 3 | [sys_jwt_token](#sys_jwt_token) | 登录JWT Token(缓存表) |
| 4 | [sys_lock](#sys_lock) | 自增长id表 |
| 5 | [sys_login_failed_count](#sys_login_failed_count) | 连续登录失败次数(缓存表) |
| 6 | [sys_login_log](#sys_login_log) | 登录日志 |
| 7 | [sys_resource](#sys_resource) | 资源表 |
| 8 | [sys_role](#sys_role) | 角色表 |
| 9 | [sys_role_resource](#sys_role_resource) | 角色资源关联表 |
| 10 | [sys_security_context](#sys_security_context) | 用户security context(缓存表) |
| 11 | [sys_user](#sys_user) | 用户表 |
| 12 | [sys_user_role](#sys_user_role) | 用户角色关联表 |
| 13 | [task_http_job](#task_http_job) | Http任务 |
| 14 | [task_java_job](#task_java_job) | js脚本任务 |
| 15 | [task_job](#task_job) | 定时任务 |
| 16 | [task_job_log](#task_job_log) | 任务执行日志 |
| 17 | [task_job_trigger](#task_job_trigger) | 任务触发器 |
| 18 | [task_job_trigger_log](#task_job_trigger_log) | 任务触发器日志 |
| 19 | [task_js_job](#task_js_job) | js脚本任务 |
| 20 | [task_scheduler](#task_scheduler) | 调度器 |
| 21 | [task_scheduler_lock](#task_scheduler_lock) | 调度器集群锁 |
| 22 | [task_scheduler_log](#task_scheduler_log) | 调度器事件日志 |
| 23 | [task_shell_job](#task_shell_job) | js脚本任务 |

---
<br/>

**表名：** <a id="auto_increment_id">auto_increment_id</a>

**说明：** 自增长id表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | sequence_name | varchar(127) | escape(primaryKey) | Y | Y(唯一) |  | 序列名称 |
| 3 | current_value | bigint(20) | escape(primaryKey) | Y |  | -1 | 当前值 |
| 4 | description | varchar(511) | escape(primaryKey) | N |  |  | 说明 |
| 5 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 6 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_auto_increment_id_sequence_name | sequence_name|  Y |
| 2 | primary | id|  Y |

---

**表名：** <a id="biz_code">biz_code</a>

**说明：** 业务编码表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | code_name | varchar(127) | escape(primaryKey) | Y | Y(唯一) |  | 编码名称 |
| 3 | pattern | varchar(127) | escape(primaryKey) | Y |  |  | 编码规则表达式 |
| 4 | sequence | bigint(20) | escape(primaryKey) | Y |  | -1 | 序列值 |
| 5 | reset_pattern | varchar(127) | escape(primaryKey) | N |  |  | 重置sequence值的表达式，使用Java日期格式化字符串 |
| 6 | reset_flag | varchar(127) | escape(primaryKey) | Y |  |  | 重置sequence值标识，此字段值变化后则需要重置 |
| 7 | description | varchar(511) | escape(primaryKey) | N |  |  | 说明 |
| 8 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 9 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | biz_code_code_name | code_name|  Y |
| 2 | primary | id|  Y |

---

**表名：** <a id="sys_jwt_token">sys_jwt_token</a>

**说明：** 登录JWT Token(缓存表)

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | token id |
| 2 | user_id | bigint(20) | escape(primaryKey) | Y | Y |  | 用户id |
| 3 | token | varchar(4095) | escape(primaryKey) | Y |  |  | token数据 |
| 4 | expired_time | datetime(3) | escape(primaryKey) | N |  |  | token过期时间(空表示永不过期) |
| 5 | disable | int(11) | escape(primaryKey) | Y |  | 0 | token是否禁用: 0:未禁用；1:已禁用 |
| 6 | disable_reason | int(11) | escape(primaryKey) | N |  |  | token禁用原因: 0:使用RefreshToken；1:管理员手动禁用；2:并发登录被挤下线；3:用户主动登出 |
| 7 | refresh_token | varchar(127) | escape(primaryKey) | N |  |  | token数据 |
| 8 | rt_expired_time | datetime(3) | escape(primaryKey) | Y |  |  | 刷新token过期时间 |
| 9 | rt_state | int(11) | escape(primaryKey) | Y |  | 1 | 刷新token状态: 0:无效(已使用)；1:有效(未使用) |
| 10 | rt_use_time | datetime(3) | escape(primaryKey) | N |  |  | 刷新token使用时间 |
| 11 | rt_create_token_id | bigint(20) | escape(primaryKey) | N | Y |  | 刷新token创建的token id |
| 12 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 13 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_sys_jwt_token_rt_create_token_id | rt_create_token_id|  N |
| 2 | idx_sys_jwt_token_user_id | user_id|  N |
| 3 | primary | id|  Y |

---

**表名：** <a id="sys_lock">sys_lock</a>

**说明：** 自增长id表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | lock_name | varchar(127) | escape(primaryKey) | Y | Y(唯一) |  | 锁名称 |
| 3 | lock_count | bigint(20) | escape(primaryKey) | Y |  | 0 | 锁次数 |
| 4 | description | varchar(511) | escape(primaryKey) | N |  |  | 说明 |
| 5 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 6 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | primary | id|  Y |
| 2 | sys_lock_lock_name | lock_name|  Y |

---

**表名：** <a id="sys_login_failed_count">sys_login_failed_count</a>

**说明：** 连续登录失败次数(缓存表)

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | user_id | bigint(20) | escape(primaryKey) | Y | Y |  | 用户id |
| 3 | login_type | int(11) | escape(primaryKey) | Y |  |  | 登录方式 |
| 4 | failed_count | int(11) | escape(primaryKey) | Y |  |  | 登录失败次数 |
| 5 | last_login_time | datetime(3) | escape(primaryKey) | Y |  |  | 最后登录失败时间 |
| 6 | delete_flag | int(11) | escape(primaryKey) | Y |  | 0 | 数据删除标志: 0:未删除，1:已删除 |
| 7 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 8 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_sys_login_failed_count_user_id | user_id|  N |
| 2 | primary | id|  Y |

---

**表名：** <a id="sys_login_log">sys_login_log</a>

**说明：** 登录日志

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | user_id | bigint(20) | escape(primaryKey) | Y | Y |  | 用户id |
| 3 | login_time | datetime(3) | escape(primaryKey) | Y | Y |  | 登录时间 |
| 4 | login_ip | varchar(31) | escape(primaryKey) | N |  |  | 登录ip |
| 5 | login_type | int(11) | escape(primaryKey) | Y |  |  | 登录方式 |
| 6 | login_channel | int(11) | escape(primaryKey) | Y |  |  | 登录渠道 |
| 7 | login_state | int(11) | escape(primaryKey) | Y |  |  | 登录状态: 0:登录失败，1:登录成功 |
| 8 | request_data | varchar(4095) | escape(primaryKey) | Y |  |  | 登录请求数据 |
| 9 | jwt_token_id | bigint(20) | escape(primaryKey) | N | Y |  | token id |
| 10 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_sys_login_log_jwt_token_id | jwt_token_id|  N |
| 2 | idx_sys_login_log_login_time | login_time|  N |
| 3 | idx_sys_login_log_user_id | user_id|  N |
| 4 | primary | id|  Y |

---

**表名：** <a id="sys_resource">sys_resource</a>

**说明：** 资源表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 资源id |
| 2 | permission | varchar(63) | escape(primaryKey) | Y | Y(唯一) |  | 权限编码 |
| 3 | resource_type | int(11) | escape(primaryKey) | Y |  |  | 资源类型: 1:API权限，2:菜单权限，3:UI权限(如:按钮、表单、表格) |
| 4 | is_enable | int(11) | escape(primaryKey) | Y |  | 1 | 是否启用: 0:禁用，1:启用 |
| 5 | create_by | bigint(20) | escape(primaryKey) | Y |  |  | 创建人(用户id) |
| 6 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 7 | update_by | bigint(20) | escape(primaryKey) | N |  |  | 更新人(用户id) |
| 8 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | permission | permission|  Y |
| 2 | primary | id|  Y |

---

**表名：** <a id="sys_role">sys_role</a>

**说明：** 角色表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 角色id |
| 2 | role_code | varchar(63) | escape(primaryKey) | Y | Y(唯一) |  | 角色编号 |
| 3 | role_name | varchar(63) | escape(primaryKey) | Y | Y |  | 角色名称 |
| 4 | is_enable | int(11) | escape(primaryKey) | Y |  | 1 | 是否启用: 0:禁用，1:启用 |
| 5 | create_by | bigint(20) | escape(primaryKey) | Y |  |  | 创建人(用户id) |
| 6 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 7 | update_by | bigint(20) | escape(primaryKey) | N |  |  | 更新人(用户id) |
| 8 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_sys_role_role_name | role_name|  N |
| 2 | primary | id|  Y |
| 3 | role_code | role_code|  Y |

---

**表名：** <a id="sys_role_resource">sys_role_resource</a>

**说明：** 角色资源关联表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | role_id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 角色id |
| 2 | resource_id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 资源id |
| 3 | create_by | bigint(20) | escape(primaryKey) | Y |  |  | 创建人(用户id) |
| 4 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 5 | update_by | bigint(20) | escape(primaryKey) | N |  |  | 更新人(用户id) |
| 6 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_sys_role_resource_resource_id | resource_id|  N |
| 2 | idx_sys_role_resource_role_id | role_id|  N |
| 3 | primary | role_id, resource_id|  Y |

---

**表名：** <a id="sys_security_context">sys_security_context</a>

**说明：** 用户security context(缓存表)

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | user_id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 用户id |
| 3 | security_context | varchar(16365) | escape(primaryKey) | Y |  |  | 用户security context |
| 4 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 5 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | primary | id|  Y |
| 2 | user_id | user_id|  Y |

---

**表名：** <a id="sys_user">sys_user</a>

**说明：** 用户表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 用户id |
| 2 | login_name | varchar(63) | escape(primaryKey) | Y | Y(唯一) |  | 用户登录名(允许修改) |
| 3 | password | varchar(127) | escape(primaryKey) | Y |  |  | 登录密码 |
| 4 | user_name | varchar(63) | escape(primaryKey) | Y | Y |  | 登录名 |
| 5 | is_enable | int(11) | escape(primaryKey) | Y |  | 1 | 是否启用: 0:禁用，1:启用 |
| 6 | create_by | bigint(20) | escape(primaryKey) | Y |  |  | 创建人(用户id) |
| 7 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 8 | update_by | bigint(20) | escape(primaryKey) | N |  |  | 更新人(用户id) |
| 9 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_sys_user_user_name | user_name|  N |
| 2 | login_name | login_name|  Y |
| 3 | primary | id|  Y |

---

**表名：** <a id="sys_user_role">sys_user_role</a>

**说明：** 用户角色关联表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | user_id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 用户id |
| 2 | role_id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 角色id |
| 3 | create_by | bigint(20) | escape(primaryKey) | Y |  |  | 创建人(用户id) |
| 4 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 5 | update_by | bigint(20) | escape(primaryKey) | N |  |  | 更新人(用户id) |
| 6 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_sys_user_role_role_id | role_id|  N |
| 2 | idx_sys_user_role_user_id | user_id|  N |
| 3 | primary | user_id, role_id|  Y |

---

**表名：** <a id="task_http_job">task_http_job</a>

**说明：** Http任务

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | job_id | bigint(20) | escape(primaryKey) | Y | Y |  | 任务ID |
| 4 | request_method | varchar(15) | escape(primaryKey) | Y |  |  | http请求method，ALL GET HEAD POST PUT DELETE CONNECT OPTIONS TRACE PATCH |
| 5 | request_url | varchar(511) | escape(primaryKey) | Y | Y |  | Http请求地址 |
| 6 | request_data | mediumtext | escape(primaryKey) | N |  |  | Http请求数据json格式，包含：params、headers、body |
| 7 | success_check | text | escape(primaryKey) | N |  |  | Http请求是否成功校验(js脚本) |
| 8 | create_at | datetime(3) | escape(primaryKey) | Y | Y | CURRENT_TIMESTAMP(3) | 创建时间 |
| 9 | update_at | datetime(3) | escape(primaryKey) | N | Y |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_http_job_create_at | create_at|  N |
| 2 | idx_task_http_job_job_id | job_id|  N |
| 3 | idx_task_http_job_request_url | request_url|  N |
| 4 | idx_task_http_job_update_at | update_at|  N |
| 5 | primary | id|  Y |

---

**表名：** <a id="task_java_job">task_java_job</a>

**说明：** js脚本任务

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | job_id | bigint(20) | escape(primaryKey) | Y | Y |  | 任务ID |
| 4 | is_static | tinyint(4) | escape(primaryKey) | Y |  | 1 | 是否是静态方法(函数)，0：非静态，1：静态 |
| 5 | class_name | varchar(255) | escape(primaryKey) | Y | Y |  | java class全路径 |
| 6 | class_method | varchar(63) | escape(primaryKey) | Y | Y |  | java class method |
| 7 | create_at | datetime(3) | escape(primaryKey) | Y | Y | CURRENT_TIMESTAMP(3) | 创建时间 |
| 8 | update_at | datetime(3) | escape(primaryKey) | N | Y |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_java_job_class_method | class_method|  N |
| 2 | idx_task_java_job_class_name | class_name|  N |
| 3 | idx_task_java_job_create_at | create_at|  N |
| 4 | idx_task_java_job_job_id | job_id|  N |
| 5 | idx_task_java_job_update_at | update_at|  N |
| 6 | primary | id|  Y |

---

**表名：** <a id="task_job">task_job</a>

**说明：** 定时任务

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | name | varchar(127) | escape(primaryKey) | Y | Y |  | 任务名称 |
| 4 | type | tinyint(4) | escape(primaryKey) | Y |  |  | 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本 |
| 5 | max_reentry | tinyint(4) | escape(primaryKey) | Y |  | 0 | 最大重入执行数量(对于单个节点当前任务未执行完成就触发了下一次执行导致任务重入执行)，小于等于0：表示禁止重入执行 |
| 6 | allow_concurrent | tinyint(4) | escape(primaryKey) | Y |  | 1 | 是否允许多节点并发执行，使用悲观锁实现，不建议禁止，0：禁止，1：允许 |
| 7 | max_retry_count | int(11) | escape(primaryKey) | Y |  | 0 | 执行失败时的最大重试次数 |
| 8 | route_strategy | tinyint(4) | escape(primaryKey) | Y |  | 0 | 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单 |
| 9 | first_scheduler | varchar(2047) | escape(primaryKey) | N |  |  | 路由策略，1-指定节点优先，调度器名称集合 |
| 10 | whitelist_scheduler | varchar(2047) | escape(primaryKey) | N |  |  | 路由策略，2-固定节点白名单，调度器名称集合 |
| 11 | blacklist_scheduler | varchar(2047) | escape(primaryKey) | N |  |  | 路由策略，3-固定节点黑名单，调度器名称集合 |
| 12 | load_balance | tinyint(4) | escape(primaryKey) | Y |  | 1 | 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH |
| 13 | is_update_data | tinyint(4) | escape(primaryKey) | Y |  | 1 | 是否更新任务数据，0：不更新，1：更新 |
| 14 | job_data | text | escape(primaryKey) | N |  |  | 任务数据(json格式) |
| 15 | run_count | bigint(20) | escape(primaryKey) | Y |  | 0 | 运行次数 |
| 16 | disable | tinyint(4) | escape(primaryKey) | Y |  | 0 | 是否禁用：0-启用，1-禁用 |
| 17 | description | varchar(511) | escape(primaryKey) | N |  |  | 描述 |
| 18 | create_at | datetime(3) | escape(primaryKey) | Y | Y | CURRENT_TIMESTAMP(3) | 创建时间 |
| 19 | update_at | datetime(3) | escape(primaryKey) | N | Y |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_job_create_at | create_at|  N |
| 2 | idx_task_job_name | name|  N |
| 3 | idx_task_job_update_at | update_at|  N |
| 4 | primary | id|  Y |

---

**表名：** <a id="task_job_log">task_job_log</a>

**说明：** 任务执行日志

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | instance_name | varchar(127) | escape(primaryKey) | Y | Y |  | 调度器实例名称 |
| 4 | job_trigger_log_id | bigint(20) | escape(primaryKey) | Y | Y |  | 对应的触发器日志ID |
| 5 | job_trigger_id | bigint(20) | escape(primaryKey) | N | Y |  | 任务触发器ID |
| 6 | job_id | bigint(20) | escape(primaryKey) | Y | Y |  | 任务ID |
| 7 | fire_time | datetime(3) | escape(primaryKey) | Y | Y |  | 触发时间 |
| 8 | start_time | datetime(3) | escape(primaryKey) | Y | Y |  | 开始执行时间 |
| 9 | end_time | datetime(3) | escape(primaryKey) | N | Y |  | 执行结束时间 |
| 10 | run_time | int(11) | escape(primaryKey) | N |  |  | 执行耗时(单位：毫秒) |
| 11 | status | tinyint(4) | escape(primaryKey) | N |  |  | 任务执行结果，0：成功，1：失败，2：取消 |
| 12 | retry_count | int(11) | escape(primaryKey) | Y |  |  | 重试次数 |
| 13 | exception_info | varchar(2047) | escape(primaryKey) | N |  |  | 异常信息 |
| 14 | run_count | bigint(20) | escape(primaryKey) | Y |  |  | 执行次数 |
| 15 | before_job_data | text | escape(primaryKey) | N |  |  | 执行前的任务数据 |
| 16 | after_job_data | text | escape(primaryKey) | N |  |  | 执行后的任务数据 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_job_log_end_time | end_time|  N |
| 2 | idx_task_job_log_fire_time | fire_time|  N |
| 3 | idx_task_job_log_instance_name | instance_name|  N |
| 4 | idx_task_job_log_job_id | job_id|  N |
| 5 | idx_task_job_log_job_trigger_id | job_trigger_id|  N |
| 6 | idx_task_job_log_job_trigger_log_id | job_trigger_log_id|  N |
| 7 | idx_task_job_log_start_time | start_time|  N |
| 8 | primary | id|  Y |

---

**表名：** <a id="task_job_trigger">task_job_trigger</a>

**说明：** 任务触发器

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | job_id | bigint(20) | escape(primaryKey) | Y | Y |  | 任务ID |
| 4 | name | varchar(127) | escape(primaryKey) | Y | Y |  | 触发器名称 |
| 5 | start_time | datetime(3) | escape(primaryKey) | Y |  |  | 触发开始时间 |
| 6 | end_time | datetime(3) | escape(primaryKey) | N |  |  | 触发结束时间 |
| 7 | last_fire_time | datetime(3) | escape(primaryKey) | N | Y |  | 上一次触发时间 |
| 8 | next_fire_time | datetime(3) | escape(primaryKey) | N | Y |  | 下一次触发时间 |
| 9 | misfire_strategy | tinyint(4) | escape(primaryKey) | Y |  | 2 | 错过触发策略，1：忽略，2：立即补偿触发一次 |
| 10 | allow_concurrent | tinyint(4) | escape(primaryKey) | Y |  | 0 | 是否允许多节点并行触发，使用悲观锁实现，不建议允许，0：禁止，1：允许 |
| 11 | type | tinyint(4) | escape(primaryKey) | Y |  |  | 任务类型，1：cron触发，2：固定间隔触发 |
| 12 | cron | varchar(511) | escape(primaryKey) | N |  |  | cron表达式 |
| 13 | fixed_interval | bigint(20) | escape(primaryKey) | N |  |  | 固定间隔触发，间隔时间(单位：秒) |
| 14 | fire_count | bigint(20) | escape(primaryKey) | Y |  | 0 | 触发次数 |
| 15 | disable | tinyint(4) | escape(primaryKey) | Y |  | 0 | 是否禁用：0-启用，1-禁用 |
| 16 | description | varchar(511) | escape(primaryKey) | N |  |  | 描述 |
| 17 | create_at | datetime(3) | escape(primaryKey) | Y | Y | CURRENT_TIMESTAMP(3) | 创建时间 |
| 18 | update_at | datetime(3) | escape(primaryKey) | N | Y |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_job_trigger_create_at | create_at|  N |
| 2 | idx_task_job_trigger_job_id | job_id|  N |
| 3 | idx_task_job_trigger_last_fire_time | last_fire_time|  N |
| 4 | idx_task_job_trigger_name | name|  N |
| 5 | idx_task_job_trigger_next_fire_time | next_fire_time|  N |
| 6 | idx_task_job_trigger_update_at | update_at|  N |
| 7 | primary | id|  Y |

---

**表名：** <a id="task_job_trigger_log">task_job_trigger_log</a>

**说明：** 任务触发器日志

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | instance_name | varchar(127) | escape(primaryKey) | Y | Y |  | 调度器实例名称 |
| 4 | job_trigger_id | bigint(20) | escape(primaryKey) | N |  |  | 任务触发器ID |
| 5 | job_id | bigint(20) | escape(primaryKey) | Y | Y |  | 任务ID |
| 6 | trigger_name | varchar(127) | escape(primaryKey) | Y | Y |  | 触发器名称 |
| 7 | fire_time | datetime(3) | escape(primaryKey) | Y | Y |  | 触发时间 |
| 8 | is_manual | tinyint(4) | escape(primaryKey) | Y |  |  | 是否是手动触发，0：系统自动触发，1：用户手动触发 |
| 9 | trigger_time | int(11) | escape(primaryKey) | Y |  |  | 触发耗时(单位：毫秒) |
| 10 | last_fire_time | datetime(3) | escape(primaryKey) | N | Y |  | 上一次触发时间 |
| 11 | next_fire_time | datetime(3) | escape(primaryKey) | N | Y |  | 下一次触发时间 |
| 12 | fire_count | bigint(20) | escape(primaryKey) | Y |  |  | 触发次数 |
| 13 | mis_fired | tinyint(4) | escape(primaryKey) | Y |  |  | 是否错过了触发，0：否，1：是 |
| 14 | trigger_msg | varchar(511) | escape(primaryKey) | N |  |  | 触发器消息 |
| 15 | create_at | datetime(3) | escape(primaryKey) | Y | Y | CURRENT_TIMESTAMP(3) | 创建时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_job_trigger_log_create_at | create_at|  N |
| 2 | idx_task_job_trigger_log_fire_time | fire_time|  N |
| 3 | idx_task_job_trigger_log_instance_name | instance_name|  N |
| 4 | idx_task_job_trigger_log_job_id | job_id|  N |
| 5 | idx_task_job_trigger_log_last_fire_time | last_fire_time|  N |
| 6 | idx_task_job_trigger_log_next_fire_time | next_fire_time|  N |
| 7 | idx_task_job_trigger_log_trigger_name | trigger_name|  N |
| 8 | primary | id|  Y |

---

**表名：** <a id="task_js_job">task_js_job</a>

**说明：** js脚本任务

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | job_id | bigint(20) | escape(primaryKey) | Y | Y |  | 任务ID |
| 4 | content | text | escape(primaryKey) | N |  |  | 文件内容 |
| 5 | read_only | tinyint(4) | escape(primaryKey) | Y |  | 0 | 读写权限：0-可读可写，1-只读 |
| 6 | create_at | datetime(3) | escape(primaryKey) | Y | Y | CURRENT_TIMESTAMP(3) | 创建时间 |
| 7 | update_at | datetime(3) | escape(primaryKey) | N | Y |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_js_job_create_at | create_at|  N |
| 2 | idx_task_js_job_job_id | job_id|  N |
| 3 | idx_task_js_job_update_at | update_at|  N |
| 4 | primary | id|  Y |

---

**表名：** <a id="task_scheduler">task_scheduler</a>

**说明：** 调度器

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间(同一个namespace的不同调度器属于同一个集群) |
| 3 | instance_name | varchar(127) | escape(primaryKey) | Y | Y |  | 调度器实例名称 |
| 4 | last_heartbeat_time | datetime(3) | escape(primaryKey) | Y |  |  | 最后心跳时间 |
| 5 | heartbeat_interval | bigint(20) | escape(primaryKey) | Y |  | 3000 | 心跳频率(单位：毫秒) |
| 6 | config | text | escape(primaryKey) | Y |  |  | 调度器配置，线程池大小、负载权重、最大并发任务数... |
| 7 | runtime_info | text | escape(primaryKey) | N |  |  | 调度器运行时信息 |
| 8 | description | varchar(511) | escape(primaryKey) | N |  |  | 描述 |
| 9 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 10 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_scheduler_instance_name | instance_name|  N |
| 2 | primary | id|  Y |

---

**表名：** <a id="task_scheduler_lock">task_scheduler_lock</a>

**说明：** 调度器集群锁

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | lock_name | varchar(63) | escape(primaryKey) | Y | Y |  | 锁名称 |
| 4 | lock_count | bigint(20) | escape(primaryKey) | Y |  | 0 | 锁次数 |
| 5 | description | varchar(511) | escape(primaryKey) | N |  |  | 描述 |
| 6 | create_at | datetime(3) | escape(primaryKey) | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 7 | update_at | datetime(3) | escape(primaryKey) | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_scheduler_lock_lock_name | lock_name|  N |
| 2 | primary | id|  Y |

---

**表名：** <a id="task_scheduler_log">task_scheduler_log</a>

**说明：** 调度器事件日志

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 编号 |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | instance_name | varchar(127) | escape(primaryKey) | Y | Y |  | 调度器实例名称 |
| 4 | event_name | varchar(63) | escape(primaryKey) | Y |  |  | 事件名称 |
| 5 | log_data | text | escape(primaryKey) | N |  |  | 事件日志数据 |
| 6 | create_at | datetime(3) | escape(primaryKey) | Y | Y | CURRENT_TIMESTAMP(3) | 创建时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_scheduler_log_create_at | create_at|  N |
| 2 | idx_task_scheduler_log_instance_name | instance_name|  N |
| 3 | primary | id|  Y |

---

**表名：** <a id="task_shell_job">task_shell_job</a>

**说明：** js脚本任务

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | escape(primaryKey) | Y | Y(唯一) |  | 主键id |
| 2 | namespace | varchar(63) | escape(primaryKey) | Y |  |  | 命名空间 |
| 3 | job_id | bigint(20) | escape(primaryKey) | Y | Y |  | 任务ID |
| 4 | shell_type | varchar(15) | escape(primaryKey) | Y |  |  | shell脚本类型：bash|sh|ash|powershell|cmd|python|node|deno|php |
| 5 | shell_charset | varchar(15) | escape(primaryKey) | N |  |  | 执行终端的字符集编码，如：“UTF-8” |
| 6 | shell_timeout | int(11) | escape(primaryKey) | Y |  | 600 | 执行超时时间，单位：秒，默认：“10分钟” |
| 7 | content | text | escape(primaryKey) | N |  |  | 文件内容 |
| 8 | read_only | tinyint(4) | escape(primaryKey) | Y |  | 0 | 读写权限：0-可读可写，1-只读 |
| 9 | create_at | datetime(3) | escape(primaryKey) | Y | Y | CURRENT_TIMESTAMP(3) | 创建时间 |
| 10 | update_at | datetime(3) | escape(primaryKey) | N | Y |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_task_shell_job_create_at | create_at|  N |
| 2 | idx_task_shell_job_job_id | job_id|  N |
| 3 | idx_task_shell_job_update_at | update_at|  N |
| 4 | primary | id|  Y |

---

