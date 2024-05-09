package org.clever.data.jdbc.querydsl;

import com.querydsl.core.types.Expression;
import org.clever.core.RenameStrategy;
import org.jetbrains.annotations.Nullable;

/**
 * querydsl查询返回数组支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/02/20 22:10 <br/>
 */
public class QArray extends AbstractQResult<Object[]> {
    /**
     * @param exprs 需要查询的“表字段、表”
     */
    public QArray(Expression<?>... exprs) {
        super(Object[].class, RenameStrategy.None, exprs);
    }

    @Nullable
    @Override
    public Object[] newInstance(Object... args) {
        return args;
    }
}
