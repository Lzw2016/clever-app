package org.clever.data.jdbc.meta.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.Assert;
import org.clever.data.dynamic.sql.dialect.DbType;

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
     * 数据库版本
     */
    private String version;
    /**
     * 数据库 schema 名称
     */
    private String name;
    /**
     * 数据库表
     */
    private final List<Table> tables = new ArrayList<>();
    /**
     * 序列信息
     */
    private final List<Sequence> sequences = new ArrayList<>();
    /**
     * 存储过程&函数
     */
    private final List<Procedure> procedures = new ArrayList<>();

    public Schema(DbType dbType) {
        Assert.notNull(dbType, "参数 dbType 不能为空");
        this.dbType = dbType;
    }

    public Schema(DbType dbType, String version, String name) {
        this(dbType);
        this.version = version;
        this.name = name;
    }

    public void addTable(Table table) {
        Assert.notNull(table, "参数 table 不能为空");
        tables.add(table);
    }

    public Table getTable(String tableName) {
        if (tableName == null) {
            return null;
        }
        return tables.stream()
            .filter(table -> tableName.equalsIgnoreCase(table.getName()))
            .findFirst().orElse(null);
    }

    public void addSequence(Sequence sequence) {
        Assert.notNull(sequence, "参数 sequence 不能为空");
        sequences.add(sequence);
    }

    public Sequence getSequence(String sequenceName) {
        if (sequenceName == null) {
            return null;
        }
        return sequences.stream()
            .filter(sequence -> sequenceName.equalsIgnoreCase(sequence.getName()))
            .findFirst().orElse(null);
    }

    public void addProcedure(Procedure procedure) {
        Assert.notNull(procedure, "参数 procedure 不能为空");
        procedures.add(procedure);
    }

    public Procedure getProcedure(String procedureName) {
        if (procedureName == null) {
            return null;
        }
        return procedures.stream()
            .filter(procedure -> procedureName.equalsIgnoreCase(procedure.getName()))
            .findFirst().orElse(null);
    }
}
