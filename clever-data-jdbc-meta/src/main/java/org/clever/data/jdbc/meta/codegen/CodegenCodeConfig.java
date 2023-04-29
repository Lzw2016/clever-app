package org.clever.data.jdbc.meta.codegen;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 11:40 <br/>
 */
@Getter
public class CodegenCodeConfig implements Serializable {
    /**
     * 忽略Schema名(忽略大小写过滤)
     */
    private final Set<String> ignoreSchemas = new HashSet<>();
    /**
     * 忽略表名(忽略大小写过滤)
     */
    private final Set<String> ignoreTables = new HashSet<>();
    /**
     * 忽略表前缀(忽略大小写过滤)
     */
    private final Set<String> ignoreTablesPrefix = new HashSet<>();
    /**
     * 忽略表后缀(忽略大小写过滤)
     */
    private final Set<String> ignoreTablesSuffix = new HashSet<>();
    /**
     * 指定Schema名(忽略大小写过滤)
     */
    private final Set<String> schemas = new HashSet<>();
    /**
     * 指定表名(忽略大小写过滤)
     */
    private final Set<String> tables = new HashSet<>();
    /**
     * 指定表前缀(忽略大小写过滤)
     */
    private final Set<String> tablesPrefix = new HashSet<>();
    /**
     * 指定表后缀(忽略大小写过滤)
     */
    private final Set<String> tablesSuffix = new HashSet<>();
    /**
     * 文件输出目录
     */
    private String outDir;
    /**
     * class package 名
     */
    private String packageName;
    /**
     * 生成代码类型
     */
    private final Set<CodegenType> codegenTypes = new HashSet<>();

    public CodegenCodeConfig() {
        codegenTypes.add(CodegenType.JAVA_ENTITY);
        codegenTypes.add(CodegenType.JAVA_QUERYDSL);
    }

    /**
     * 忽略Schema名(忽略大小写过滤)
     */
    public CodegenCodeConfig addIgnoreSchema(String schemaName) {
        if (StringUtils.isNotBlank(schemaName)) {
            ignoreSchemas.add(schemaName);
        }
        return this;
    }

    /**
     * 忽略表名(忽略大小写过滤)
     */
    public CodegenCodeConfig addIgnoreTable(String tableName) {
        if (StringUtils.isNotBlank(tableName)) {
            ignoreTables.add(tableName);
        }
        return this;
    }

    /**
     * 忽略表前缀(忽略大小写过滤)
     */
    public CodegenCodeConfig addIgnoreTablePrefix(String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            ignoreTablesPrefix.add(tablePrefix);
        }
        return this;
    }

    /**
     * 忽略表后缀(忽略大小写过滤)
     */
    public CodegenCodeConfig addIgnoreTableSuffix(String tableSuffix) {
        if (StringUtils.isNotBlank(tableSuffix)) {
            ignoreTablesSuffix.add(tableSuffix);
        }
        return this;
    }

    /**
     * 指定Schema名(忽略大小写过滤)
     */
    public CodegenCodeConfig addSchema(String schemaName) {
        if (StringUtils.isNotBlank(schemaName)) {
            schemas.add(schemaName);
        }
        return this;
    }

    /**
     * 指定表名(忽略大小写过滤)
     */
    public CodegenCodeConfig addTable(String tableName) {
        if (StringUtils.isNotBlank(tableName)) {
            tables.add(tableName);
        }
        return this;
    }

    /**
     * 指定表前缀(忽略大小写过滤)
     */
    public CodegenCodeConfig addTablePrefix(String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tablesPrefix.add(tablePrefix);
        }
        return this;
    }

    /**
     * 指定表后缀(忽略大小写过滤)
     */
    public CodegenCodeConfig addTableSuffix(String tableSuffix) {
        if (StringUtils.isNotBlank(tableSuffix)) {
            tablesSuffix.add(tableSuffix);
        }
        return this;
    }

    /**
     * 文件输出目录
     */
    public CodegenCodeConfig setOutDir(String outDir) {
        this.outDir = outDir;
        return this;
    }

    /**
     * class package 名
     */
    public CodegenCodeConfig setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    /**
     * 忽略Schema名(忽略大小写过滤)
     */
    public Set<String> getIgnoreSchemas() {
        return ignoreSchemas.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表名(忽略大小写过滤)
     */
    public Set<String> getIgnoreTables() {
        return ignoreTables.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表前缀(忽略大小写过滤)
     */
    public Set<String> getIgnoreTablesPrefix() {
        return ignoreTablesPrefix.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表后缀(忽略大小写过滤)
     */
    public Set<String> getIgnoreTablesSuffix() {
        return ignoreTablesSuffix.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 指定Schema名(忽略大小写过滤)
     */
    public Set<String> getSchemas() {
        return schemas.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 指定表名(忽略大小写过滤)
     */
    public Set<String> getTables() {
        return tables.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 指定表前缀(忽略大小写过滤)
     */
    public Set<String> getTablesPrefix() {
        return tablesPrefix.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 指定表后缀(忽略大小写过滤)
     */
    public Set<String> getTablesSuffix() {
        return tablesSuffix.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 是否包含 “代码类型”
     */
    public boolean hasCodegenType(CodegenType codegenType) {
        return codegenTypes.contains(codegenType);
    }

//    public File getOutDirFile() {
//    }
}
