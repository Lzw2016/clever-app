package org.clever.data.jdbc.querydsl.utils;

import com.querydsl.core.JoinExpression;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLQuery;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.function.TwoPredicate;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.OrderItem;
import org.clever.core.model.request.page.Page;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.data.jdbc.querydsl.QArray;
import org.clever.data.jdbc.querydsl.QEntity;
import org.clever.data.jdbc.querydsl.QLinkedMap;
import org.clever.data.jdbc.querydsl.QList;
import org.clever.data.jdbc.support.SqlUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.util.*;
import java.util.function.Supplier;

/**
 * 为扩展querydsl功能提供的工具类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/09 14:53 <br/>
 */
public class QueryDslUtils {
    /**
     * @param exprs 需要查询的“表字段、表”
     */
    public static QLinkedMap linkedMap(Expression<?>... exprs) {
        return new QLinkedMap(exprs);
    }

    /**
     * @param exprs 需要查询的“表字段、表”
     */
    public static QLinkedMap linkedMap(Collection<? extends Expression<?>> exprs) {
        return new QLinkedMap(exprs.toArray(new Expression<?>[0]));
    }

    /**
     * @param renameStrategy 字段名重命名规则
     * @param exprs          exprs 需要查询的“表字段、表”
     */
    public static QLinkedMap linkedMap(RenameStrategy renameStrategy, Expression<?>... exprs) {
        return new QLinkedMap(renameStrategy, exprs);
    }

    /**
     * @param renameStrategy 字段名重命名规则
     * @param exprs          exprs 需要查询的“表字段、表”
     */
    public static QLinkedMap linkedMap(RenameStrategy renameStrategy, Collection<? extends Expression<?>> exprs) {
        return new QLinkedMap(renameStrategy, exprs.toArray(new Expression<?>[0]));
    }

    /**
     * @param exprs exprs 需要查询的“表字段、表”
     */
    public static QArray array(Expression<?>... exprs) {
        return new QArray(exprs);
    }

    /**
     * @param exprs exprs 需要查询的“表字段、表”
     */
    public static QArray array(Collection<? extends Expression<?>> exprs) {
        return new QArray(exprs.toArray(new Expression<?>[0]));
    }

    /**
     * @param exprs exprs 需要查询的“表字段、表”
     */
    public static QList list(Expression<?>... exprs) {
        return new QList(exprs);
    }

    /**
     * @param exprs exprs 需要查询的“表字段、表”
     */
    public static QList list(Collection<? extends Expression<?>> exprs) {
        return new QList(exprs.toArray(new Expression<?>[0]));
    }

    /**
     * @param type           数据行类型
     * @param renameStrategy 字段名重命名规则
     * @param exprs          需要查询的“表字段、表”
     */
    public static <T> QEntity<T> entity(Class<? extends T> type, RenameStrategy renameStrategy, Expression<?>... exprs) {
        return new QEntity<>(type, renameStrategy, exprs);
    }

    /**
     * @param type           数据行类型
     * @param renameStrategy 字段名重命名规则
     * @param exprs          需要查询的“表字段、表”
     */
    public static <T> QEntity<T> entity(Class<? extends T> type, RenameStrategy renameStrategy, Collection<? extends Expression<?>> exprs) {
        return new QEntity<>(type, renameStrategy, exprs.toArray(new Expression<?>[0]));
    }

    /**
     * @param type  数据行类型
     * @param exprs 需要查询的“表字段、表”
     */
    public static <T> QEntity<T> entity(Class<? extends T> type, Expression<?>... exprs) {
        return new QEntity<>(type, exprs);
    }

    /**
     * @param type  数据行类型
     * @param exprs 需要查询的“表字段、表”
     */
    public static <T> QEntity<T> entity(Class<? extends T> type, Collection<? extends Expression<?>> exprs) {
        return new QEntity<>(type, exprs.toArray(new Expression<?>[0]));
    }

    /**
     * 排序查询(只增加排序参数)
     */
    public static <T> SQLQuery<T> queryBySort(SQLQuery<T> sqlQuery, QueryBySort queryBySort) {
        return queryBySort(sqlQuery, getFieldMap(sqlQuery.getMetadata()), queryBySort);
    }

    /**
     * 排序分页查询(增加排序、分页参数)
     */
    public static <T> Page<T> queryByPage(SQLQuery<T> sqlQuery, QueryByPage queryByPage) {
        final QueryMetadata queryMetadata = sqlQuery.getMetadata();
        final Map<String, Path<?>> fieldMap = getFieldMap(queryMetadata);
        // 排序
        queryBySort(sqlQuery, fieldMap, queryByPage);
        // 分页
        Page<T> page = QueryByPage.result(queryByPage, Collections.emptyList());
        // 执行 count 查询
        if (page.isSearchCount()) {
            long total = sqlQuery.fetchCount();
            page.setTotal(total);
            // 溢出总页数，设置最后一页
            long pages = page.getPages();
            if (page.getCurrent() > pages) {
                page.setCurrent(pages);
            }
            Supplier<Connection> connProvider = ReflectionsUtils.getFieldValue(sqlQuery, "connProvider");
            sqlQuery = sqlQuery.clone(connProvider.get());
        } else {
            page.setSearchCount(false);
            page.setTotal(-1);
        }
        List<T> list = sqlQuery.offset(page.offset()).limit(page.getSize()).fetch();
        page.setRecords(list);
        // 排序信息
        List<String> orderFieldsTmp = queryByPage.getOrderFields();
        List<String> sortsTmp = queryByPage.getSorts();
        for (int i = 0; i < orderFieldsTmp.size(); i++) {
            String fieldSql = orderFieldsTmp.get(i);
            String sort = sortsTmp.get(i);
            OrderItem orderItem = new OrderItem();
            orderItem.setColumn(fieldSql);
            orderItem.setAsc(SqlUtils.ASC.equalsIgnoreCase(StringUtils.trim(sort)));
            page.addOrder(orderItem);
        }
        return page;
    }

    // 增加排序参数
    private static <T> SQLQuery<T> queryBySort(SQLQuery<T> sqlQuery, Map<String, Path<?>> fieldMap, QueryBySort queryBySort) {
        List<String> orderFields = queryBySort.getOrderFields();
        List<String> sorts = queryBySort.getSorts();
        if (!orderFields.isEmpty()) {
            for (int i = 0; i < orderFields.size(); i++) {
                String sqlField = orderFields.get(i);
                String sort = sorts.get(i);
                Path<?> fieldPath = null;
                sqlField = queryBySort.getMappingField(sqlField);
                if (StringUtils.isNotBlank(sqlField)) {
                    fieldPath = getPath(fieldMap, sqlField);
                }
                if (fieldPath == null) {
                    sqlField = orderFields.get(i);
                    fieldPath = getPath(fieldMap, sqlField);
                }
                if (!(fieldPath instanceof ComparableExpressionBase)) {
                    continue;
                }
                ComparableExpressionBase<?> fieldExpression = (ComparableExpressionBase<?>) fieldPath;
                if (QueryBySort.DESC.equalsIgnoreCase(sort)) {
                    sqlQuery.orderBy(fieldExpression.desc());
                } else {
                    sqlQuery.orderBy(fieldExpression.asc());
                }
            }
        }
        return sqlQuery;
    }

    // 获取查询表的所有字段
    @SneakyThrows
    private static Map<String, Path<?>> getFieldMap(QueryMetadata queryMetadata) {
        Map<String, Path<?>> fieldMap = new LinkedHashMap<>();
        for (JoinExpression joinExpression : queryMetadata.getJoins()) {
            Object target = joinExpression.getTarget();
            if (!(target instanceof RelationalPath)) {
                continue;
            }
            for (Field field : target.getClass().getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                    Object fieldValue = field.get(target);
                    if (fieldValue instanceof Path) {
                        Path<?> path = (Path<?>) fieldValue;
                        StringBuilder fieldName = new StringBuilder();
                        if (path.getMetadata() != null
                            && path.getMetadata().getParent() != null
                            && path.getMetadata().getParent().getMetadata() != null
                            && StringUtils.isNotBlank(path.getMetadata().getParent().getMetadata().getName())) {
                            fieldName.append(path.getMetadata().getParent().getMetadata().getName()).append(".");
                        }
                        if (path.getMetadata() != null && StringUtils.isNotBlank(path.getMetadata().getName())) {
                            fieldName.append(path.getMetadata().getName());
                        }
                        if (fieldName.length() > 0) {
                            fieldMap.put(fieldName.toString(), path);
                        }
                    }
                }
            }
        }
        return fieldMap;
    }

    private static Path<?> getPath(Map<String, Path<?>> fieldMap, String sqlField) {
        TwoPredicate<String, String> match = getMatchFun();
        for (Map.Entry<String, Path<?>> entry : fieldMap.entrySet()) {
            String key = entry.getKey();
            Path<?> path = entry.getValue();
            if (key.equalsIgnoreCase(sqlField)) {
                return path;
            }
            PathMetadata pathMetadata = path.getMetadata();
            if (pathMetadata != null) {
                String field = pathMetadata.getName();
                if (match.test(field, sqlField)) {
                    return path;
                }
            }
            key = StringUtils.substringAfterLast(key, ".");
            if (StringUtils.isBlank(key)) {
                key = entry.getKey();
            }
            if (match.test(key, sqlField)) {
                return path;
            }
        }
        return null;
    }

    @NotNull
    private static TwoPredicate<String, String> getMatchFun() {
        RenameStrategy[] strategies = new RenameStrategy[]{
            RenameStrategy.None,
            RenameStrategy.ToCamel,
            RenameStrategy.ToCamelUpper,
            RenameStrategy.ToUnderline,
            RenameStrategy.ToUnderlineUpper,
            RenameStrategy.ToUpperCase,
            RenameStrategy.ToLowerCase,
        };
        return (rawStr, matchStr) -> {
            for (RenameStrategy strategy : strategies) {
                if (NamingUtils.rename(matchStr, strategy).equals(rawStr)) {
                    return true;
                }
            }
            return false;
        };
    }
}
