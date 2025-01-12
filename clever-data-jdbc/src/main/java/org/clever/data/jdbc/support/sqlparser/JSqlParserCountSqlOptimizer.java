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
     * 获取jsqlparser中count的SelectItem
     */
    protected static final List<SelectItem<?>> COUNT_SELECT_ITEM = Collections.singletonList(
        new SelectItem<>(new Column().withColumnName("COUNT(1)")).withAlias(new Alias("total"))
    );

    @Override
    public String getCountSql(String rawSql, CountSqlOptions options) {
        if (options == null) {
            options = CountSqlOptimizer.DEFAULT_OPTIONS;
        }
        if (!options.isOptimizeCountSql()) {
            return CountSqlOptimizer.getRawCountSql(rawSql);
        }
        try {
            Select select = (Select) GlobalSqlParser.parse(rawSql);
            // union语法支持
            if (select instanceof SetOperationList) {
                return CountSqlOptimizer.getRawCountSql(rawSql);
            }
            PlainSelect plainSelect = (PlainSelect) select;
            // 优化 order by 在非分组情况下
            List<OrderByElement> orderBy = plainSelect.getOrderByElements();
            if (!CollectionUtils.isEmpty(orderBy)) {
                boolean canClean = true;
                for (OrderByElement order : orderBy) {
                    // order by 里带参数,不去除order by
                    Expression expression = order.getExpression();
                    if (!(expression instanceof Column) && expression.toString().contains(Keywords.QUESTION_MARK)) {
                        canClean = false;
                        break;
                    }
                }
                if (canClean) {
                    plainSelect.setOrderByElements(null);
                }
            }
            Distinct distinct = plainSelect.getDistinct();
            GroupByElement groupBy = plainSelect.getGroupBy();
            // 包含 distinct、groupBy 不优化
            if (null != distinct || null != groupBy) {
                return CountSqlOptimizer.getRawCountSql(rawSql);
            }
            // selectItems 包含 #{} ${}，它将被翻译为?，并且它可能位于函数中：power(#{myInt},2)
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                if (item.toString().contains(Keywords.QUESTION_MARK)) {
                    return CountSqlOptimizer.getRawCountSql(rawSql);
                }
            }
            // 包含 join 连表,进行判断是否移除 join 连表
            if (options.isOptimizeJoin()) {
                List<Join> joins = plainSelect.getJoins();
                if (!CollectionUtils.isEmpty(joins)) {
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
            }
            // 优化 SQL
            plainSelect.setSelectItems(COUNT_SELECT_ITEM);
            return select.toString();
        } catch (JSQLParserException e) {
            // 无法优化使用原 SQL
            log.warn("optimize this sql to a count sql has exception, sql:{}", rawSql, e);
        } catch (Exception e) {
            log.warn("optimize this sql to a count sql has error, sql:{}", rawSql, e);
        }
        return CountSqlOptimizer.getRawCountSql(rawSql);
    }
}
