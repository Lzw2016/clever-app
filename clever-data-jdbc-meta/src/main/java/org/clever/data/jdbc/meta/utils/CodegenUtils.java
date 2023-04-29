package org.clever.data.jdbc.meta.utils;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import com.jfinal.template.expr.ast.MethodKit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.NamingUtils;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.AbstractMetaData;
import org.clever.data.jdbc.meta.DataBaseMetaData;
import org.clever.data.jdbc.meta.codegen.CodegenCodeConfig;
import org.clever.data.jdbc.meta.codegen.CodegenType;
import org.clever.data.jdbc.meta.codegen.EntityModel;
import org.clever.data.jdbc.meta.codegen.EntityPropertyModel;
import org.clever.data.jdbc.meta.codegen.handler.*;
import org.clever.data.jdbc.meta.codegen.support.QueryDSLSupport;
import org.clever.data.jdbc.meta.model.Column;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;
import org.clever.data.jdbc.support.DbColumnMetaData;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生成代码 <br/>
 * 1. 生成QueryDSL实体类<br/>
 * 2. 生成QueryDSL查询类<br/>
 * 3. 生成数据库文档(html、word、markdown)<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 21:34 <br/>
 */
@Slf4j
public class CodegenUtils {
    // 模版引擎
    public static final Engine ENGINE;
    public static final Map<CodegenType, CodegenHandler> CODEGEN_HANDLER_MAP = new HashMap<>();

    // 类型需要的 import package
    public static final Map<String, String> TYPE_PACKAGE = new HashMap<>();
    // jdbc类型映射 Map<java.sql.Types, String> | https://www.dbvisitor.net/docs/guides/types/java-jdbc
    public static final Map<Integer, String> JDBC_TYPES_MAPPING = new HashMap<>();
    // 字段类型名称 Map<java.sql.Types, java.sql.TypesName>
    public static final Map<Integer, String> JDBC_TYPES_NAME_MAPPING = new HashMap<>();

    static {
        final ClassLoader classLoader = CodegenUtils.class.getClassLoader();
        final boolean java8time = ClassUtils.isPresent("java.time.LocalTime", classLoader);
        // 配置模版引擎
        Engine.setChineseExpression(true);
        MethodKit.removeForbiddenClass(Class.class);
        Engine.setFastMode(true);
        ENGINE = Engine.create("Codegen");
        ENGINE.setDevMode(true);
        ENGINE.setStaticFieldExpression(true);
        ENGINE.setStaticMethodExpression(true);
        ENGINE.addSharedStaticMethod(NamingUtils.class);
        ENGINE.addSharedStaticMethod(QueryDSLSupport.class);
        // ENGINE.addSharedObject();
        ENGINE.setBaseTemplatePath(null);
        ENGINE.setToClassPathSourceFactory();
        // 配置模版
        CODEGEN_HANDLER_MAP.put(CodegenType.JAVA_ENTITY, new CodegenJavaEntity());
        CODEGEN_HANDLER_MAP.put(CodegenType.GROOVY_ENTITY, new CodegenGroovyEntity());
        CODEGEN_HANDLER_MAP.put(CodegenType.KOTLIN_ENTITY, new CodegenKotlinEntity());
        CODEGEN_HANDLER_MAP.put(CodegenType.JAVA_QUERYDSL, new CodegenJavaQueryDSL());
        CODEGEN_HANDLER_MAP.put(CodegenType.GROOVY_QUERYDSL, new CodegenGroovyQueryDSL());
        CODEGEN_HANDLER_MAP.put(CodegenType.KOTLIN_QUERYDSL, new CodegenKotlinQueryDSL());
        // 类型需要的 import package
        TYPE_PACKAGE.put("Boolean", "java.lang.Boolean");
        TYPE_PACKAGE.put("Byte", "java.lang.Byte");
        TYPE_PACKAGE.put("Short", "java.lang.Short");
        TYPE_PACKAGE.put("Integer", "java.lang.Integer");
        TYPE_PACKAGE.put("Long", "java.lang.Long");
        TYPE_PACKAGE.put("Float", "java.lang.Float");
        TYPE_PACKAGE.put("Double", "java.lang.Double");
        TYPE_PACKAGE.put("Character", "java.lang.Character");
        TYPE_PACKAGE.put("String", "java.lang.String");
        TYPE_PACKAGE.put("Void", "java.lang.Void");
        TYPE_PACKAGE.put("Object", "java.lang.Object");
        TYPE_PACKAGE.put("Date", "java.util.Date");
        TYPE_PACKAGE.put("Time", "java.sql.Time");
        TYPE_PACKAGE.put("BigDecimal", "java.math.BigDecimal");
        TYPE_PACKAGE.put("URL", "java.net.URL");
        if (java8time) {
            TYPE_PACKAGE.put("LocalTime", "java.time.LocalTime");
            TYPE_PACKAGE.put("OffsetTime", "java.time.OffsetTime");
            TYPE_PACKAGE.put("OffsetDateTime", "java.time.OffsetDateTime");
            TYPE_PACKAGE.put("ZonedDateTime", "java.time.ZonedDateTime");
        }
        // jdbc类型映射
        JDBC_TYPES_MAPPING.put(Types.BIT, "Boolean");
        JDBC_TYPES_MAPPING.put(Types.TINYINT, "Byte");
        JDBC_TYPES_MAPPING.put(Types.SMALLINT, "Short");
        JDBC_TYPES_MAPPING.put(Types.INTEGER, "Integer");
        JDBC_TYPES_MAPPING.put(Types.BIGINT, "Long");
        JDBC_TYPES_MAPPING.put(Types.FLOAT, "Float");
        JDBC_TYPES_MAPPING.put(Types.REAL, "BigDecimal");
        JDBC_TYPES_MAPPING.put(Types.DOUBLE, "Double");
        JDBC_TYPES_MAPPING.put(Types.NUMERIC, "BigDecimal");
        JDBC_TYPES_MAPPING.put(Types.DECIMAL, "BigDecimal");
        JDBC_TYPES_MAPPING.put(Types.CHAR, "Character");
        JDBC_TYPES_MAPPING.put(Types.VARCHAR, "String");
        JDBC_TYPES_MAPPING.put(Types.LONGVARCHAR, "String");
        JDBC_TYPES_MAPPING.put(Types.DATE, "Date");
        JDBC_TYPES_MAPPING.put(Types.TIME, "LocalTime");
        JDBC_TYPES_MAPPING.put(Types.TIMESTAMP, "Date");
        JDBC_TYPES_MAPPING.put(Types.BINARY, "byte[]");
        JDBC_TYPES_MAPPING.put(Types.VARBINARY, "byte[]");
        JDBC_TYPES_MAPPING.put(Types.LONGVARBINARY, "byte[]");
        JDBC_TYPES_MAPPING.put(Types.NULL, "Void");
        JDBC_TYPES_MAPPING.put(Types.OTHER, "Object");
        JDBC_TYPES_MAPPING.put(Types.JAVA_OBJECT, "Object");
        JDBC_TYPES_MAPPING.put(Types.DISTINCT, "");
        JDBC_TYPES_MAPPING.put(Types.STRUCT, "");
        JDBC_TYPES_MAPPING.put(Types.ARRAY, "Object[]");
        JDBC_TYPES_MAPPING.put(Types.BLOB, "byte[]");
        JDBC_TYPES_MAPPING.put(Types.CLOB, "String");
        JDBC_TYPES_MAPPING.put(Types.REF, "");
        JDBC_TYPES_MAPPING.put(Types.DATALINK, "URL");
        JDBC_TYPES_MAPPING.put(Types.BOOLEAN, "Boolean");
        JDBC_TYPES_MAPPING.put(Types.ROWID, "Long");
        JDBC_TYPES_MAPPING.put(Types.NCHAR, "Character");
        JDBC_TYPES_MAPPING.put(Types.NVARCHAR, "String");
        JDBC_TYPES_MAPPING.put(Types.LONGNVARCHAR, "String");
        JDBC_TYPES_MAPPING.put(Types.NCLOB, "String");
        JDBC_TYPES_MAPPING.put(Types.SQLXML, "");
        JDBC_TYPES_MAPPING.put(Types.REF_CURSOR, "");
        if (java8time) {
            JDBC_TYPES_MAPPING.put(Types.TIME_WITH_TIMEZONE, "OffsetTime");
            JDBC_TYPES_MAPPING.put(Types.TIMESTAMP_WITH_TIMEZONE, "ZonedDateTime");
        } else {
            JDBC_TYPES_MAPPING.put(Types.TIME_WITH_TIMEZONE, "Date");
            JDBC_TYPES_MAPPING.put(Types.TIMESTAMP_WITH_TIMEZONE, "Date");
        }
        // 字段类型名称
        JDBC_TYPES_NAME_MAPPING.put(Types.BIT, "Types.BIT");
        JDBC_TYPES_NAME_MAPPING.put(Types.TINYINT, "Types.TINYINT");
        JDBC_TYPES_NAME_MAPPING.put(Types.SMALLINT, "Types.SMALLINT");
        JDBC_TYPES_NAME_MAPPING.put(Types.INTEGER, "Types.INTEGER");
        JDBC_TYPES_NAME_MAPPING.put(Types.BIGINT, "Types.BIGINT");
        JDBC_TYPES_NAME_MAPPING.put(Types.FLOAT, "Types.FLOAT");
        JDBC_TYPES_NAME_MAPPING.put(Types.REAL, "Types.REAL");
        JDBC_TYPES_NAME_MAPPING.put(Types.DOUBLE, "Types.DOUBLE");
        JDBC_TYPES_NAME_MAPPING.put(Types.NUMERIC, "Types.NUMERIC");
        JDBC_TYPES_NAME_MAPPING.put(Types.DECIMAL, "Types.DECIMAL");
        JDBC_TYPES_NAME_MAPPING.put(Types.CHAR, "Types.CHAR");
        JDBC_TYPES_NAME_MAPPING.put(Types.VARCHAR, "Types.VARCHAR");
        JDBC_TYPES_NAME_MAPPING.put(Types.LONGVARCHAR, "Types.LONGVARCHAR");
        JDBC_TYPES_NAME_MAPPING.put(Types.DATE, "Types.DATE");
        JDBC_TYPES_NAME_MAPPING.put(Types.TIME, "Types.TIME");
        JDBC_TYPES_NAME_MAPPING.put(Types.TIMESTAMP, "Types.TIMESTAMP");
        JDBC_TYPES_NAME_MAPPING.put(Types.BINARY, "Types.BINARY");
        JDBC_TYPES_NAME_MAPPING.put(Types.VARBINARY, "Types.VARBINARY");
        JDBC_TYPES_NAME_MAPPING.put(Types.LONGVARBINARY, "Types.LONGVARBINARY");
        JDBC_TYPES_NAME_MAPPING.put(Types.NULL, "Types.NULL");
        JDBC_TYPES_NAME_MAPPING.put(Types.OTHER, "Types.OTHER");
        JDBC_TYPES_NAME_MAPPING.put(Types.JAVA_OBJECT, "Types.JAVA_OBJECT");
        JDBC_TYPES_NAME_MAPPING.put(Types.DISTINCT, "Types.DISTINCT");
        JDBC_TYPES_NAME_MAPPING.put(Types.STRUCT, "Types.STRUCT");
        JDBC_TYPES_NAME_MAPPING.put(Types.ARRAY, "Types.ARRAY");
        JDBC_TYPES_NAME_MAPPING.put(Types.BLOB, "Types.BLOB");
        JDBC_TYPES_NAME_MAPPING.put(Types.CLOB, "Types.CLOB");
        JDBC_TYPES_NAME_MAPPING.put(Types.REF, "Types.REF");
        JDBC_TYPES_NAME_MAPPING.put(Types.DATALINK, "Types.DATALINK");
        JDBC_TYPES_NAME_MAPPING.put(Types.BOOLEAN, "Types.BOOLEAN");
        JDBC_TYPES_NAME_MAPPING.put(Types.ROWID, "Types.ROWID");
        JDBC_TYPES_NAME_MAPPING.put(Types.NCHAR, "Types.NCHAR");
        JDBC_TYPES_NAME_MAPPING.put(Types.NVARCHAR, "Types.NVARCHAR");
        JDBC_TYPES_NAME_MAPPING.put(Types.LONGNVARCHAR, "Types.LONGNVARCHAR");
        JDBC_TYPES_NAME_MAPPING.put(Types.NCLOB, "Types.NCLOB");
        JDBC_TYPES_NAME_MAPPING.put(Types.SQLXML, "Types.SQLXML");
        JDBC_TYPES_NAME_MAPPING.put(Types.REF_CURSOR, "Types.REF_CURSOR");
        JDBC_TYPES_NAME_MAPPING.put(Types.TIME_WITH_TIMEZONE, "Types.TIME_WITH_TIMEZONE");
        JDBC_TYPES_NAME_MAPPING.put(Types.TIMESTAMP_WITH_TIMEZONE, "Types.TIMESTAMP_WITH_TIMEZONE");
        JDBC_TYPES_NAME_MAPPING.put(-102, "Types.TIMESTAMP_WITH_TIMEZONE");
    }

    /**
     * 生成代码
     *
     * @param jdbc   数据源
     * @param config 配置
     */
    @SneakyThrows
    public static void genCode(Jdbc jdbc, CodegenCodeConfig config) {
        Assert.notNull(jdbc, "参数 jdbc 不能为null");
        Assert.notNull(config, "参数 config 不能为null");
        AbstractMetaData metaData = MetaDataUtils.createMetaData(jdbc);
        config.getIgnoreSchemas().forEach(metaData::addIgnoreSchema);
        config.getIgnoreTables().forEach(metaData::addIgnoreTable);
        config.getIgnoreTablesPrefix().forEach(metaData::addIgnoreTablePrefix);
        config.getIgnoreTablesSuffix().forEach(metaData::addIgnoreTableSuffix);
        List<Schema> schemas = metaData.getSchemas(config.getSchemas(), config.getTables());
        final Set<String> tablesPrefix = config.getTablesPrefix();
        final Set<String> tablesSuffix = config.getTablesSuffix();
        for (Schema schema : schemas) {
            List<Table> tables = schema.getTables();
            for (Table table : tables) {
                String tableName = table.getName();
                // List<Column> columns = table.getColumns();
                if (!tablesPrefix.isEmpty() && tablesPrefix.stream().noneMatch(tableName::startsWith)) {
                    continue;
                }
                if (!tablesSuffix.isEmpty() && tablesSuffix.stream().noneMatch(tableName::endsWith)) {
                    continue;
                }
                if (config.getCodegenTypes().isEmpty()) {
                    continue;
                }
                EntityModel entityModel = createEntityModel(metaData, table);
                entityModel.setPackageName(config.getPackageName());
                for (CodegenType codegenType : config.getCodegenTypes()) {
                    CodegenHandler codegenHandler = CODEGEN_HANDLER_MAP.get(codegenType);
                    if (codegenHandler == null) {
                        throw new UnsupportedOperationException("未配置代码生成实现: " + codegenType);
                    }
                    Template template = codegenHandler.getTemplate(ENGINE);
                    String codes = template.renderToString(codegenHandler.getTemplateData(metaData, entityModel));
                    File outFile = new File(FilenameUtils.concat(config.getOutDir(), codegenHandler.getFileName(entityModel)));
                    FileUtils.writeStringToFile(outFile, codes, StandardCharsets.UTF_8);
                    log.info("生成代码成功 | CodegenType={} | --> {}", codegenType, outFile.getAbsolutePath());
                }
            }
        }
    }

    public static EntityModel createEntityModel(DataBaseMetaData metaData, Table table) {
        Assert.notNull(metaData, "参数 metaData 不能为null");
        Assert.notNull(table, "参数 table 不能为null");
        EntityModel entityModel = new EntityModel();
        entityModel.setTable(table);
        // entityModel.setPackageName("");
        entityModel.setClassName(NamingUtils.underlineToCamel(table.getName(), true));
        List<DbColumnMetaData> columnsMetaData = metaData.getJdbc().queryMetaData(String.format("select * from %s", table.getName()));
        List<Column> columns = table.getColumns();
        for (Column column : columns) {
            DbColumnMetaData columnMetaData = columnsMetaData.stream()
                    .filter(meta -> column.getName().equalsIgnoreCase(meta.getColumnName()))
                    .findFirst().orElse(null);
            EntityPropertyModel property = new EntityPropertyModel();
            if (columnMetaData == null) {
                throw new UnsupportedOperationException("不能获取 DbColumnMetaData，字段名: " + column.getName());
            }
            property.setJdbcType(columnMetaData.getColumnType());
            property.setJdbcTypeName(JDBC_TYPES_NAME_MAPPING.get(columnMetaData.getColumnType()));
            String typeName = JDBC_TYPES_MAPPING.get(columnMetaData.getColumnType());
            if (StringUtils.isBlank(typeName)) {
                throw new UnsupportedOperationException("缺少jdbc类型映射，java.sql.Types=" + columnMetaData.getColumnType());
            }
            property.setTypeName(typeName);
            String importPackage = TYPE_PACKAGE.get(typeName);
            if (StringUtils.isNotBlank(importPackage)) {
                property.setFullTypeName(importPackage);
                entityModel.addImportPackage(importPackage);
            }
            property.setName(NamingUtils.underlineToCamel(column.getName()));
            property.setComment(column.getComment());
            property.setColumn(column);
            entityModel.addProperty(property);
        }
        return entityModel;
    }
}
