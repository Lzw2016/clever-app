//package org.clever.data.jdbc.ext
//
//import com.querydsl.core.types.Expression
//import com.querydsl.core.types.Path
//import com.querydsl.core.types.Predicate
//import com.querydsl.sql.RelationalPathBase
//import com.querydsl.sql.SQLQuery
//import com.querydsl.sql.dml.SQLDeleteClause
//import com.querydsl.sql.dml.SQLInsertClause
//import com.querydsl.sql.dml.SQLUpdateClause
//import org.clever.core.model.request.QueryByPage
//import org.clever.core.model.request.page.Page
//import org.clever.data.jdbc.querydsl.QArray
//import org.clever.data.jdbc.querydsl.QLinkedMap
//import org.clever.data.jdbc.querydsl.QList
//import org.clever.data.jdbc.querydsl.sql.dml.MapMapper
//import org.clever.data.jdbc.querydsl.sql.dml.SafeSQLInsertClause
//import org.clever.data.jdbc.querydsl.sql.dml.SafeSQLUpdateClause
//import org.clever.data.jdbc.querydsl.utils.QueryDslUtils
//import org.clever.data.jdbc.querydsl.utils.SQLClause
//
//import java.util.function.Supplier
//
///**
// *
// * 作者：lizw <br/>
// * 创建时间：2024/01/17 19:53 <br/>
// */
//class QueryDSLExtension {
//    /**
//     * 以 LinkedHashMap 的形式返回数据
//     */
//    static QLinkedMap linkedMap(RelationalPathBase<?> self) {
//        return QueryDslUtils.linkedMap(self.all())
//    }
//
//    /**
//     * 以 LinkedHashMap 的形式返回数据
//     */
//    static QLinkedMap linkedMap(Expression<?>[] self) {
//        return QueryDslUtils.linkedMap(self)
//    }
//
//    /**
//     * 以 LinkedHashMap 的形式返回数据
//     */
//    static QLinkedMap linkedMap(Collection<? extends Expression<?>> self) {
//        Expression<?>[] array = self.toArray(new Expression<?>[0])
//        return QueryDslUtils.linkedMap(array)
//    }
//
//    /**
//     * 以 Array 的形式返回数据
//     */
//    static QArray array(RelationalPathBase<?> self) {
//        return QueryDslUtils.array(self.all())
//    }
//
//    /**
//     * 以 Array 的形式返回数据
//     */
//    static QArray array(Expression<?>[] self) {
//        return QueryDslUtils.array(self)
//    }
//
//    /**
//     * 以 Array 的形式返回数据
//     */
//    static QArray array(Collection<? extends Expression<?>> self) {
//        Expression<?>[] array = self.toArray(new Expression<?>[0])
//        return QueryDslUtils.array(array)
//    }
//
//    /**
//     * 以 List 的形式返回数据
//     */
//    static QList list(RelationalPathBase<?> self) {
//        return QueryDslUtils.list(self.all())
//    }
//
//    /**
//     * 以 List 的形式返回数据
//     */
//    static QList list(Expression<?>[] self) {
//        return QueryDslUtils.list(self)
//    }
//
//    /**
//     * 以 List 的形式返回数据
//     */
//    static QList list(Collection<? extends Expression<?>> self) {
//        Expression<?>[] array = self.toArray(new Expression<?>[0])
//        return QueryDslUtils.list(array)
//    }
//
//    /**
//     * 过滤、排序、分页查询
//     */
//    static <T> Page<T> queryByPage(SQLQuery<T> self) {
//        return QueryDslUtils.queryByPage(self)
//    }
//
//    /**
//     * 过滤查询(只增加过滤参数)
//     */
//    static <T> SQLQuery<T> queryByFilter(SQLQuery<T> self) {
//        return QueryDslUtils.queryByFilter(self)
//    }
//
//    /**
//     * 排序查询(只增加排序参数)
//     */
//    static <T> SQLQuery<T> queryBySort(SQLQuery<T> self) {
//        return QueryDslUtils.queryBySort(self)
//    }
//
//    /**
//     * 分页查询(只增加分页参数)
//     */
//    static <T> Page<T> queryByPage(SQLQuery<T> self, QueryByPage queryByPage) {
//        return QueryDslUtils.queryByPage(self, queryByPage)
//    }
//
//    /**
//     * 根据条件增加sql where条件
//     * @param when 指定的条件
//     * @param o sql where条件
//     */
//    static <T> SQLQuery<T> whereIf(SQLQuery<T> self, boolean when, Predicate... o) {
//        if (when) {
//            self.where(o)
//        }
//        return self
//    }
//
//    /**
//     * 根据条件增加sql where条件
//     * @param when 指定的条件
//     * @param o sql where条件
//     */
//    static <T> SQLQuery<T> whereIf(SQLQuery<T> self, boolean when, Predicate o) {
//        if (when) {
//            self.where(o)
//        }
//        return self
//    }
//
//    /**
//     * 根据条件增加sql where条件
//     * @param when 指定的条件
//     * @param o sql where条件
//     */
//    static <T> SQLQuery<T> whereIf(SQLQuery<T> self, boolean when, Supplier<? extends Predicate> o) {
//        if (when) {
//            Object where = o.get()
//            if (where != null) {
//                if (where instanceof Collection && !where.isEmpty() && where[0] instanceof Predicate) {
//                    self.where(where.toArray() as Predicate[])
//                } else if (where instanceof Predicate[] && where.length > 0) {
//                    self.where(where as Predicate[])
//                } else {
//                    self.where(where as Predicate)
//                }
//            }
//        }
//        return self
//    }
//
//    static SQLInsertClause setMap(SQLInsertClause self, Map<String, ?> valueMap) {
//        self.populate(valueMap, MapMapper.DEFAULT)
//        return self
//    }
//
//    static SQLInsertClause setMapIf(SQLInsertClause self, boolean when, Map<String, ?> valueMap) {
//        if (when) {
//            self.populate(valueMap, MapMapper.DEFAULT)
//        }
//        return self
//    }
//
//    static SQLInsertClause setx(SQLInsertClause self, Path<?> path, Object value) {
//        SQLClause.setx(self, path, value)
//        return self
//    }
//
//    static SQLInsertClause setxIf(SQLInsertClause self, boolean when, Path<?> path, Object value) {
//        if (when) {
//            self.setx(path, value)
//        }
//        return self
//    }
//
//    static <T> SQLInsertClause setIf(SQLInsertClause self, boolean when, Path<T> path, T value) {
//        if (when) {
//            self.set(path, value)
//        }
//        return self
//    }
//
//    static <T> SQLInsertClause setIf(SQLInsertClause self, boolean when, Path<T> path, Expression<? extends T> expression) {
//        if (when) {
//            self.set(path, expression)
//        }
//        return self
//    }
//
//    static <T> SQLInsertClause setNullIf(SQLInsertClause self, boolean when, Path<T> path) {
//        if (when) {
//            self.setNull(path)
//        }
//        return self
//    }
//
//    static SQLInsertClause autoFill(SQLInsertClause self, boolean autoFill) {
//        if (self instanceof SafeSQLInsertClause) {
//            ((SafeSQLInsertClause) self).autoFill(autoFill)
//        } else {
//            throw new UnsupportedOperationException("当前SQLInsertClause类型不支持autoFill操作: classType=${self.getClass().getName()}")
//        }
//        return self
//    }
//
//    static SQLUpdateClause setMap(SQLUpdateClause self, Map<String, ?> valueMap) {
//        self.populate(valueMap, MapMapper.DEFAULT)
//        return self
//    }
//
//    static SQLUpdateClause setMapIf(SQLUpdateClause self, boolean when, Map<String, ?> valueMap) {
//        if (when) {
//            self.populate(valueMap, MapMapper.DEFAULT)
//        }
//        return self
//    }
//
//    static SQLUpdateClause setx(SQLUpdateClause self, Path<?> path, Object value) {
//        SQLClause.setx(self, path, value)
//        return self
//    }
//
//    static SQLUpdateClause setxIf(SQLUpdateClause self, boolean when, Path<?> path, Object value) {
//        if (when) {
//            self.setx(path, value)
//        }
//        return self
//    }
//
//    static <T> SQLUpdateClause setIf(SQLUpdateClause self, boolean when, Path<T> path, T value) {
//        if (when) {
//            self.set(path, value)
//        }
//        return self
//    }
//
//    static <T> SQLUpdateClause setIf(SQLUpdateClause self, boolean when, Path<T> path, Expression<? extends T> expression) {
//        if (when) {
//            self.set(path, expression)
//        }
//        return self
//    }
//
//    static <T> SQLUpdateClause setNullIf(SQLUpdateClause self, boolean when, Path<T> path) {
//        if (when) {
//            self.setNull(path)
//        }
//        return self
//    }
//
//    static SQLUpdateClause whereIf(SQLUpdateClause self, boolean when, Predicate... o) {
//        if (when) {
//            self.where(o)
//        }
//        return self
//    }
//
//    static SQLUpdateClause whereIf(SQLUpdateClause self, boolean when, Supplier<? extends Predicate> o) {
//        if (when) {
//            Object where = o.get()
//            if (where != null) {
//                if (where instanceof Collection && !where.isEmpty() && where[0] instanceof Predicate) {
//                    self.where(where.toArray() as Predicate[])
//                } else if (where instanceof Predicate[] && where.length > 0) {
//                    self.where(where as Predicate[])
//                } else {
//                    self.where(where as Predicate)
//                }
//            }
//        }
//        return self
//    }
//
//    static SQLUpdateClause setIf(SQLUpdateClause self, boolean when, List<? extends Path<?>> paths, List<?> values) {
//        if (when) {
//            self.set(paths, values)
//        }
//        return self
//    }
//
//    static SQLUpdateClause autoFill(SQLUpdateClause self, boolean autoFill) {
//        if (self instanceof SafeSQLUpdateClause) {
//            ((SafeSQLUpdateClause) self).autoFill(autoFill)
//        } else {
//            throw new UnsupportedOperationException("当前SQLUpdateClause类型不支持autoFill操作: classType=${self.getClass().getName()}")
//        }
//        return self
//    }
//
//    static SQLDeleteClause whereIf(SQLDeleteClause self, boolean when, Predicate p) {
//        if (when) {
//            self.where(p)
//        }
//        return self
//    }
//
//    static SQLDeleteClause whereIf(SQLDeleteClause self, boolean when, Predicate... o) {
//        if (when) {
//            self.where(o)
//        }
//        return self
//    }
//
//    static SQLDeleteClause whereIf(SQLDeleteClause self, boolean when, Supplier<? extends Predicate> o) {
//        if (when) {
//            Object where = o.get()
//            if (where != null) {
//                if (where instanceof Collection && !where.isEmpty() && where[0] instanceof Predicate) {
//                    self.where(where.toArray() as Predicate[])
//                } else if (where instanceof Predicate[] && where.length > 0) {
//                    self.where(where as Predicate[])
//                } else {
//                    self.where(where as Predicate)
//                }
//            }
//        }
//        return self
//    }
//
//    static setByMapping(SQLInsertClause self, Map<String, Object> rowData, Map<String, String> propertyMapping) {
//        doMapping(rowData, propertyMapping)
//        self.populate(rowData, MapMapper.DEFAULT)
//        return self
//    }
//
//    static setByMapping(SQLUpdateClause self, Map<String, Object> rowData, Map<String, String> propertyMapping) {
//        doMapping(rowData, propertyMapping)
//        self.populate(rowData, MapMapper.DEFAULT)
//        return self
//    }
//
//    private static doMapping(Map<String, Object> rowData, Map<String, String> propertyMapping) {
//        // rowData = new LinkedHashMap<>(rowData)
//        for (final def entry in propertyMapping.entrySet()) {
//            String att = entry.getKey()
//            String field = entry.getValue()
//            if (rowData.containsKey(att)) {
//                Object value = rowData.get(att)
//                rowData.remove(att)
//                rowData.put(field, value)
//            }
//        }
//    }
//}
