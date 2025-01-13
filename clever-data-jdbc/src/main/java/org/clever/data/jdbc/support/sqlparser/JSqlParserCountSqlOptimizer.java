package org.clever.data.jdbc.support.sqlparser;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/06/12 11:16 <br/>
 */
@Data
@Slf4j
public class JSqlParserCountSqlOptimizer implements CountSqlOptimizer {
    /**
     * 固定的select字句 {@code count(1) as total }
     */
    protected static final List<SelectItem<?>> SELECT_COUNT = Collections.singletonList(
        new SelectItem<>(new Column().withColumnName("COUNT(1)")).withAlias(new Alias("TOTAL"))
    );
    /**
     * 固定的select字句 {@code 0 as group_row }
     */
    protected static final List<SelectItem<?>> SELECT_ZERO = Collections.singletonList(
        new SelectItem<>(new Column().withColumnName("0")).withAlias(new Alias("GROUP_ROW"))
    );

    @Override
    public String getCountSql(String rawSql, CountSqlOptions options) {
        if (options == null) {
            options = CountSqlOptimizer.DEFAULT_OPTIONS;
        }
        if (!options.isOptimizeCountSql()) {
            return CountSqlOptimizer.getRawCountSql(rawSql);
        }
        // 影响 SQL 查询结果行数的语法包括: distinct、join、where、group by、having、limit、offset、union
        try {
            boolean wrapperCountSql = false;
            Select select = (Select) GlobalSqlParser.parse(rawSql);
            if (select instanceof SetOperationList) {
                // union TODO 可以继续优化子句
                return CountSqlOptimizer.getRawCountSql(rawSql);
            } else if (select instanceof PlainSelect plainSelect) {
                // 优化select字句
                Distinct distinct = plainSelect.getDistinct();
                if (distinct != null) {
                    wrapperCountSql = true;
                } else {
                    plainSelect.setSelectItems(SELECT_COUNT);
                }
                // 优化join字句 TODO 意义不大，需要继续深度优化
                List<Join> joins = plainSelect.getJoins();
                if (options.isOptimizeJoin() && !CollectionUtils.isEmpty(joins)) {
                    boolean canRemoveJoin = true;
                    String whereStr = Optional.ofNullable(plainSelect.getWhere()).map(Expression::toString).orElse(Keywords.EMPTY);
                    // 不区分大小写
                    whereStr = whereStr.toLowerCase();
                    for (Join join : joins) {
                        if (!join.isLeft()) {
                            canRemoveJoin = false;
                            break;
                        }
                        FromItem rightItem = join.getRightItem();
                        String str = "";
                        if (rightItem instanceof Table table) {
                            str = Optional.ofNullable(table.getAlias()).map(Alias::getName).orElse(table.getName()) + Keywords.DOT;
                        } else if (rightItem instanceof ParenthesedSelect subSelect) {
                            // 如果 left join 是子查询，并且子查询里包含 ?(代表有入参) 或者 where 条件里包含使用 join 的表的字段作条件,就不移除 join
                            if (subSelect.toString().contains(Keywords.QUESTION_MARK)) {
                                canRemoveJoin = false;
                                break;
                            }
                            str = subSelect.getAlias().getName() + Keywords.DOT;
                        }
                        // 不区分大小写
                        str = str.toLowerCase();
                        if (whereStr.contains(str)) {
                            // 如果 where 条件里包含使用 join 的表的字段作条件,就不移除 join
                            canRemoveJoin = false;
                            break;
                        }
                        for (Expression expression : join.getOnExpressions()) {
                            if (expression.toString().contains(Keywords.QUESTION_MARK)) {
                                // 如果 join 里包含 ?(代表有入参) 就不移除 join
                                canRemoveJoin = false;
                                break;
                            }
                        }
                    }
                    if (canRemoveJoin) {
                        plainSelect.setJoins(null);
                    }
                }
                // 优化group by字句
                GroupByElement groupBy = plainSelect.getGroupBy();
                if (groupBy != null) {
                    wrapperCountSql = true;
                    if (distinct == null) {
                        plainSelect.setSelectItems(SELECT_ZERO);
                    }
                }
                // 优化order by字句
                List<OrderByElement> orderBy = plainSelect.getOrderByElements();
                if (!CollectionUtils.isEmpty(orderBy)) {
                    plainSelect.setOrderByElements(null);
                }
            }
            String sql = select.toString();
            if (wrapperCountSql) {
                sql = CountSqlOptimizer.getRawCountSql(sql);
            }
            return sql;
        } catch (JSQLParserException e) {
            log.warn("optimize this sql to a count sql has exception, sql:{}", rawSql, e);
        } catch (Exception e) {
            log.warn("optimize this sql to a count sql has error, sql:{}", rawSql, e);
        }
        return CountSqlOptimizer.getRawCountSql(rawSql);
    }
}
