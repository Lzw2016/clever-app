package org.clever.data.jdbc.querydsl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpressionBase;
import com.querydsl.core.types.Visitor;
import com.querydsl.sql.RelationalPathBase;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * querydsl查询返回数组支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/02/20 22:10 <br/>
 */
public class QArray extends FactoryExpressionBase<Object[]> {
    private final ArrayList<Expression<?>> args;

    public QArray(Expression<?>... args) {
        super(Object[].class);
        ArrayList<Expression<?>> paths = new ArrayList<>(args.length);
        for (Expression<?> arg : args) {
            if (arg instanceof RelationalPathBase) {
                paths.addAll(Arrays.asList(((RelationalPathBase<?>) arg).all()));
            } else {
                paths.add(arg);
            }
        }
        this.args = paths;
    }

    @Override
    public List<Expression<?>> getArgs() {
        return Collections.unmodifiableList(args);
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
