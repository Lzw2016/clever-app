package org.clever.data.jdbc.querydsl;

import com.querydsl.core.types.Expression;
import org.clever.core.RenameStrategy;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * querydsl查询返回ArrayList支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/02/21 09:44 <br/>
 */
public class QList extends AbstractQResult<ArrayList<Object>> {
    /**
     * @param exprs 需要查询的“表字段、表”
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public QList(Expression<?>... exprs) {
        super((Class) ArrayList.class, RenameStrategy.None, exprs);
    }

    @Nullable
    @Override
    public ArrayList<Object> newInstance(Object... args) {
        return new ArrayList<>(Arrays.asList(args));
    }
}
