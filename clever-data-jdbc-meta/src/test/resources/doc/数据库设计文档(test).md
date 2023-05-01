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

---
<br/>

**表名：** <a id="auto_increment_id">auto_increment_id</a>

**说明：** 自增长id表

**表字段：**

| 序号 | 名称 | 数据类型 | 主键 | 不能为空 | 是否索引 | 默认值 | 说明 |
| :--- | :--- | :---| :--- | :--- | :--- | :--- | :--- |
| 1 | id | bigint(20) | Y(自增) | Y | Y(唯一) |  | 主键id |
| 2 | sequence_name | varchar(127) | N | Y | Y(唯一) |  | 序列名称 |
| 3 | current_value | bigint(20) | N | Y |  | -1 | 当前值 |
| 4 | description | varchar(511) | N | N |  |  | 说明 |
| 5 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 6 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | id | bigint(20) | Y(自增) | Y | Y(唯一) |  | 主键id |
| 2 | code_name | varchar(127) | N | Y | Y(唯一) |  | 编码名称 |
| 3 | pattern | varchar(127) | N | Y |  |  | 编码规则表达式 |
| 4 | sequence | bigint(20) | N | Y |  | -1 | 序列值 |
| 5 | reset_pattern | varchar(127) | N | N |  |  | 重置sequence值的表达式，使用Java日期格式化字符串 |
| 6 | reset_flag | varchar(127) | N | Y |  |  | 重置sequence值标识，此字段值变化后则需要重置 |
| 7 | description | varchar(511) | N | N |  |  | 说明 |
| 8 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 9 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | id | bigint(20) | Y | Y | Y(唯一) |  | token id |
| 2 | user_id | bigint(20) | N | Y | Y |  | 用户id |
| 3 | token | varchar(4095) | N | Y |  |  | token数据 |
| 4 | expired_time | datetime(3) | N | N |  |  | token过期时间(空表示永不过期) |
| 5 | disable | int(11) | N | Y |  | 0 | token是否禁用: 0:未禁用；1:已禁用 |
| 6 | disable_reason | int(11) | N | N |  |  | token禁用原因: 0:使用RefreshToken；1:管理员手动禁用；2:并发登录被挤下线；3:用户主动登出 |
| 7 | refresh_token | varchar(127) | N | N |  |  | token数据 |
| 8 | rt_expired_time | datetime(3) | N | Y |  |  | 刷新token过期时间 |
| 9 | rt_state | int(11) | N | Y |  | 1 | 刷新token状态: 0:无效(已使用)；1:有效(未使用) |
| 10 | rt_use_time | datetime(3) | N | N |  |  | 刷新token使用时间 |
| 11 | rt_create_token_id | bigint(20) | N | N | Y |  | 刷新token创建的token id |
| 12 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 13 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | id | bigint(20) | Y(自增) | Y | Y(唯一) |  | 主键id |
| 2 | lock_name | varchar(127) | N | Y | Y(唯一) |  | 锁名称 |
| 3 | lock_count | bigint(20) | N | Y |  | 0 | 锁次数 |
| 4 | description | varchar(511) | N | N |  |  | 说明 |
| 5 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 6 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | id | bigint(20) | Y | Y | Y(唯一) |  | 主键id |
| 2 | user_id | bigint(20) | N | Y | Y |  | 用户id |
| 3 | login_type | int(11) | N | Y |  |  | 登录方式 |
| 4 | failed_count | int(11) | N | Y |  |  | 登录失败次数 |
| 5 | last_login_time | datetime(3) | N | Y |  |  | 最后登录失败时间 |
| 6 | delete_flag | int(11) | N | Y |  | 0 | 数据删除标志: 0:未删除，1:已删除 |
| 7 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 8 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | id | bigint(20) | Y | Y | Y(唯一) |  | 主键id |
| 2 | user_id | bigint(20) | N | Y | Y |  | 用户id |
| 3 | login_time | datetime(3) | N | Y | Y |  | 登录时间 |
| 4 | login_ip | varchar(31) | N | N |  |  | 登录ip |
| 5 | login_type | int(11) | N | Y |  |  | 登录方式 |
| 6 | login_channel | int(11) | N | Y |  |  | 登录渠道 |
| 7 | login_state | int(11) | N | Y |  |  | 登录状态: 0:登录失败，1:登录成功 |
| 8 | request_data | varchar(4095) | N | Y |  |  | 登录请求数据 |
| 9 | jwt_token_id | bigint(20) | N | N | Y |  | token id |
| 10 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |

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
| 1 | id | bigint(20) | Y | Y | Y(唯一) |  | 资源id |
| 2 | permission | varchar(63) | N | Y | Y(唯一) |  | 权限编码 |
| 3 | resource_type | int(11) | N | Y |  |  | 资源类型: 1:API权限，2:菜单权限，3:UI权限(如:按钮、表单、表格) |
| 4 | is_enable | int(11) | N | Y |  | 1 | 是否启用: 0:禁用，1:启用 |
| 5 | create_by | bigint(20) | N | Y |  |  | 创建人(用户id) |
| 6 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 7 | update_by | bigint(20) | N | N |  |  | 更新人(用户id) |
| 8 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | id | bigint(20) | Y | Y | Y(唯一) |  | 角色id |
| 2 | role_code | varchar(63) | N | Y | Y(唯一) |  | 角色编号 |
| 3 | role_name | varchar(63) | N | Y | Y |  | 角色名称 |
| 4 | is_enable | int(11) | N | Y |  | 1 | 是否启用: 0:禁用，1:启用 |
| 5 | create_by | bigint(20) | N | Y |  |  | 创建人(用户id) |
| 6 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 7 | update_by | bigint(20) | N | N |  |  | 更新人(用户id) |
| 8 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | role_id | bigint(20) | Y | Y | Y(唯一) |  | 角色id |
| 2 | resource_id | bigint(20) | Y | Y | Y(唯一) |  | 资源id |
| 3 | create_by | bigint(20) | N | Y |  |  | 创建人(用户id) |
| 4 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 5 | update_by | bigint(20) | N | N |  |  | 更新人(用户id) |
| 6 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | id | bigint(20) | Y | Y | Y(唯一) |  | 主键id |
| 2 | user_id | bigint(20) | N | Y | Y(唯一) |  | 用户id |
| 3 | security_context | varchar(16365) | N | Y |  |  | 用户security context |
| 4 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 5 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | id | bigint(20) | Y | Y | Y(唯一) |  | 用户id |
| 2 | login_name | varchar(63) | N | Y | Y(唯一) |  | 用户登录名(允许修改) |
| 3 | password | varchar(127) | N | Y |  |  | 登录密码 |
| 4 | user_name | varchar(63) | N | Y | Y |  | 登录名 |
| 5 | is_enable | int(11) | N | Y |  | 1 | 是否启用: 0:禁用，1:启用 |
| 6 | create_by | bigint(20) | N | Y |  |  | 创建人(用户id) |
| 7 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 8 | update_by | bigint(20) | N | N |  |  | 更新人(用户id) |
| 9 | update_at | datetime(3) | N | N |  |  | 更新时间 |

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
| 1 | user_id | bigint(20) | Y | Y | Y(唯一) |  | 用户id |
| 2 | role_id | bigint(20) | Y | Y | Y(唯一) |  | 角色id |
| 3 | create_by | bigint(20) | N | Y |  |  | 创建人(用户id) |
| 4 | create_at | datetime(3) | N | Y |  | CURRENT_TIMESTAMP(3) | 创建时间 |
| 5 | update_by | bigint(20) | N | N |  |  | 更新人(用户id) |
| 6 | update_at | datetime(3) | N | N |  |  | 更新时间 |

**表索引：**

| 序号 | 索引名 | 索引字段 | 唯一索引 |
| :--- | :--- | :--- | :--- |
| 1 | idx_sys_user_role_role_id | role_id|  N |
| 2 | idx_sys_user_role_user_id | user_id|  N |
| 3 | primary | user_id, role_id|  Y |

---

