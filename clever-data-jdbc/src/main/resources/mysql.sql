# create database if not exists db_name default character set utf8mb4 collate utf8mb4_unicode_ci;
# use db_name;
set names utf8mb4;

/* ====================================================================================================================
    auto_increment_id -- 自增长ID数据表
==================================================================================================================== */
create table auto_increment_id
(
    id                  bigint                  not null        auto_increment                          comment '主键id',
    sequence_name       varchar(127)    binary  not null        unique                                  comment '序列名称',
    current_value       bigint                  not null        default -1                              comment '当前值',
    description         varchar(511)                                                                    comment '说明',
    create_at           datetime(3)             not null        default current_timestamp(3)            comment '创建时间',
    update_at           datetime(3)                             on update current_timestamp(3)          comment '更新时间',
    primary key (id)
) engine=innodb default charset=utf8mb4 comment = '自增长id表';
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    biz_code -- 业务编码表
==================================================================================================================== */
create table biz_code
(
    id                  bigint                  not null        auto_increment                          comment '主键id',
    code_name           varchar(127)    binary  not null        unique                                  comment '编码名称',
    pattern             varchar(127)    binary  not null                                                comment '编码规则表达式',
    sequence            bigint                  not null        default -1                              comment '序列值',
    reset_pattern       varchar(127)    binary                                                          comment '重置sequence值的表达式，使用Java日期格式化字符串',
    reset_flag          varchar(127)    binary  not null        default ''                              comment '重置sequence值标识，此字段值变化后则需要重置',
    description         varchar(511)                                                                    comment '说明',
    create_at           datetime(3)             not null        default current_timestamp(3)            comment '创建时间',
    update_at           datetime(3)                             on update current_timestamp(3)          comment '更新时间',
    primary key (id)
) engine=innodb default charset=utf8mb4 comment = '业务编码表';
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    sys_lock -- 系统锁
==================================================================================================================== */
create table sys_lock
(
    id                  bigint                  not null        auto_increment                          comment '主键id',
    lock_name           varchar(127)    binary  not null        unique                                  comment '锁名称',
    lock_count          bigint                  not null        default 0                               comment '锁次数',
    description         varchar(511)                                                                    comment '说明',
    create_at           datetime(3)             not null        default current_timestamp(3)            comment '创建时间',
    update_at           datetime(3)                             on update current_timestamp(3)          comment '更新时间',
    primary key (id)
) engine=innodb default charset=utf8mb4 comment = '自增长id表';
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    存储过程 procedure
==================================================================================================================== */
-- MySQL 存储过程不支持嵌套事务!
-- #使用数据库工具调用mysql存储过程
-- call next_ids('t01', 1, @p1,@p2);
-- select @p1, @p2;
-- #存储过程授权
-- grant execute on procedure next_ids to `db_user`@`%`;

-- [存储过程]批量获取自增长序列值
create procedure next_ids(
    in  seq_name        varchar(127),   -- 序列名称
    in  size            int,            -- 批量值大小
    in  step            bigint,         -- 序列步进长度(必须大于0,默认为1)
    out old_val         bigint,         -- 序列自动增长之前的值
    out current_val     bigint          -- 序列自动增长后的值
)
begin
    -- 数据主键
    declare _row_id     bigint  default null;
    declare _add_value  int     default null;
    -- seq_name 不能为空
    if (seq_name is null or length(trim(seq_name)) <= 0) then
        signal sqlstate '45000' set message_text = '参数seq_name不能为空', mysql_errno = 1001;
    end if;
    set seq_name := trim(seq_name);
    -- size 默认值为 1
    if (size is null or trim(size) = '' or size <= 0) then
        set size := 1;
    end if;
    -- step 默认值为 1
    if (step is null or trim(step) = '' or step <= 0) then
        set step := 1;
    end if;
    set _add_value := size * step;
    -- 查询数据主键
    select
        id, current_value into _row_id, old_val
    from auto_increment_id
    where sequence_name = seq_name;
    if (_row_id is null or trim(_row_id) = '' or old_val is null) then
        -- 插入新数据
        insert into auto_increment_id
            (sequence_name, description)
        values
            (seq_name, '系统自动生成')
        on duplicate key update update_at = now();
        -- 查询数据主键
        select
            id, current_value into _row_id, old_val
        from auto_increment_id
        where sequence_name = seq_name;
    end if;
    -- 更新序列数据(使用Mysql行级锁保证并发性)
    update auto_increment_id set current_value = current_value + _add_value where id = _row_id;
    -- 查询更新之后的值
    select current_value into current_val from auto_increment_id where id = _row_id;
    set old_val := current_val - _add_value;
end;

-- [存储过程]获取下一个自增长序列值
create function next_id(
    seq_name        varchar(127)   -- 序列名称
)
returns bigint
begin
    declare _old_val        bigint  default null;
    declare _current_val    bigint  default null;
    call next_ids(seq_name, 1, 1, _old_val, _current_val);
    return _current_val;
end;

-- [存储过程]获取当前自增长序列值
create function current_id(
    seq_name        varchar(127)   -- 序列名称
)
returns bigint
begin
    declare _current_val    int default null;
    set seq_name := trim(seq_name);
    select current_value into _current_val from auto_increment_id where sequence_name = seq_name;
    return _current_val;
end;
