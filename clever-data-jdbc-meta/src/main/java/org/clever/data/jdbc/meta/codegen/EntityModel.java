package org.clever.data.jdbc.meta.codegen;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.meta.model.Table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 生成java实体类所需要的数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 11:09 <br/>
 */
@Data
public class EntityModel implements Serializable {
    /**
     * 实体类包名
     */
    private String packageName;
    /**
     * 需要 import 的包
     */
    private Set<String> importPackages = new HashSet<>();
    /**
     * 实体类名
     */
    private String className;
    /**
     * 类成员属性
     */
    private List<EntityPropertyModel> properties = new ArrayList<>();
    /**
     * 对应的表信息
     */
    private Table table;

    public EntityModel addImportPackage(String packageName) {
        if (StringUtils.isNotBlank(packageName)) {
            importPackages.add(packageName);
        }
        return this;
    }

    public void addProperty(EntityPropertyModel property) {
        if (property != null) {
            properties.add(property);
        }
    }
}
