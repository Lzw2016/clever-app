package org.clever.data.jdbc.support.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/14 09:56 <br/>
 */
@Slf4j
public class GlobalSqlParserTest {
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
        Statement statement = GlobalSqlParser.parse(sql);
        if (statement instanceof PlainSelect plainSelect) {
            List<SelectItem<?>> list = plainSelect.getSelectItems();
            for (SelectItem<?> selectItem : list) {
                Expression expression = selectItem.getExpression();
                log.info("type={} | sql={}", expression.getClass().getName(), expression);
            }
        }
        log.info("statement={}", statement);
    }
}
