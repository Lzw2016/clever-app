package org.clever.data.jdbc.querydsl;

import com.querydsl.core.types.Expression;
import org.clever.core.RenameStrategy;
import org.clever.core.mapper.BeanCopyUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * querydsl查询返回实体类支持
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/05/09 14:46 <br/>
 */
public class QEntity<T> extends AbstractQResult<T> {
    /**
     * @param type           数据行类型
     * @param renameStrategy 字段名重命名规则
     * @param exprs          需要查询的“表字段、表”
     */
    public QEntity(Class<? extends T> type, RenameStrategy renameStrategy, Expression<?>... exprs) {
        super(type, renameStrategy, exprs);
    }

    /**
     * 默认字段名重命名规则：“小写驼峰”
     *
     * @param type  数据行类型
     * @param exprs 需要查询的“表字段、表”
     */
    public QEntity(Class<? extends T> type, Expression<?>... exprs) {
        this(type, RenameStrategy.ToCamel, exprs);
    }

    @Override
    public @Nullable T newInstance(Object... args) {
        Map<String, Object> map = toMap(args);
        return BeanCopyUtils.toBean(map, this.clazz);
    }
}
