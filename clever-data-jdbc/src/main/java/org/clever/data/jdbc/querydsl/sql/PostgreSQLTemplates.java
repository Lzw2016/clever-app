package org.clever.data.jdbc.querydsl.sql;

import com.querydsl.core.types.Ops;

/**
 * 基于 {@link com.querydsl.sql.PostgreSQLTemplates} 实现的，为了解决下面问题: <br />
 * 1. 时间类型的 interval 运算不能使用两个字段运算。 <br />
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/12/20 18:13 <br/>
 */
public class PostgreSQLTemplates extends com.querydsl.sql.PostgreSQLTemplates {
    public static final PostgreSQLTemplates DEFAULT = new PostgreSQLTemplates();

    public PostgreSQLTemplates() {
        super('\\', false);
        fix();
    }

    public PostgreSQLTemplates(boolean quote) {
        super('\\', quote);
        fix();
    }

    public PostgreSQLTemplates(char escape, boolean quote) {
        super(escape, quote);
        fix();
    }

    private void fix() {
        // {0} + interval '{1s} years' -> 这种模式第二个参数不支持表字段名, 如: start_time + interval 'expiration_time_seconds seconds'
        // 修复方案:
        // 1. start_time + expiration_time_seconds * (interval '1 seconds')
        // 2. start_time + (expiration_time_seconds || ' seconds')::interval
        add(Ops.DateTimeOps.ADD_YEARS, "{0} + ({1s} || ' years')::interval");
        add(Ops.DateTimeOps.ADD_MONTHS, "{0} + ({1s} || ' months')::interval");
        add(Ops.DateTimeOps.ADD_WEEKS, "{0} + ({1s} || ' weeks')::interval");
        add(Ops.DateTimeOps.ADD_DAYS, "{0} + ({1s} || ' days')::interval");
        add(Ops.DateTimeOps.ADD_HOURS, "{0} + ({1s} || ' hours')::interval");
        add(Ops.DateTimeOps.ADD_MINUTES, "{0} + ({1s} || ' minutes')::interval");
        add(Ops.DateTimeOps.ADD_SECONDS, "{0} + ({1s} || ' seconds')::interval");
    }
}
