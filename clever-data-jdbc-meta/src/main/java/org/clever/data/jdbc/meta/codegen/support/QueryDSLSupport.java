package org.clever.data.jdbc.meta.codegen.support;

import com.querydsl.codegen.EntityType;
import com.querydsl.codegen.JavaTypeMappings;
import com.querydsl.codegen.utils.model.ClassType;
import com.querydsl.codegen.utils.model.SimpleType;
import com.querydsl.codegen.utils.model.Type;
import com.querydsl.codegen.utils.model.TypeCategory;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.meta.codegen.EntityPropertyModel;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 18:20 <br/>
 */
public class QueryDSLSupport {
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    @SneakyThrows
    public static String getQueryDslFieldDefine(EntityPropertyModel property) {
        Class<?> clazz = Class.forName(property.getFullTypeName());
        JavaTypeMappings typeMappings = new JavaTypeMappings();
        TypeCategory typeCategory = TypeCategory.get(clazz.getName());
        if (Number.class.isAssignableFrom(clazz)) {
            typeCategory = TypeCategory.NUMERIC;
        } else if (Enum.class.isAssignableFrom(clazz)) {
            typeCategory = TypeCategory.ENUM;
        }
        ClassType classType = new ClassType(typeCategory, clazz);
        SimpleType propertyType = new SimpleType(classType, classType.getParameters());
        Type queryType = typeMappings.getPathType(propertyType, new EntityType(classType), false);
        // 构建字段定义
        StringBuilder fieldType = new StringBuilder();
        fieldType.append(queryType.getSimpleName());
        String javaType = "";
        if (!queryType.getParameters().isEmpty()) {
            javaType = queryType.getParameters().get(0).getSimpleName();
            fieldType.append("<").append(javaType).append(">");
        }
        fieldType.append(" ").append(property.getName());
        String createMethod = "";
        switch (propertyType.getCategory()) {
            case STRING:
                createMethod = "createString";
                break;
            case BOOLEAN:
                createMethod = "createBoolean";
                break;
            case SIMPLE:
                createMethod = "createSimple";
                break;
            case COMPARABLE:
                createMethod = "createComparable";
                break;
            case ENUM:
                createMethod = "createEnum";
                break;
            case DATE:
                createMethod = "createDate";
                break;
            case DATETIME:
                createMethod = "createDateTime";
                break;
            case TIME:
                createMethod = "createTime";
                break;
            case NUMERIC:
                createMethod = "createNumber";
                break;
            case ARRAY:
                createMethod = "createArray";
                break;
        }
        if (StringUtils.isBlank(createMethod)) {
            return "";
        }
        fieldType.append(" = ").append(createMethod).append("(\"").append(property.getName());
        if (StringUtils.isNotBlank(javaType)) {
            fieldType.append("\", ").append(javaType).append(".class)");
        } else {
            fieldType.append("\")");
        }
        return fieldType.toString();
    }
}
