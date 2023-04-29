package org.clever.data.jdbc.meta;

import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;

import java.util.Collection;
import java.util.List;

/**
 * 获取数据库元数据接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 19:56 <br/>
 */
public interface DataBaseMetaData {
    /**
     * 当前的Jdbc对象
     */
    Jdbc getJdbc();

    /**
     * 获取数据库连接默认的Schema名
     */
    String currentSchema();

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

//String getDiffDDL

//表
//字段
//主键
//索引(唯一索引)
//序列
//存储过程(函数)
//
//删除、创建、变更
//
//DropTableDDL
//CreateTableDDL
//ModifyTableDDL
//RenameTableDDL
//ModifyIdxDDL
//ModifyProcedureSQL
//
//getTableIndex
//getProcedure
//
//executeModifyProcedureSQL
//
//executeDDL
//
//DataSync
}
