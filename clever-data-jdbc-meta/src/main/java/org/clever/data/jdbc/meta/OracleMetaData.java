package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.Schema;

import java.util.*;

/**
 * 获取数据库元数据 Oracle 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 15:53 <br/>
 */
public class OracleMetaData extends AbstractMetaData {
    private final Jdbc jdbc;

    public OracleMetaData(Jdbc jdbc) {
        this.jdbc = jdbc;
//        addIgnoreSchema("information_schema");
//        addIgnoreSchema("mysql");
//        addIgnoreSchema("performance_schema");
//        addIgnoreSchema("sys");
    }

    @Override
    public String currentSchema() {
        return StringUtils.lowerCase(jdbc.queryString("select sys_context('userenv', 'current_schema') from dual"));
    }

    @Override
    protected List<Schema> doGetSchemas(Collection<String> schemasName,
                                        Collection<String> tablesName,
                                        Set<String> ignoreSchemas,
                                        Set<String> ignoreTables,
                                        Set<String> ignoreTablesPrefix,
                                        Set<String> ignoreTablesSuffix) {
        // 所有的 Schema | Map<schemaName, Schema>
        final Map<String, Schema> mapSchema = new HashMap<>();
        return null;
    }
}
