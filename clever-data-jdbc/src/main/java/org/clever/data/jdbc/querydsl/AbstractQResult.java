package org.clever.data.jdbc.querydsl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpressionBase;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Visitor;
import com.querydsl.sql.RelationalPath;
import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.reflection.ReflectionsUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * QueryDSL查询返回值工厂基类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/05/08 18:13 <br/>
 */
public abstract class AbstractQResult<T> extends FactoryExpressionBase<T> {
    /**
     * 数据行类型
     */
    protected final Class<? extends T> clazz;
    /**
     * 查询的列
     */
    protected final List<Expression<?>> columns;
    /**
     * 字段名重命名规则
     */
    protected RenameStrategy renameStrategy;
    /**
     * 字段名缓存
     */
    protected final Map<Integer, String> columnNameCache = new HashMap<>();

    /**
     * @param type           数据行类型
     * @param renameStrategy 字段名重命名规则
     * @param exprs          需要查询的“表字段、表”
     */
    public AbstractQResult(Class<? extends T> type, RenameStrategy renameStrategy, Expression<?>... exprs) {
        super(type);
        this.clazz = type;
        this.columns = flattenColumns(exprs);
        this.renameStrategy = renameStrategy;
    }

    /**
     * 增加字段
     *
     * @param exprs 需要查询的“表字段、表”
     */
    public AbstractQResult<T> addColumns(Expression<?>... exprs) {
        this.columns.addAll(flattenColumns(exprs));
        return this;
    }

    /**
     * 设置字段名重命名规则
     *
     * @param renameStrategy 字段名重命名规则
     */
    public AbstractQResult<T> setRenameStrategy(RenameStrategy renameStrategy) {
        columnNameCache.clear();
        if (renameStrategy == null) {
            renameStrategy = RenameStrategy.None;
        }
        this.renameStrategy = renameStrategy;
        return this;
    }

    @Override
    public List<Expression<?>> getArgs() {
        return Collections.unmodifiableList(columns);
    }

    @Nullable
    @Override
    public <R, C> R accept(Visitor<R, C> v, @Nullable C context) {
        return v.visit(this, context);
    }

    /**
     * 拍平数据库查询字段
     *
     * @param exprs 表字段、表
     * @return 所有的表字段
     */
    protected List<Expression<?>> flattenColumns(Expression<?>... exprs) {
        if (exprs == null) {
            return Collections.emptyList();
        }
        List<Expression<?>> columns = new ArrayList<>(exprs.length);
        for (Expression<?> expr : exprs) {
            if (expr instanceof RelationalPath) {
                RelationalPath<?> table = (RelationalPath<?>) expr;
                columns.addAll(table.getColumns());
            } else {
                columns.add(expr);
            }
        }
        return columns;
    }

    /**
     * 字段重命名
     *
     * @param expression 字段
     * @param columnIdx  字段索引位置
     */
    protected String renameColumn(Expression<?> expression, int columnIdx) {
        return columnNameCache.computeIfAbsent(columnIdx, idx -> {
            String name = String.format("__%s", (idx + 1));
            try {
                if (expression instanceof Path) {
                    name = ((Path<?>) expression).getMetadata().getName();
                } else {
                    Object mixin = ReflectionsUtils.getFieldValue(expression, "mixin");
                    if (mixin != null) {
                        Object cols = ReflectionsUtils.getFieldValue(mixin, "args");
                        if (cols instanceof Collection) {
                            Object[] colArr = ((Collection<?>) cols).toArray();
                            if (colArr.length > 0) {
                                Object col = colArr[colArr.length - 1];
                                if (col instanceof Path) {
                                    name = ((Path<?>) col).getMetadata().getName();
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return NamingUtils.rename(name, renameStrategy);
        });
    }

    /**
     * 数据行转换成Map
     * @param args 数据行
     */
    protected LinkedHashMap<String, Object> toMap(Object... args) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(args.length);
        for (int columnIdx = 0; columnIdx < args.length; columnIdx++) {
            final Expression<?> expression = this.columns.get(columnIdx);
            final String columnName = renameColumn(expression, columnIdx);
            final Object columnValue = args[columnIdx];
            map.put(columnName, columnValue);
        }
        return map;
    }
}
