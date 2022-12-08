package org.clever.data.jdbc.querydsl;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpressionBase;
import com.querydsl.core.types.Visitor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * querydsl查询返回ArrayList支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/02/21 09:44 <br/>
 */
public class QList extends FactoryExpressionBase<ArrayList<?>> {
    private final ArrayList<Expression<?>> args;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public QList(Expression<?>... args) {
        super((Class) ArrayList.class);
        this.args = new ArrayList<>(Arrays.asList(args));
    }

    @Override
    public List<Expression<?>> getArgs() {
        return ImmutableList.copyOf(args);
    }

    @Nullable
    @Override
    public ArrayList<?> newInstance(Object... args) {
        return new ArrayList<>(Arrays.asList(args));
    }

    @Nullable
    @Override
    public <R, C> R accept(Visitor<R, C> v, @Nullable C context) {
        return v.visit(this, context);
    }
}
