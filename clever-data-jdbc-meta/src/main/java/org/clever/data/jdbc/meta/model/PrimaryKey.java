package org.clever.data.jdbc.meta.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库表主键
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 13:28 <br/>
 */
@ToString(exclude = {"table"})
@EqualsAndHashCode(callSuper = true)
@Data
public class PrimaryKey extends AttributedObject {
    /**
     * 数据表
     */
    @JsonIgnore
    private final Table table;
    /**
     * 主键名称
     */
    private String name;
    /**
     * 数据表字段(有序)
     */
    private final List<Column> columns = new ArrayList<>();

    public PrimaryKey(Table table) {
        this.table = table;
    }

    public String getTableName() {
        if (table == null) {
            return null;
        }
        return table.getName();
    }

    public void addColumn(Column column) {
        Assert.notNull(column, "参数 column 不能为空");
        columns.add(column);
    }
}
