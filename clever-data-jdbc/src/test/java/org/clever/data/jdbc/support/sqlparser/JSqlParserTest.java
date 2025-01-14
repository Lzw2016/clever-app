package org.clever.data.jdbc.support.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/14 09:39 <br/>
 */
@Slf4j
public class JSqlParserTest {
    @Test
    public void test01() throws JSQLParserException {
        String sql = """
            select
                t.id as a,
                t.namespace as b,
                t.report_time as c,
                (select t2.instance_name from task_scheduler t2 where t2.namespace=t.namespace limit 1) as d,
                (t.namespace || '_ns') as e
            from task_report as t
            """;
        Statement statement = CCJSqlParserUtil.parse(sql);
        if (statement instanceof PlainSelect plainSelect) {
            List<SelectItem<?>> list = plainSelect.getSelectItems();
            for (SelectItem<?> selectItem : list) {
                Expression expression = selectItem.getExpression();
                log.info("type={} | sql={}", expression.getClass().getName(), expression);
            }
            Column column = new Column()
                .withTable(new Table()
                    .withName("task_report")
                    .withAlias(new Alias("t"))
                )
                .withColumnName("job_count");
            SelectItem<?> selectItem = new SelectItem<>(column).withAlias(new Alias("f"));
            log.info("selectItem={}", selectItem);
            plainSelect.addSelectItems(selectItem);
        }
        log.info("statement={}", statement);
    }

    @Test
    public void test02() {
        // 表达式可以是简单的值（如常量、变量），也可以是复杂的逻辑组合（如 AND、OR、IN、LIKE 等）
        String[] exps = new String[]{
            // Column
            "t.job_count",
            "delete",
            // Concat
            "t.job_count || t.trigger_count",
            // Addition
            "t.job_count + t.trigger_count",
            "fun(t.job_count, t.job_count) + 5",
            // Function
            "fun(t.job_count, t.job_count)",
            // StringValue
            "'delete from task_report'",
            "';'",
            // ParenthesedSelect
            "(select t2.instance_name from task_scheduler t2 where t2.namespace=t.namespace)",
            // EqualsTo
            "t.job_count=1",
            // GreaterThan
            "t.job_count>1",
            // AndExpression
            "t.job_count>1 and t.trigger_count=1",
            // 解析失败
            // "delete from task_report",
            // "for update",
            // "t.job_count ; ",
            // " ; ",
            // "select t2.instance_name from task_scheduler t2 where t2.namespace=t.namespace",
            // "delete from task_report",
            // "(delete from task_report)",
            // "update task_report set namespace='' where 1=1",
            // "(update task_report set namespace='' where 1=1)",
        };
        for (String exp : exps) {
            if (StringUtils.isBlank(exp)) {
                continue;
            }
            try {
                Expression expression = CCJSqlParserUtil.parseExpression(exp, false);
                log.info("type={} | sql={}", expression.getClass().getName(), expression);
            } catch (Exception e) {
                log.error(exp, e);
            }
        }
    }

    @Test
    public void test03() throws JSQLParserException {
        String sql = """
            select id from test where 1=1 group by id ;
            select 0 as group_row from test where 1=1 group by id;

            update task_report set job_count = 960, job_err_count = 0, trigger_count = 971, misfire_count = 0 where task_report.report_time = '2024-10-02';
            delete from wms8_his.order_out_details t where exists(select 1 from wms8_his.order_out_0520 t1 where t.order_out_id=t1.order_out_id);

            drop function current_id;
            create function current_id(
                seq_name varchar(127)
            )
            returns bigint deterministic
            begin
                declare _current_val    bigint  default null;
                set seq_name := trim(seq_name);
                select current_value into _current_val from auto_increment_id where sequence_name = seq_name;
                return _current_val;
            end;
            alter table test add constraint test_pk_abc primary key (id);
            alter table test drop primary key;
            alter table test drop key `primary`;
            ;
            ;
            ;
            """;
        Statements statements = CCJSqlParserUtil.parseStatements(sql);
        for (Statement statement : statements) {
            log.info("type={} | sql={}", statement.getClass().getName(), statement);
        }
    }
}
