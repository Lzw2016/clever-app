package org.clever.data.jdbc.querydsl;

import com.querydsl.core.types.Expression;
import org.clever.core.RenameStrategy;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

/**
 * querydsl查询返回LinkedHashMap支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/09 14:56 <br/>
 */
public class QLinkedMap extends AbstractQResult<LinkedHashMap<String, Object>> {
    /**
     * @param renameStrategy 字段名重命名规则
     * @param exprs          需要查询的“表字段、表”
     */
    public QLinkedMap(RenameStrategy renameStrategy, Expression<?>... exprs) {
        // noinspection unchecked,rawtypes
        super((Class) LinkedHashMap.class, renameStrategy, exprs);
    }

    /**
     * 默认字段名重命名规则：“全小写下划线”
     *
     * @param exprs 需要查询的“表字段、表”
     */
    public QLinkedMap(Expression<?>... exprs) {
        this(RenameStrategy.ToUnderline, exprs);
    }

    @Nullable
    @Override
    public LinkedHashMap<String, Object> newInstance(Object... args) {
        return toMap(args);
    }
}
