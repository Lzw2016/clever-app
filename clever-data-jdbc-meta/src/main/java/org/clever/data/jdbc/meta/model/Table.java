package org.clever.data.jdbc.meta.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.clever.core.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 table 元数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 20:05 <br/>
 */
@ToString(exclude = {"schema"})
@EqualsAndHashCode(callSuper = true)
@Data
public class Table extends AttributedObject {
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
     * 数据表描述
     */
    private String comment;
    /**
     * 数据表字段
     */
    private final List<Column> columns = new ArrayList<>();
    /**
     * 表的主键
     */
    private PrimaryKey primaryKey;
    /**
     * 表的索引(不包含主键索引)
     */
    private final List<Index> indices = new ArrayList<>();

    public Table(Schema schema) {
        Assert.notNull(schema, "参数 schema 不能为空");
        this.schema = schema;
    }

    public String getSchemaName() {
        if (schema == null) {
            return null;
        }
        return schema.getName();
    }

    public void addColumn(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        columns.add(column);
    }

    public Column getColumn(String columnName) {
        if (columnName == null) {
            return null;
        }
        return columns.stream()
                .filter(column -> columnName.equalsIgnoreCase(column.getName()))
                .findFirst().orElse(null);
    }

    public void addIndex(Index index) {
        Assert.notNull(index, "参数 index 不能为空");
        indices.add(index);
    }

    public Index getIndex(String indexName) {
        if (indexName == null) {
            return null;
        }
        return indices.stream()
                .filter(index -> indexName.equalsIgnoreCase(index.getName()))
                .findFirst().orElse(null);
    }
}
