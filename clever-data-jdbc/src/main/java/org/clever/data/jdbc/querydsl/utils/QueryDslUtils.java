//package org.clever.data.jdbc.querydsl.utils;
//
//import com.querydsl.core.JoinExpression;
//import com.querydsl.core.QueryMetadata;
//import com.querydsl.core.types.ConstantImpl;
//import com.querydsl.core.types.Expression;
//import com.querydsl.core.types.Path;
//import com.querydsl.core.types.dsl.*;
//import com.querydsl.sql.RelationalPathBase;
//import com.querydsl.sql.SQLQuery;
//import org.clever.core.Conv;
//import org.clever.core.DateUtils;
//import org.clever.core.NamingUtils;
//import org.clever.core.reflection.ReflectionsUtils;
//import org.clever.data.jdbc.querydsl.QArray;
//import org.clever.data.jdbc.querydsl.QLinkedMap;
//import org.clever.data.jdbc.querydsl.QList;
//import org.clever.model.YvanExt;
//import org.clever.model.request.QueryByPage;
//import org.clever.model.request.QueryBySort;
//import org.clever.model.request.filter.FilterItem;
//import org.clever.model.request.filter.FilterType;
//import org.clever.model.request.page.Page;
//import lombok.SneakyThrows;
//import org.apache.commons.lang3.StringUtils;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Modifier;
//import java.math.BigDecimal;
//import java.sql.Connection;
//import java.util.*;
//import java.util.function.Supplier;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/12/09 14:53 <br/>
// */
//public class QueryDslUtils {
//    public static QLinkedMap linkedMap(Expression<?>... exprs) {
//        return new QLinkedMap(exprs);
//    }
//
//    public static QArray array(Expression<?>... exprs) {
//        return new QArray(exprs);
//    }
//
//    public static QList list(Expression<?>... exprs) {
//        return new QList(exprs);
//    }
//
//    /**
//     * 过滤查询(只增加过滤参数)
//     */
//    public static <T> SQLQuery<T> queryByFilter(SQLQuery<T> sqlQuery) {
//        return queryByFilter(sqlQuery, getFieldMap(sqlQuery.getMetadata()));
//    }
//
//    /**
//     * 排序查询(只增加排序参数)
//     */
//    public static <T> SQLQuery<T> queryBySort(SQLQuery<T> sqlQuery) {
//        return queryBySort(sqlQuery, getFieldMap(sqlQuery.getMetadata()));
//    }
//
//    /**
//     * 分页查询(只增加分页参数)
//     */
//    public static <T> Page<T> queryByPage(SQLQuery<T> sqlQuery, QueryByPage queryByPage) {
//        if (queryByPage == null) {
//            queryByPage = new QueryByPage();
//        }
//        Page<T> page = queryByPage.result(Collections.emptyList());
//        // 执行 count 查询
//        if (page.isSearchCount()) {
//            long total = sqlQuery.fetchCount();
//            page.setTotal(total);
//            // 溢出总页数，设置最后一页
//            long pages = page.getPages();
//            if (page.getCurrent() > pages) {
//                page.setCurrent(pages);
//            }
//            Supplier<Connection> connProvider = ReflectionsUtils.getFieldValue(sqlQuery, "connProvider");
//            sqlQuery = sqlQuery.clone(connProvider.get());
//        } else {
//            page.setSearchCount(false);
//            page.setTotal(-1);
//        }
//        List<T> list = sqlQuery.offset(page.offset()).limit(page.getSize()).fetch();
//        page.setRecords(list);
//        return page;
//    }
//
//    /**
//     * 过滤、排序、分页查询
//     */
//    public static <T> Page<T> queryByPage(SQLQuery<T> sqlQuery) {
//        final QueryMetadata queryMetadata = sqlQuery.getMetadata();
//        final Map<String, Path<?>> fieldMap = getFieldMap(queryMetadata);
//        // 过滤
//        queryByFilter(sqlQuery, fieldMap);
//        // 排序
//        queryBySort(sqlQuery, fieldMap);
//        // 分页
//        QueryByPage queryByPage = YvanExt.getQueryByPage();
//        return queryByPage(sqlQuery, queryByPage);
//    }
//
//    // 增加过滤参数
//    private static <T> SQLQuery<T> queryByFilter(SQLQuery<T> sqlQuery, Map<String, Path<?>> fieldMap) {
//        List<FilterItem> filterItemList = YvanExt.getFilterModel();
//        if (!filterItemList.isEmpty()) {
//            for (FilterItem filterItem : filterItemList) {
//                final String sqlField = filterItem.getField();
//                Path<?> fieldPath = getPath(fieldMap, sqlField);
//                if (fieldPath == null) {
//                    continue;
//                }
//                Class<?> fieldType = fieldPath.getType();
//                final FilterType filterType = filterItem.getFilterType();
//                SimpleExpression<?> simplePath = null;
//                Set<Object> setIn = filterItem.getSetIn();
//                if (fieldPath instanceof SimpleExpression) {
//                    simplePath = (SimpleExpression<?>) fieldPath;
//                }
//                StringPath stringPath = null;
//                String stringFilter = filterItem.getStringFilter();
//                if (fieldPath instanceof StringPath) {
//                    stringPath = (StringPath) fieldPath;
//                }
//                NumberPath<?> numberPath = null;
//                BigDecimal bigDecimalFilter = filterItem.getBigDecimalFilter();
//                BigDecimal bigDecimalFilterTo = filterItem.getBigDecimalFilterTo();
//                if (fieldPath instanceof NumberPath) {
//                    numberPath = (NumberPath<?>) fieldPath;
//                }
//                DateTimePath<?> dateTimePath = null;
//                Date dateFrom = DateUtils.parseDate(filterItem.getDateFrom());
//                Date dateTo = DateUtils.parseDate(filterItem.getDateTo());
//                if (fieldPath instanceof DateTimePath) {
//                    dateTimePath = (DateTimePath<?>) fieldPath;
//                }
//                switch (filterType) {
//                    case Text_StartsWith:
//                        if (stringPath != null && StringUtils.isNotBlank(stringFilter)) {
//                            sqlQuery.where(stringPath.startsWith(stringFilter));
//                        }
//                        break;
//                    case Text_Equals:
//                        if (stringPath != null && StringUtils.isNotBlank(stringFilter)) {
//                            sqlQuery.where(stringPath.eq(stringFilter));
//                        }
//                        break;
//                    case Text_Contains:
//                        if (stringPath != null && StringUtils.isNotBlank(stringFilter)) {
//                            sqlQuery.where(stringPath.contains(stringFilter));
//                        }
//                        break;
//                    case Number_Equals:
//                        if (numberPath != null && bigDecimalFilter != null) {
//                            sqlQuery.where(numberPath.eq(ConstantImpl.create(bigDecimalFilter)));
//                        }
//                        break;
//                    case Number_NotEqual:
//                        if (numberPath != null && bigDecimalFilter != null) {
//                            sqlQuery.where(numberPath.ne(ConstantImpl.create(bigDecimalFilter)));
//                        }
//                        break;
//                    case Number_LessThan:
//                        if (numberPath != null && bigDecimalFilter != null) {
//                            sqlQuery.where(numberPath.lt(bigDecimalFilter));
//                        }
//                        break;
//                    case Number_LessThanOrEqual:
//                        if (numberPath != null && bigDecimalFilter != null) {
//                            sqlQuery.where(numberPath.loe(bigDecimalFilter));
//                        }
//                        break;
//                    case Number_GreaterThan:
//                        if (numberPath != null && bigDecimalFilter != null) {
//                            sqlQuery.where(numberPath.gt(bigDecimalFilter));
//                        }
//                        break;
//                    case Number_GreaterThanOrEqual:
//                        if (numberPath != null && bigDecimalFilter != null) {
//                            sqlQuery.where(numberPath.goe(bigDecimalFilter));
//                        }
//                        break;
//                    case Number_InRange:
//                        if (numberPath != null && (bigDecimalFilter != null || bigDecimalFilterTo != null)) {
//                            if (bigDecimalFilter != null && bigDecimalFilterTo == null) {
//                                sqlQuery.where(numberPath.goe(bigDecimalFilter));
//                            } else if (bigDecimalFilter == null) {
//                                sqlQuery.where(numberPath.loe(bigDecimalFilterTo));
//                            } else {
//                                sqlQuery.where(numberPath.goe(bigDecimalFilter).and(numberPath.loe(bigDecimalFilterTo)));
//                            }
//                        }
//                        break;
//                    case Date_InRange:
//                        if (dateTimePath != null && (dateFrom != null || dateTo != null)) {
//                            if (dateFrom != null && dateTo == null) {
//                                // noinspection unchecked,rawtypes
//                                sqlQuery.where(dateTimePath.goe((Expression) ConstantImpl.create(dateFrom)));
//                            } else if (dateFrom == null) {
//                                // noinspection unchecked,rawtypes
//                                sqlQuery.where(dateTimePath.loe((Expression) ConstantImpl.create(dateTo)));
//                            } else {
//                                // noinspection unchecked,rawtypes
//                                sqlQuery.where(dateTimePath.goe((Expression) ConstantImpl.create(dateFrom)).and(dateTimePath.loe((Expression) ConstantImpl.create(dateTo))));
//                            }
//                        }
//                        break;
//                    case Set_In:
//                        if (simplePath != null && setIn != null && !setIn.isEmpty()) {
//                            if (String.class.isAssignableFrom(fieldType)) {
//                                String[] values = setIn.stream()
//                                        .map(item -> org.clever.core.StringUtils.objectToString(item, StringUtils.EMPTY))
//                                        .toArray(String[]::new);
//                                // noinspection unchecked
//                                sqlQuery.where(((SimpleExpression<String>) simplePath).in(values));
//                            } else if (Number.class.isAssignableFrom(fieldType)) {
//                                Number[] values = setIn.stream()
//                                        .map(item -> Conv.asNumber(item, null))
//                                        .filter(Objects::nonNull)
//                                        .toArray(Number[]::new);
//                                // noinspection unchecked
//                                sqlQuery.where(((SimpleExpression<Number>) simplePath).in(values));
//                            }
//                        }
//                        break;
//                }
//            }
//        }
//        return sqlQuery;
//    }
//
//    // 增加排序参数
//    private static <T> SQLQuery<T> queryBySort(SQLQuery<T> sqlQuery, Map<String, Path<?>> fieldMap) {
//        QueryBySort queryBySort = YvanExt.getQueryBySort();
//        List<String> orderFields = queryBySort.getOrderFields();
//        List<String> sorts = queryBySort.getSorts();
//        if (!orderFields.isEmpty()) {
//            for (int i = 0; i < orderFields.size(); i++) {
//                String sqlField = orderFields.get(i);
//                String sort = sorts.get(i);
//                Path<?> fieldPath = getPath(fieldMap, sqlField);
//                if (!(fieldPath instanceof ComparableExpressionBase)) {
//                    continue;
//                }
//                ComparableExpressionBase<?> fieldExpression = (ComparableExpressionBase<?>) fieldPath;
//                if (QueryBySort.DESC.equalsIgnoreCase(sort)) {
//                    sqlQuery.orderBy(fieldExpression.desc());
//                } else {
//                    sqlQuery.orderBy(fieldExpression.asc());
//                }
//            }
//        }
//        return sqlQuery;
//    }
//
//    // 获取查询表的所有字段
//    @SneakyThrows
//    private static Map<String, Path<?>> getFieldMap(QueryMetadata queryMetadata) {
//        Map<String, Path<?>> fieldMap = new LinkedHashMap<>();
//        for (JoinExpression joinExpression : queryMetadata.getJoins()) {
//            Object target = joinExpression.getTarget();
//            if (!(target instanceof RelationalPathBase)) {
//                continue;
//            }
//            for (Field field : target.getClass().getFields()) {
//                if (!Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
//                    Object fieldValue = field.get(target);
//                    if (fieldValue instanceof Path) {
//                        Path<?> path = (Path<?>) fieldValue;
//                        StringBuilder fieldName = new StringBuilder();
//                        if (path.getMetadata() != null
//                                && path.getMetadata().getParent() != null
//                                && path.getMetadata().getParent().getMetadata() != null
//                                && StringUtils.isNotBlank(path.getMetadata().getParent().getMetadata().getName())) {
//                            fieldName.append(path.getMetadata().getParent().getMetadata().getName()).append(".");
//                        }
//                        if (path.getMetadata() != null && StringUtils.isNotBlank(path.getMetadata().getName())) {
//                            fieldName.append(path.getMetadata().getName());
//                        }
//                        if (fieldName.length() > 0) {
//                            fieldMap.put(fieldName.toString(), path);
//                        }
//                    }
//                }
//            }
//        }
//        return fieldMap;
//    }
//
//    private static Path<?> getPath(Map<String, Path<?>> fieldMap, String sqlField) {
//        final String field = NamingUtils.underlineToCamel(sqlField);
//        String fieldKey = fieldMap.keySet().stream()
//                .filter(key -> Objects.equals(field, key))
//                .findFirst().orElse(null);
//        if (StringUtils.isBlank(fieldKey)) {
//            fieldKey = fieldMap.keySet().stream()
//                    .filter(key -> key.endsWith("." + field))
//                    .findFirst().orElse(null);
//        }
//        if (StringUtils.isBlank(fieldKey)) {
//            return null;
//        }
//        return fieldMap.get(fieldKey);
//    }
//}
