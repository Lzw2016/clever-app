# create database if not exists db_name default character set utf8mb4 collate utf8mb4_unicode_ci;
# use db_name;
set names utf8mb4;

/* ====================================================================================================================
    sys_user -- 用户表
==================================================================================================================== */
create table sys_user
(
    user_id         bigint              not null        auto_increment                  comment '用户id',
    user_code       varchar(127)        not null        unique                          comment '用户编号',
    user_name       varchar(63)         not null                                        comment '登录名',
    is_enable       int                 not null        default 1                       comment '是否启用: 0:禁用，1:启用',
    -- 其它扩展字段
    create_by       bigint              not null                                        comment '创建人',
    create_at       datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    update_by       bigint                                                              comment '更新人',
    update_at       datetime(3)                         on update current_timestamp(3)  comment '更新时间',
    primary key (user_id)
) engine=innodb default charset=utf8mb4 comment = '用户表';
create index idx_sys_user_user_name on sys_user (user_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_role -- 角色表
==================================================================================================================== */
create table sys_role
(
    role_id         bigint              not null        auto_increment                  comment '角色id',
    role_code       varchar(63)         not null        unique                          comment '角色编号',
    role_name       varchar(63)         not null                                        comment '角色名称',
    is_enable       int                 not null        default 1                       comment '是否启用: 0:禁用，1:启用',
    -- 其它扩展字段
    create_by       bigint              not null                                        comment '创建人',
    create_at       datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    update_by       bigint                                                              comment '更新人',
    update_at       datetime(3)                         on update current_timestamp(3)  comment '更新时间',
    primary key (role_id)
) engine=innodb default charset=utf8mb4 comment = '角色表';
create index idx_sys_role_role_name on sys_role (role_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_resource -- 资源表
==================================================================================================================== */
create table sys_resource
(
    resource_id     bigint              not null        auto_increment                  comment '资源id',
    permission      varchar(63)         not null        unique                          comment '权限编码',
    resource_type   int                 not null                                        comment '资源类型: 1:API权限，2:菜单权限，3:UI权限(如:按钮、表单、表格)',
    is_enable       int                 not null        default 1                       comment '是否启用: 0:禁用，1:启用',
    -- 其它扩展字段
    create_by       bigint              not null                                        comment '创建人',
    create_at       datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    update_by       bigint                                                              comment '更新人',
    update_at       datetime(3)                         on update current_timestamp(3)  comment '更新时间',
    primary key (resource_id)
) engine=innodb default charset=utf8mb4 comment = '资源表';
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_user_role -- 用户角色关联表
==================================================================================================================== */
create table sys_user_role
(
    user_id         bigint              not null                                        comment '用户id',
    role_id         bigint              not null                                        comment '角色id',
    create_by       bigint              not null                                        comment '创建人',
    create_at       datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    update_by       bigint                                                              comment '更新人',
    update_at       datetime(3)                         on update current_timestamp(3)  comment '更新时间',
    primary key (user_id, role_id)
) engine=innodb default charset=utf8mb4 comment = '用户角色关联表';
create index idx_sys_user_role_user_id on sys_user_role (user_id);
create index idx_sys_user_role_role_id on sys_user_role (role_id);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_role_resource -- 角色资源关联表
==================================================================================================================== */
create table sys_role_resource
(
    role_id         bigint              not null                                        comment '角色id',
    resource_id     bigint              not null                                        comment '资源id',
    create_by       bigint              not null                                        comment '创建人',
    create_at       datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    update_by       bigint                                                              comment '更新人',
    update_at       datetime(3)                         on update current_timestamp(3)  comment '更新时间',
    primary key (role_id, resource_id)
) engine=innodb default charset=utf8mb4 comment = '角色资源关联表';
create index idx_sys_role_resource_role_id on sys_role_resource (role_id);
create index idx_sys_role_resource_resource_id on sys_role_resource (resource_id);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_login_log -- 登录日志
==================================================================================================================== */
create table sys_login_log
(
    id                  bigint              not null       auto_increment                   comment '主键id',
    user_id             bigint              not null                                        comment '用户id',
    login_time          datetime(3)         not null                                        comment '登录时间',
    login_ip            varchar(31)                                                         comment '登录ip',
    login_type          int                 not null                                        comment '登录方式',
    login_channel       int                 not null                                        comment '登录渠道',
    login_state         int                 not null                                        comment '登录状态: 0:登录失败，1:登录成功',
    request_data        varchar(4095)       not null                                        comment '登录请求数据',
    jwt_token_id        bigint                                                              comment 'token id',
    create_at           datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    primary key (id)
) engine=innodb default charset=utf8mb4 comment = '登录日志';
create index idx_sys_login_log_user_id on sys_login_log (user_id);
create index idx_sys_login_log_login_time on sys_login_log (login_time);
create index idx_sys_login_log_jwt_token_id on sys_login_log (jwt_token_id);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_jwt_token -- 登录JWT Token(缓存表)
==================================================================================================================== */
create table sys_jwt_token
(
    token_id            bigint              not null       auto_increment                   comment 'token id',
    user_id             bigint              not null                                        comment '用户id',
    token               varchar(4095)       not null                                        comment 'token数据',
    expired_time        datetime(3)                                                         comment 'token过期时间(空表示永不过期)',
    disable             int                 not null        default 0                       comment 'token是否禁用: 0:未禁用；1:已禁用',
    disable_reason      int                                                                 comment 'token禁用原因: 0:使用RefreshToken；1:管理员手动禁用；2:并发登录被挤下线；3:用户主动登出',
    refresh_token       varchar(127)                                                        comment 'token数据',
    rt_expired_time     datetime(3)         not null                                        comment '刷新token过期时间',
    rt_state            int                 not null        default 1                       comment '刷新token状态: 0:无效(已使用)；1:有效(未使用)',
    rt_use_time         datetime(3)                                                         comment '刷新token使用时间',
    rt_create_token_id  bigint                                                              comment '刷新token创建的token id',
    create_at           datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    update_at           datetime(3)                         on update current_timestamp(3)  comment '更新时间',
    primary key (token_id)
) engine=innodb default charset=utf8mb4 comment = '登录JWT Token(缓存表)';
create index idx_sys_jwt_token_user_id on sys_jwt_token (user_id);
create index idx_sys_jwt_token_rt_create_token_id on sys_jwt_token (rt_create_token_id);
# create index idx_sys_jwt_token_ on sys_jwt_token ();
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_login_failed_count -- 连续登录失败次数(缓存表)
==================================================================================================================== */
create table sys_login_failed_count
(
    id                  bigint              not null        auto_increment                  comment '主键id',
    user_id             bigint              not null                                        comment '用户id',
    login_type          int                 not null                                        comment '登录方式',
    failed_count        int                 not null                                        comment '登录失败次数',
    last_login_time     datetime(3)         not null                                        comment '最后登录失败时间',
    delete_flag         int                 not null        default 0                       comment '数据删除标志: 0:未删除，1:已删除',
    create_at           datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    update_at           datetime(3)                         on update current_timestamp(3)  comment '更新时间',
    primary key (id)
) engine=innodb default charset=utf8mb4 comment = '连续登录失败次数(缓存表)';
create index idx_sys_login_failed_count_user_id on sys_login_failed_count (user_id);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_security_context -- 用户security context(缓存表)
==================================================================================================================== */
create table sys_security_context
(
    id                  bigint              not null        auto_increment                  comment '主键id',
    user_id             bigint              not null                                        comment '用户id',
    security_context    varchar(16365)      not null                                        comment '用户security context',
    create_at           datetime(3)         not null        default current_timestamp(3)    comment '创建时间',
    update_at           datetime(3)                         on update current_timestamp(3)  comment '更新时间',
    primary key (id)
) engine=innodb default charset=utf8mb4 comment = '用户security context(缓存表)';
create index idx_sys_security_context_user_id on sys_security_context (user_id);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/

