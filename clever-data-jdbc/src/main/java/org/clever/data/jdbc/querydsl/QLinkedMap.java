package org.clever.data.jdbc.querydsl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpressionBase;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Visitor;
import com.querydsl.sql.RelationalPathBase;
import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.reflection.ReflectionsUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * querydsl查询返回LinkedHashMap支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/09 14:56 <br/>
 */
public class QLinkedMap extends FactoryExpressionBase<LinkedHashMap<String, ?>> {
    private final ArrayList<Expression<?>> args;
    private RenameStrategy renameStrategy;
    private final Map<Integer, String> columnNameCache = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public QLinkedMap(RenameStrategy renameStrategy, Expression<?>... args) {
        super((Class) LinkedHashMap.class);
        ArrayList<Expression<?>> paths = new ArrayList<>(args.length);
        for (Expression<?> arg : args) {
            if (arg instanceof RelationalPathBase) {
                paths.addAll(Arrays.asList(((RelationalPathBase<?>) arg).all()));
            } else {
                paths.add(arg);
            }
        }
        this.args = paths;
        this.renameStrategy = renameStrategy;
    }

    public QLinkedMap(Expression<?>... args) {
        this(RenameStrategy.ToUnderline, args);
    }

    public QLinkedMap add(Expression<?>... args) {
        this.args.addAll(Arrays.asList(args));
        return this;
    }

    public QLinkedMap setRenameStrategy(RenameStrategy renameStrategy) {
        columnNameCache.clear();
        if (renameStrategy == null) {
            renameStrategy = RenameStrategy.None;
        }
        this.renameStrategy = renameStrategy;
        return this;
    }

    @Override
    public List<Expression<?>> getArgs() {
        return Collections.unmodifiableList(args);
    }

    @Nullable
    @Override
    public LinkedHashMap<String, ?> newInstance(Object... args) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(args.length);
        for (int i = 0; i < args.length; i++) {
            final Expression<?> expression = this.args.get(i);
            final String columnName = columnNameCache.computeIfAbsent(i, idx -> {
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
            result.put(columnName, args[i]);
        }
        return result;
    }

    @Nullable
    @Override
    public <R, C> R accept(Visitor<R, C> v, @Nullable C context) {
        return v.visit(this, context);
    }
}
