/* ====================================================================================================================
    auto_increment_id -- 自增长ID数据表
==================================================================================================================== */
create table auto_increment_id
(
    id                  bigint                  not null        auto_increment                          comment '主键id',
    sequence_name       varchar(127)    binary  not null                                                comment '序列名称',
    current_value       bigint                  not null        default -1                              comment '当前值',
    description         varchar(511)                                                                    comment '说明',
    create_at           datetime(3)             not null        default current_timestamp(3)            comment '创建时间',
    update_at           datetime(3)                             on update current_timestamp(3)          comment '更新时间',
    primary key (id)
) comment = '自增长id表';
create unique index idx_auto_increment_id_sequence_name on auto_increment_id (sequence_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    biz_code -- 业务编码表
==================================================================================================================== */
create table biz_code
(
    id                  bigint                  not null        auto_increment                          comment '主键id',
    code_name           varchar(127)    binary  not null                                                comment '编码名称',
    pattern             varchar(127)    binary  not null                                                comment '编码规则表达式',
    sequence            bigint                  not null        default -1                              comment '序列值',
    reset_pattern       varchar(127)    binary                                                          comment '重置sequence值的表达式，使用Java日期格式化字符串',
    reset_flag          varchar(127)    binary  not null        default ''                              comment '重置sequence值标识，此字段值变化后则需要重置',
    description         varchar(511)                                                                    comment '说明',
    create_at           datetime(3)             not null        default current_timestamp(3)            comment '创建时间',
    update_at           datetime(3)                             on update current_timestamp(3)          comment '更新时间',
    primary key (id)
) comment = '业务编码表';
create unique index biz_code_code_name on biz_code (code_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_lock -- 系统锁
==================================================================================================================== */
create table sys_lock
(
    id                  bigint                  not null        auto_increment                          comment '主键id',
    lock_name           varchar(127)    binary  not null                                                comment '锁名称',
    lock_count          bigint                  not null        default 0                               comment '锁次数',
    description         varchar(511)                                                                    comment '说明',
    create_at           datetime(3)             not null        default current_timestamp(3)            comment '创建时间',
    update_at           datetime(3)                             on update current_timestamp(3)          comment '更新时间',
    primary key (id)
) comment = '自增长id表';
create unique index sys_lock_lock_name on sys_lock (lock_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/





/* ====================================================================================================================
    存储过程 procedure
==================================================================================================================== */
-- [存储过程]获取自增长序列值
delimiter $
-- create definer = `dbuser`@`%` procedure next_id
create procedure next_id(
    in p_sequence_name      varchar(127),   -- 序列名称
    in p_step               bigint,         -- 序列步进长度(必须大于0,默认为1)
    out p_old_value         bigint,         -- 序列自动增长之前的值
    out p_current_value     bigint          -- 序列自动增长后的值
)
begin
    -- 数据主键
    declare row_id bigint default null;
    -- p_step 默认值为 1
    if (p_step is null or trim(p_step)='' or p_step<=0 ) then
        set p_step = 1;
    end if;
    -- 查询数据主键
    select
        id, current_value into row_id, p_old_value
    from auto_increment_id
    where sequence_name=p_sequence_name;
    -- 开启事务
    start transaction;
    if (row_id is null or trim(row_id)='' or p_old_value is null) then
        -- 插入新数据
        insert into auto_increment_id
            (sequence_name, description)
        values
            (p_sequence_name, '系统自动生成')
        on duplicate key update update_at=now();
        -- 查询数据主键
        select
            id, current_value into row_id, p_old_value
        from auto_increment_id
        where sequence_name=p_sequence_name;
    end if;
    -- 更新序列数据(使用Mysql行级锁保证并发性)
    update auto_increment_id set current_value=current_value+p_step where id=row_id;
    -- 查询更新之后的值
    select current_value into p_current_value from auto_increment_id where id=row_id;
    set p_old_value = p_current_value - p_step;
    -- 提交事务
    commit;
end
$

