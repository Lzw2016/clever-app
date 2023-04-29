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
@SuppressWarnings("DuplicatedCode")
public class QueryDSLSupport {
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    @SneakyThrows
    public static String getQueryDslFieldDefine(EntityPropertyModel property) {
        final Class<?> clazz = Class.forName(property.getFullTypeName());
        final JavaTypeMappings typeMappings = new JavaTypeMappings();
        final TypeCategory typeCategory = getTypeCategory(clazz);
        final ClassType classType = new ClassType(typeCategory, clazz);
        final SimpleType propertyType = new SimpleType(classType, classType.getParameters());
        final Type queryType = typeMappings.getPathType(propertyType, new EntityType(classType), false);
        final String javaType = getJavaType(queryType);
        final String createMethod = getCreateMethod(propertyType);
        // 构建字段定义
        StringBuilder fieldType = new StringBuilder();
        fieldType.append(queryType.getSimpleName());
        if (StringUtils.isNotBlank(javaType)) {
            fieldType.append("<").append(javaType).append(">");
        }
        fieldType.append(" ").append(property.getName());
        fieldType.append(" = ").append(createMethod).append("(\"").append(property.getName());
        if (StringUtils.isNotBlank(javaType)) {
            fieldType.append("\", ").append(javaType).append(".class)");
        } else {
            fieldType.append("\")");
        }
        return fieldType.toString();
    }

    @SneakyThrows
    public static String getQueryDslFieldDefineForKotlin(EntityPropertyModel property) {
        final Class<?> clazz = Class.forName(property.getFullTypeName());
        final JavaTypeMappings typeMappings = new JavaTypeMappings();
        final TypeCategory typeCategory = getTypeCategory(clazz);
        final ClassType classType = new ClassType(typeCategory, clazz);
        final SimpleType propertyType = new SimpleType(classType, classType.getParameters());
        final Type queryType = typeMappings.getPathType(propertyType, new EntityType(classType), false);
        final String javaType = getJavaType(queryType);
        final String createMethod = getCreateMethod(propertyType);
        // 构建字段定义
        StringBuilder fieldType = new StringBuilder();
        fieldType.append(property.getName()).append(": ").append(queryType.getSimpleName());
        if (StringUtils.isNotBlank(javaType)) {
            fieldType.append("<").append(javaType).append(">");
        }
        fieldType.append(" = ").append(createMethod).append("(\"").append(property.getName());
        if (StringUtils.isNotBlank(javaType)) {
            fieldType.append("\", ").append(javaType).append("::class.java)");
        } else {
            fieldType.append("\")");
        }
        return fieldType.toString();
    }

    private static String getCreateMethod(SimpleType propertyType) {
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
        return createMethod;
    }

    private static String getJavaType(Type queryType) {
        String javaType = "";
        if (!queryType.getParameters().isEmpty()) {
            javaType = queryType.getParameters().get(0).getSimpleName();
        }
        return javaType;
    }

    private static TypeCategory getTypeCategory(Class<?> clazz) {
        TypeCategory typeCategory = TypeCategory.get(clazz.getName());
        if (Number.class.isAssignableFrom(clazz)) {
            typeCategory = TypeCategory.NUMERIC;
        } else if (Enum.class.isAssignableFrom(clazz)) {
            typeCategory = TypeCategory.ENUM;
        }
        return typeCategory;
    }
}
