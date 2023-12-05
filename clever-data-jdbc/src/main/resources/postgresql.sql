/* ====================================================================================================================
    auto_increment_id -- 自增长ID数据表
==================================================================================================================== */
create table auto_increment_id
(
    id                  bigserial       not null,
    sequence_name       varchar(127)    not null,
    current_value       bigint          not null        default -1,
    description         varchar(511),
    create_at           timestamptz     not null        default now(),
    update_at           timestamptz                     default now(),
    primary key (id)
);
comment on table auto_increment_id is '自增长ID数据表';
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
    id                  bigserial       not null,
    code_name           varchar(127)    not null,
    pattern             varchar(127)    not null,
    sequence            bigint          not null        default -1,
    reset_pattern       varchar(127),
    reset_flag          varchar(127)    not null        default '',
    description         varchar(511),
    create_at           timestamptz     not null        default now(),
    update_at           timestamptz                     default now(),
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
    id                  bigserial       not null,
    lock_name           varchar(127)    not null,
    lock_count          bigint          not null        default 0,
    description         varchar(511),
    create_at           timestamptz     not null        default now(),
    update_at           timestamptz                     default now(),
    primary key (id)
);
comment on table sys_lock is '系统锁';
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
-- #存储过程授权
-- alter procedure next_codes(varchar, int4, out varchar[]) owner to db_user;

-- [存储过程]与Java语言相同的DateFormat规则的时间格式化函数
create or replace function java_date_format(
    date_time timestamp, pattern varchar
)
returns varchar
language plpgsql
as
$function$
DECLARE
    _java_patterns  varchar[];
    _pattern_map    json;
    _key            varchar;
    _value          varchar;
    _pg_pattern     varchar;
BEGIN
    if(pattern is null or length(trim(pattern))<=0) then
        return '';
    end if;
    if(date_time is null) then
        return '';
    end if;
    /*
    DateFormat 规则
    ----------------------------------------
    Postgresql  Java    描述
    ----------------------------------------
    DDD         D       年份中的天数
    MS          SSS     毫秒数
    YYYY        yyyy    4位年份
    YY          yy      2位年份
    MM          MM      月份
    DD          dd      月份中的天数
    HH24        HH      24小时数
    HH12        hh      12小时数
    MI          mm      分钟数
    SS          ss      秒数
    */
    _java_patterns := array['D', 'SSS', 'yyyy', 'yy', 'MM', 'dd', 'HH', 'hh', 'mm', 'ss'];
    _pattern_map := '{"yy": "YY", "yyyy": "YYYY", "MM": "MM", "dd": "DD", "HH": "HH24", "hh": "HH12", "mm": "MI", "ss": "SS", "SSS": "MS", "D": "DDD"}'::json;
    -- 确保 pattern 中只包含支持的 DateFormat 规则
    -- _pg_pattern := pattern;
    -- for _key in select json_object_keys(_pattern_map) loop
    --     _pg_pattern := replace(_pg_pattern, _key, '');
    -- end loop;
    -- if(length(trim(_pg_pattern))>0) then
    --     raise exception using message = -20000, hint = ('不支持的 pattern=[' || _pg_pattern || ']');
    -- end if;
    -- 替换 DateFormat 规则
    _pg_pattern := pattern;
    foreach _key in array _java_patterns loop
        _value := _pattern_map->>_key;
        _pg_pattern := replace(_pg_pattern, _key, _value);
    end loop;
    -- call p_log('java_date_format', '_pg_pattern=' || _pg_pattern);
    return to_char(date_time, _pg_pattern);
END;
$function$
;

-- [存储过程]批量获取业务code值
create or replace function next_code(
    codename varchar, size int4
)
returns varchar[]
language plpgsql
as
$function$
DECLARE
    _row                record;
    _new_reset_flag     varchar;
    _new_sequence       bigint;
    _new_pattern        varchar;
    _matches            varchar[];
    _match              varchar;
    _match_pattern      varchar;
    _match_value        varchar;
    _seq_digit_str      varchar;
    _seq_digit          int4;
    _seq_placeholder    varchar = '@@@_seq_@@@';
    _res_seq            varchar;
    _res                varchar[];
BEGIN
    if (size is null or size <= 0) then
        raise exception using message = -20000, hint = ('参数 size=[' || codename || '] 错误');
    end if;
    select
        id, pattern, sequence, reset_pattern, reset_flag, now() at time zone 'Asia/Shanghai' as now into _row
    from biz_code where code_name = codename for update;
    if (_row is null) then
        raise exception using message = -20000, hint = ('biz_code 表数据 code_name=[' || codename || ']不存在');
    end if;
    -- 计算 reset_flag
    _new_reset_flag := _row.reset_flag;
    _new_sequence := _row.sequence;
    if (_row.reset_pattern is not null and length(trim(_row.reset_pattern)) > 0) then
        _new_reset_flag := java_date_format(_row.now, _row.reset_pattern);
    end if;
    -- 判断是否需要重置 sequence 计数
    if (_new_reset_flag is distinct from _row.reset_flag) then
        _new_sequence := 0;
    end if;
    _new_sequence := _new_sequence + size;
    -- 更新数据库值
    update biz_code set sequence=_new_sequence, reset_flag=_new_reset_flag, update_at=now() where id = _row.id;
    -- 处理 pattern
    _new_pattern := _row.pattern;
    _res := array[size]; -- ::varchar[];
    for _matches in
        select regexp_matches(_row.pattern, '\$\{.*?}', 'g')
        loop
            foreach _match in array _matches loop
                _match_pattern := substring(_match, 3, length(_match) - 3);
                -- call p_log('next_code', 'match_pattern=' || _match_pattern);
                if (_match_pattern like ('seq%')) then
                    _seq_digit_str = substring(_match_pattern, 4);
                    if (_seq_digit_str is not null and length(_seq_digit_str) > 0) then
                        _seq_digit := _seq_digit_str::int4;
                    end if;
                    _new_pattern := replace(_new_pattern, _match, _seq_placeholder);
                elsif (_match_pattern like ('id%')) then
                    _seq_digit_str = substring(_match_pattern, 3);
                    if (_seq_digit_str is not null and length(_seq_digit_str) > 0) then
                        _seq_digit := _seq_digit_str::int4;
                    end if;
                    _new_pattern := replace(_new_pattern, _match, _seq_placeholder);
                else
                    _match_value := java_date_format(_row.now, _match_pattern);
                    _new_pattern := replace(_new_pattern, _match, _match_value);
                end if;
            end loop;
        end loop;
    -- 生成返回值
    _new_sequence := _new_sequence - size;
    for i in 1..size loop
        if(_seq_digit is not null) then
            _res_seq := (_new_sequence + i)::varchar;
            if (length(_res_seq) > _seq_digit) then
                _res_seq := substring(_res_seq, length(_res_seq) + 1 - _seq_digit);
            else
                _res_seq := lpad(_res_seq, _seq_digit, '0');
            end if;
            _res[i] := replace(_new_pattern, _seq_placeholder, _res_seq);
        else
            _res[i] := _new_pattern;
        end if;
    end loop;
    return _res;
END;
$function$
;

-- [存储过程]获取下一个业务code值
create or replace function next_code(
    codename varchar
)
returns varchar
language plpgsql
as
$function$
BEGIN
    return (next_code(codename, 1)::varchar[])[1];
END;
$function$
;
