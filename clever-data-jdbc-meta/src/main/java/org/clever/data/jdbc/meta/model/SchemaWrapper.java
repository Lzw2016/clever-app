package org.clever.data.jdbc.meta.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/07/10 17:02 <br/>
 */
@ToString(exclude = {"rawSchema"})
@EqualsAndHashCode(callSuper = true)
@Getter
public class SchemaWrapper extends Schema {
    @JsonIgnore
    private final Schema rawSchema;
    /**
     * 指定表前缀(忽略大小写过滤)
     */
    private final Set<String> tablesPrefix;
    /**
     * 指定表后缀(忽略大小写过滤)
     */
    private final Set<String> tablesSuffix;

    public SchemaWrapper(Schema schema, Set<String> tablesPrefix, Set<String> tablesSuffix) {
        super(schema.getDbType(), schema.getVersion(), schema.getName());
        this.rawSchema = schema;
        this.tablesPrefix = tablesPrefix;
        this.tablesSuffix = tablesSuffix;
    }

    @Override
    public void addTable(Table table) {
        rawSchema.addTable(table);
    }

    @Override
    public Table getTable(String tableName) {
        if (tableName == null) {
            return null;
        }
        return getTables().stream()
            .filter(table -> tableName.equalsIgnoreCase(table.getName()))
            .findFirst().orElse(null);
    }

    @Override
    public void addSequence(Sequence sequence) {
        rawSchema.addSequence(sequence);
    }

    @Override
    public Sequence getSequence(String sequenceName) {
        return rawSchema.getSequence(sequenceName);
    }

    @Override
    public void addProcedure(Procedure procedure) {
        rawSchema.addProcedure(procedure);
    }

    @Override
    public Procedure getProcedure(String procedureName) {
        return rawSchema.getProcedure(procedureName);
    }

    @Override
    public void setName(String name) {
        rawSchema.setName(name);
    }

    @Override
    public List<Table> getTables() {
        return rawSchema.getTables().stream().filter(table -> {
            String tableName = table.getName();
            if (!tablesPrefix.isEmpty() && tablesPrefix.stream().noneMatch(prefix -> StringUtils.startsWithIgnoreCase(tableName, prefix))) {
                return false;
            }
            return tablesSuffix.isEmpty() || tablesSuffix.stream().anyMatch(suffix -> StringUtils.endsWithIgnoreCase(tableName, suffix));
        }).collect(Collectors.toList());
    }

    @Override
    public List<Sequence> getSequences() {
        return rawSchema.getSequences();
    }

    @Override
    public List<Procedure> getProcedures() {
        return rawSchema.getProcedures();
    }
}
