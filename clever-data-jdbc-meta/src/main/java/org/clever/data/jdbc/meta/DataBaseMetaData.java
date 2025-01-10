package org.clever.data.jdbc.meta;

import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;

import java.util.Collection;
import java.util.List;

/**
 * 获取数据库元数据接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 19:56 <br/>
 */
public interface DataBaseMetaData {
    String LINE = "\n";
    String TAB = "    ";

    /**
     * 当前的Jdbc对象
     */
    Jdbc getJdbc();

    /**
     * 获取当前数据的版本
     */
    String getVersion();
    
    /**
     * 获取数据库连接默认的Schema名
     */
    String currentSchema();

    // --------------------------------------------------------------------------------------------
    //  表结构元数据
    // --------------------------------------------------------------------------------------------

    /**
     * 获取数据库元数据
     *
     * @param schemasName 指定的 Schema 集合，不指定就获取所有的 Schema
     * @param tablesName  指定的 Table 集合，不指定就获取所有的 Table
     */
    List<Schema> getSchemas(Collection<String> schemasName, Collection<String> tablesName);

    /**
     * 获取当前库的所有 Schema
     */
    List<Schema> getSchemas();

    /**
     * 获取指定的 Schema
     */
    Schema getSchema(String schemaName);

    /**
     * 获取当前的 Schema
     */
    Schema getSchema();

    /**
     * 获取指定的 Schema
     */
    Schema getSchema(String schemaName, Collection<String> tablesName);

    /**
     * 获取指定的 Table
     */
    Table getTable(String schemaName, String tableName);

    // --------------------------------------------------------------------------------------------
    //  表结构变更 DDL 语句
    // --------------------------------------------------------------------------------------------

    /***
     * 修改“表”的 DDL 语句(包含: 表名、表备注、字段、主键、索引)
     */
    String alterTable(Table newTable, Table oldTable);

    /**
     * 删除“表”的 DDL 语句
     */
    String dropTable(Table oldTable);

    /**
     * 新增“表”的 DDL 语句(包含: 表名、表备注、字段、主键、索引)
     */
    String createTable(Table newTable);

    /***
     * 修改“字段”的 DDL 语句
     */
    String alterColumn(Column newColumn, Column oldColumn);

    /***
     * 删除“字段”的 DDL 语句
     */
    String dropColumn(Column oldColumn);

    /***
     * 新增“字段”的 DDL 语句
     */
    String createColumn(Column newColumn);

    /***
     * 修改“主键”的 DDL 语句
     */
    String alterPrimaryKey(PrimaryKey newPrimaryKey, PrimaryKey oldPrimaryKey);

    /***
     * 删除“主键”的 DDL 语句
     */
    String dropPrimaryKey(PrimaryKey oldPrimaryKey);

    /***
     * 新增“主键”的 DDL 语句
     */
    String createPrimaryKey(PrimaryKey newPrimaryKey);

    /***
     * 修改“索引”的 DDL 语句
     */
    String alterIndex(Index newIndex, Index oldIndex);

    /***
     * 删除“索引”的 DDL 语句
     */
    String dropIndex(Index oldIndex);

    /***
     * 新增“索引”的 DDL 语句
     */
    String createIndex(Index newIndex);

    /**
     * 字段顺序调整
     */
    String updateColumnPosition(Table newTable);

    // --------------------------------------------------------------------------------------------
    //  其它(序列、存储过程、函数)元数据 DDL 语句
    // --------------------------------------------------------------------------------------------

    /**
     * 修改“序列”的 DDL 语句
     */
    String alterSequence(Sequence newSequence, Sequence oldSequence);

    /**
     * 删除“序列”的 DDL 语句
     */
    String dropSequence(Sequence oldSequence);

    /**
     * 新增“序列”的 DDL 语句
     */
    String createSequence(Sequence newSequence);

    /**
     * 删除“存储过程、函数”的 DDL 语句
     */
    String dropProcedure(Procedure oldProcedure);

    /**
     * 新增“存储过程、函数”的 DDL 语句
     */
    String createProcedure(Procedure newProcedure);
}
