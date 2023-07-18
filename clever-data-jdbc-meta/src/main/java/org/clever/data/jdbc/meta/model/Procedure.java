package org.clever.data.jdbc.meta.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.clever.util.Assert;

/**
 * 数据库 存储过程或函数
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 14:29 <br/>
 */
@ToString(exclude = {"schema"})
@EqualsAndHashCode(callSuper = true)
@Data
public class Procedure extends AttributedObject {
    /**
     * 数据库 schema
     */
    @JsonIgnore
    private final Schema schema;
    /**
     * 数据表名
     */
    private String name;
    /**
     * 是否是函数
     */
    private boolean function;
    /**
     * 存储过程或函数 的定义文本
     */
    private String definition;

    public Procedure(Schema schema) {
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
