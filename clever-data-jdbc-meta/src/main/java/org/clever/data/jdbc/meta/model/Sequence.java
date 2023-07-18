package org.clever.data.jdbc.meta.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.clever.util.Assert;

/**
 * 数据库 序列
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/18 13:40 <br/>
 */
@ToString(exclude = {"schema"})
@EqualsAndHashCode(callSuper = true)
@Data
public class Sequence extends AttributedObject {
    /**
     * 数据库 schema
     */
    private final Schema schema;
    /**
     * 序列名
     */
    private String name;
    /**
     * 序列的最小值
     */
    private Long minValue;
    /**
     * 序列的最大值
     */
    private Long maxValue;
    /**
     * 序列的增量值
     */
    private Long increment;
    /**
     * 是否循环
     */
    private boolean cycle;

    public Sequence(Schema schema) {
        Assert.notNull(schema, "参数 schema 不能为空");
        this.schema = schema;
    }

    public String getSchemaName() {
        if (schema == null) {
            return null;
        }
        return schema.getName();
    }
}
