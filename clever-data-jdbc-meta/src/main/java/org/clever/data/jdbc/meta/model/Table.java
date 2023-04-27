package org.clever.data.jdbc.meta.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 table 元数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 20:05 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Table extends AttributedObject {
    /**
     * 数据库 schema
     */
    private final Schema schema;
    /**
     * 数据表名
     */
    private String name;
    /**
     * 数据表描述
     */
    private String comment;
    /**
     * 数据表字段
     */
    private final List<Column> columns = new ArrayList<>();

    // TODO 管理 primaryKey
    // TODO 管理 index uniqueIndex

    public Table(Schema schema) {
        Assert.notNull(schema, "参数 schema 不能为空");
        this.schema = schema;
    }

    public String getSchemaName() {
        return schema.getName();
    }

    public void addColumn(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        columns.add(column);
    }

//    getPrimaryKey
//    getIndexes
}
