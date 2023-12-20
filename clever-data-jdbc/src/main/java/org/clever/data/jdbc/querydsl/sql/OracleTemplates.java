package org.clever.data.jdbc.querydsl.sql;

import com.querydsl.core.types.Ops;

/**
 * 基于 {@link com.querydsl.sql.OracleTemplates} 实现的，为了解决下面问题: <br />
 * 1. 时间类型的 interval 运算不能使用两个字段运算。 <br />
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/12/20 18:30 <br/>
 */
public class OracleTemplates extends com.querydsl.sql.OracleTemplates {
    public static final PostgreSQLTemplates DEFAULT = new PostgreSQLTemplates();

    public OracleTemplates() {
        super('\\', false);
        fix();
    }

    public OracleTemplates(boolean quote) {
        super('\\', quote);
        fix();
    }

    public OracleTemplates(char escape, boolean quote) {
        super(escape, quote);
        fix();
    }

    private void fix() {
        // {0} + interval '{1s}' year -> 这种模式第二个参数不支持表字段名, 如: start_time + interval 'expiration_time_seconds' second
        // 修复方案:
        // 1. start_time + expiration_time_seconds * (interval '1' second)
        // 2. 使用 NumToYMInterval 和 NumToDSInterval 函数(不支持 week)
        add(Ops.DateTimeOps.ADD_YEARS, "{0} + {1s} * (interval '1' year)");
        add(Ops.DateTimeOps.ADD_MONTHS, "{0} + {1s} * (interval '1' month)");
        add(Ops.DateTimeOps.ADD_WEEKS, "{0} + {1s} * (interval '1' week)");
        add(Ops.DateTimeOps.ADD_DAYS, "{0} + {1s} * (interval '1' day)");
        add(Ops.DateTimeOps.ADD_HOURS, "{0} + {1s} * (interval '1' hour)");
        add(Ops.DateTimeOps.ADD_MINUTES, "{0} + {1s} * (interval '1' minute)");
        add(Ops.DateTimeOps.ADD_SECONDS, "{0} + {1s} * (interval '1' second)");
    }
}
