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

    /**
     * 当前的Jdbc对象
     */
    Jdbc getJdbc();

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
     * 获取指定的 Table
     */
    Table getTable(String schemaName, String tableName);

    // --------------------------------------------------------------------------------------------
    //  表结构变更 DDL 语句
    // --------------------------------------------------------------------------------------------

    /***
     * 修改“表”的 DDL 语句
     */
    String diffTable(Table newTable, Table oldTable);

    /**
     * 删除“表”的 DDL 语句
     */
    String delTable(Table oldTable);

    /**
     * 新增“表”的 DDL 语句
     */
    String addTable(Table newTable);

    /***
     * 修改“字段”的 DDL 语句
     */
    String diffColumn(Column newColumn, Column oldColumn);

    /***
     * 删除“字段”的 DDL 语句
     */
    String delColumn(Column oldColumn);

    /***
     * 新增“字段”的 DDL 语句
     */
    String addColumn(Column newColumn);

    /***
     * 修改“主键”的 DDL 语句
     */
    String diffPrimaryKey(PrimaryKey newPrimaryKey, PrimaryKey oldPrimaryKey);

    /***
     * 删除“主键”的 DDL 语句
     */
    String delPrimaryKey(PrimaryKey oldPrimaryKey);

    /***
     * 新增“主键”的 DDL 语句
     */
    String addPrimaryKey(PrimaryKey newPrimaryKey);

    /***
     * 修改“索引”的 DDL 语句
     */
    String diffIndex(Index newIndex, Index oldIndex);

    /***
     * 删除“索引”的 DDL 语句
     */
    String delIndex(Index oldIndex);

    /***
     * 新增“索引”的 DDL 语句
     */
    String addIndex(Index newIndex);

//String getDiffDDL
//删除、创建、变更
//
//DropTableDDL
//CreateTableDDL
//ModifyTableDDL
//RenameTableDDL
//ModifyIdxDDL
//ModifyProcedureSQL
//executeModifyProcedureSQL
//executeDDL
//DataSync
}
