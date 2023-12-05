/* ====================================================================================================================
    auto_increment_id -- 自增长ID数据表
==================================================================================================================== */
create table auto_increment_id
(
    id              number(19)                                      not null,
    sequence_name   varchar2(127)                                   not null,
    current_value   number(19)      default -1                      not null,
    description     varchar2(511),
    create_at       timestamp(6)    default CURRENT_TIMESTAMP(3)    not null,
    update_at       timestamp(6),
    primary key (id)
);
comment on table auto_increment_id is '自增长id表';
comment on column auto_increment_id.id is '主键id';
comment on column auto_increment_id.sequence_name is '序列名称';
comment on column auto_increment_id.current_value is '当前值';
comment on column auto_increment_id.description is '说明';
comment on column auto_increment_id.create_at is '创建时间';
comment on column auto_increment_id.update_at is '更新时间';
create unique index idx_auto_increment_id_sequence_name on auto_increment_id (sequence_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    biz_code -- 业务编码表
==================================================================================================================== */
create table biz_code
(
    id              number(19)                                      not null,
    code_name       varchar2(127)                                   not null,
    pattern         varchar2(127)                                   not null,
    sequence        number(19)      default -1                      not null,
    reset_pattern   varchar2(127),
    reset_flag      varchar2(127)                                   not null,
    description     varchar2(511),
    create_at       timestamp(6)    default CURRENT_TIMESTAMP(3)    not null,
    update_at       timestamp(6),
    primary key (id)
);
comment on table biz_code is '业务编码表';
comment on column biz_code.id is '主键id';
comment on column biz_code.code_name is '编码名称';
comment on column biz_code.pattern is '编码规则表达式';
comment on column biz_code.sequence is '序列值';
comment on column biz_code.reset_pattern is '重置sequence值的表达式，使用Java日期格式化字符串';
comment on column biz_code.reset_flag is '重置sequence值标识，此字段值变化后则需要重置';
comment on column biz_code.description is '说明';
comment on column biz_code.create_at is '创建时间';
comment on column biz_code.update_at is '更新时间';
create unique index biz_code_code_name on biz_code (code_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_lock -- 系统锁
==================================================================================================================== */
create table sys_lock
(
    id              number(19)                                      not null,
    lock_name       varchar2(127)                                   not null,
    lock_count      number(19)      default 0                       not null,
    description     varchar2(511),
    create_at       timestamp(6)    default CURRENT_TIMESTAMP(3)    not null,
    update_at       timestamp(6),
    primary key (id)
);
comment on table sys_lock is '自增长id表';
comment on column sys_lock.id is '主键id';
comment on column sys_lock.lock_name is '锁名称';
comment on column sys_lock.lock_count is '锁次数';
comment on column sys_lock.description is '说明';
comment on column sys_lock.create_at is '创建时间';
comment on column sys_lock.update_at is '更新时间';
create unique index sys_lock_lock_name on sys_lock (lock_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    存储过程 procedure
==================================================================================================================== */
