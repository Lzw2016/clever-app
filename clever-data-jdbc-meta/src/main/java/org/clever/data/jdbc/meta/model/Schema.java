package org.clever.data.jdbc.meta.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 schema 元数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 20:26 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Schema extends AttributedObject {
    /**
     * 数据库类型
     */
    private final DbType dbType;
    /**
     * 数据库 schema 名称
     */
    private String name;
    /**
     * 数据库表
     */
    private final List<Table> tables = new ArrayList<>();

    // TODO 管理 存储过程 函数

    public Schema(DbType dbType) {
        Assert.notNull(dbType, "参数 dbType 不能为空");
        this.dbType = dbType;
    }

    public void addTable(Table table) {
        Assert.notNull(table, "参数 table 不能为空");
        tables.add(table);
    }
}
