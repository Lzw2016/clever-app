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
 * 作者：lizw <br/>
 * 创建时间：2022/02/20 22:10 <br/>
 */
public class QArray extends FactoryExpressionBase<Object[]> {
    private final ArrayList<Expression<?>> args;

    public QArray(Expression<?>... args) {
        super(Object[].class);
        this.args = new ArrayList<>(Arrays.asList(args));
    }

    @Override
    public List<Expression<?>> getArgs() {
        return ImmutableList.copyOf(args);
    }

    @Nullable
    @Override
    public Object[] newInstance(Object... args) {
        return args;
    }

    @Nullable
    @Override
    public <R, C> R accept(Visitor<R, C> v, @Nullable C context) {
        return v.visit(this, context);
    }
}
