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
    in  size            int,            -- 批量值大小(必须大于0,默认为1)
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
    declare _current_val    bigint  default null;
    set seq_name := trim(seq_name);
    select current_value into _current_val from auto_increment_id where sequence_name = seq_name;
    return _current_val;
end;

-- [存储过程]与Java语言相同的DateFormat规则的时间格式化函数
create function java_date_format(
    date_time   datetime(3),    -- 时间值
    pattern     text            -- java时间格式
)
returns text
begin
    declare _pg_pattern     text    default pattern;
    -- set @debug_msg = null;
    -- 参数验证
    if(pattern is null or length(trim(pattern))<=0) then
        return '';
    end if;
    if(date_time is null) then
        return '';
    end if;
    /*
    DateFormat 规则
    ----------------------------------------
    MySQL       Java    描述
    ----------------------------------------
    %j          D       年份中的天数
    %Y          yyyy    4位年份
    %y          yy      2位年份
    %m          MM      月份
    %d          dd      月份中的天数
    %H          HH      24小时数
    %h          hh      12小时数
    %i          mm      分钟数
    %s          ss      秒数
    %f(μs)      SSS     毫秒数
    */
    if (locate('D', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'D', '%j');
    end if;
    if (locate('yyyy', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'yyyy', '%Y');
    end if;
    if (locate('yy', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'yy', '%y');
    end if;
    if (locate('MM', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'MM', '%m');
    end if;
    if (locate('dd', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'dd', '%d');
    end if;
    if (locate('HH', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'HH', '%H');
    end if;
    if (locate('hh', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'hh', '%h');
    end if;
    if (locate('mm', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'mm', '%i');
    end if;
    if (locate('ss', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'ss', '%s');
    end if;
    if (locate('SSS', _pg_pattern) > 0) then
        set _pg_pattern := replace(_pg_pattern, 'SSS', substr(date_format(date_time, '%f'), 1, 3));
        -- set @debug_msg := date_time;
    end if;
    return date_format(date_time, _pg_pattern);
end;

-- [存储过程]获取下一个业务code值
create function next_code(
    codename        varchar(127)   -- 业务编码规则
)
returns text
begin
    declare _size               int         default 1;
    declare _id                 bigint      default null;
    declare _pattern            text        default null;
    declare _sequence           bigint      default null;
    declare _reset_pattern      text        default null;
    declare _reset_flag         text        default null;
    declare _now                datetime(3) default null;
    declare _new_reset_flag     text        default null;
    declare _new_sequence       bigint      default null;
    declare _new_pattern        text        default null;
    declare _match_idx_1        int         default null;
    declare _match_idx_2        int         default null;
    declare _match              text        default null;
    declare _match_pattern      text        default null;
    declare _match_value        text        default null;
    declare _seq_digit_str      text        default null;
    declare _seq_digit          int4        default null;
    declare _seq_placeholder    text        default '@@@_seq_@@@';
    declare _res_seq            text        default null;
    -- set @debug_msg = null;
    if (codename is null or length(trim(codename)) <= 0) then
        signal sqlstate '45000' set message_text = '参数codename不能为空', mysql_errno = 1001;
    end if;
    select
        id, pattern, sequence, reset_pattern, reset_flag, now(3)
    into
        _id, _pattern, _sequence, _reset_pattern, _reset_flag, _now
    from biz_code where code_name = codename for update;
    if (_id is null) then
        set codename := concat('biz_code 表数据 code_name=[', codename, ']不存在');
        signal sqlstate '45000' set message_text = codename, mysql_errno = 1001;
    end if;
    -- 计算 reset_flag
    set _new_reset_flag := _reset_flag;
    set _new_sequence := _sequence;
    if (_reset_pattern is not null and length(trim(_reset_pattern)) > 0) then
        set _new_reset_flag := java_date_format(_now, _reset_pattern);
    end if;
    -- 判断是否需要重置 sequence 计数
    if ((_new_reset_flag is null and _reset_flag is not null) or (_new_reset_flag is not null and _reset_flag is null) or _new_reset_flag != _reset_flag) then
        set _new_sequence := 0;
    end if;
    set _new_sequence := _new_sequence + _size;
    -- 更新数据库值
    update biz_code set sequence=_new_sequence, reset_flag=_new_reset_flag, update_at=now(3) where id = _id;
    -- 处理 pattern | MySQL8.0+才引入 regexp_substr、regexp_instr、regexp_like、regexp_replace 四个函数
    set _new_pattern := _pattern;
    set _match_idx_1 := locate('${', _new_pattern);
    set _match_idx_2 := locate('}', _new_pattern);
    while(_match_idx_1 > 0 and _match_idx_2 > 0) do
        set _match := substr(_new_pattern, _match_idx_1, _match_idx_2 - _match_idx_1 + 1);
        set _match_pattern := substr(_new_pattern, _match_idx_1 + 2, _match_idx_2 - (_match_idx_1 + 2));
        if (_match_pattern like 'seq%') then
            set _seq_digit_str := substr(_match_pattern, 4);
            if (_seq_digit_str is not null and length(trim(_seq_digit_str)) > 0) then
                set _seq_digit := convert(_seq_digit_str, signed);
            end if;
            set _new_pattern := replace(_new_pattern, _match, _seq_placeholder);
        elseif (_match_pattern like 'id%') then
            set _seq_digit_str := substr(_match_pattern, 3);
            if (_seq_digit_str is not null and length(trim(_seq_digit_str)) > 0) then
                set _seq_digit := convert(_seq_digit_str, signed);
            end if;
            set _new_pattern := replace(_new_pattern, _match, _seq_placeholder);
        else
            set _match_value := java_date_format(_now, _match_pattern);
            set _new_pattern := replace(_new_pattern, _match, _match_value);
        end if;
        set _match_idx_1 := locate('${', _new_pattern);
        set _match_idx_2 := locate('}', _new_pattern);
    end while;
    -- 生成返回值
    -- declare _idx                int         default 0;
    -- set _idx := 0;
    -- while _idx <= _size do
    --     set _idx := _idx + 1;
    -- end while;
    set _new_sequence := _new_sequence - _size;
    if(_seq_digit_str is not null) then
        set _res_seq := convert(_new_sequence + 1, char);
        if(_seq_digit is not null) then
            if (length(_res_seq) > _seq_digit) then
                set _res_seq := substr(_res_seq, length(_res_seq) + 1 - _seq_digit);
            else
                set _res_seq := lpad(_res_seq, _seq_digit, '0');
            end if;
        end if;
        return replace(_new_pattern, _seq_placeholder, _res_seq);
    else
        return _new_pattern;
    end if;
end;

-- [存储过程]获取当前自增长序列值
create function lock_it(
    name    text    -- 序列名称
)
returns text
begin
    declare _lock_name  text    default null;
    -- 参数校验
    if (name is null or length(trim(name)) <= 0) then
        signal sqlstate '45000' set message_text = '参数name不能为空', mysql_errno = 1001;
    end if;
    set name := trim(name);
    -- 查询lock数据
    select lock_name into _lock_name from sys_lock where lock_name = name;
    if (_lock_name is null) then
        insert into sys_lock
            (lock_name, description)
        values
            (name, '系统自动生成')
        on duplicate key update update_at = now(3);
    end if;
    -- 利用数据库行级锁实现全局锁
    update sys_lock set lock_count=lock_count+1, update_at=now() where lock_name = name;
    return name;
end;
