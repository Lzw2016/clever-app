package org.clever.data.jdbc.meta.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.clever.util.Assert;

/**
 * 数据库 table column 元数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 20:43 <br/>
 */
@ToString(exclude = {"table"})
@EqualsAndHashCode(callSuper = true)
@Data
public class Column extends AttributedObject {
    /**
     * 数据表
     */
    private final Table table;
    /**
     * 字段名
     */
    private String name;
    /**
     * 列注释
     */
    private String comment;
    /**
     * 是否为主键的一部分
     */
    private boolean partOfPrimaryKey;
    /**
     * 是否是索引的一部分
     */
    private boolean partOfIndex;
    /**
     * 是否是唯一索引的一部分
     */
    private boolean partOfUniqueIndex;
    /**
     * 是否自动增长
     */
    private boolean autoIncremented;
    /**
     * 不能为空
     */
    private boolean notNull;
    /**
     * 字段类型
     */
    private String dataType;
    /**
     * 字段大小(char、date、numeric、decimal)
     */
    private int size;
    /**
     * 小数位数
     */
    private int decimalDigits;
    /**
     * string类型的长度
     */
    private int width;
    /**
     * 默认值
     */
    private String defaultValue;
    /**
     * 顺序位
     */
    private int ordinalPosition;

    public Column(Table table) {
        Assert.notNull(table, "参数 table 不能为空");
        this.table = table;
    }

    public String getTableName() {
        if (table == null) {
            return null;
        }
        return table.getName();
    }
}
