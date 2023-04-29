package org.clever.data.jdbc.meta.codegen;

import lombok.Data;
import org.clever.data.jdbc.meta.model.Column;

import java.io.Serializable;

/**
 * 生成java实体类属性所需要的数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 11:18 <br/>
 */
@Data
public class EntityPropertyModel implements Serializable {
    /**
     * 字段类型名称(java.sql.Types)
     */
    private Integer jdbcType;
    /**
     * 字段类型名称，如: String、Boolean、Long
     */
    private String typeName;
    /**
     * 字段名
     */
    private String name;
    /**
     * 字段注释
     */
    private String comment;
    /**
     * 对应的数据库表字段
     */
    private Column column;
}
