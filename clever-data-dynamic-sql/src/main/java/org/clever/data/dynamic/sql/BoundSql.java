package org.clever.data.dynamic.sql;

import lombok.Getter;
import org.clever.data.dynamic.sql.reflection.MetaObject;
import org.clever.data.dynamic.sql.reflection.property.PropertyTokenizer;
import org.clever.data.dynamic.sql.utils.JavaType;

import java.util.*;

public class BoundSql {
    /**
     * 原始参数对象
     */
    @Getter
    private final Object parameterObject;
    /**
     * 生成的SQL语句(参数使用“?”占位)
     */
    @Getter
    private final String sql;
    /**
     * 参数名称形式的sql(原始SQL,参数名中存在特殊字符: “.”)
     */
    private final String rawNamedParameterSql;
    /**
     * 参数名称形式的sql(参数名中无特殊字符)
     */
    private String namedParameterSql;
    /**
     * Sql参数名称列表(有顺序)
     */
    @Getter
    private final List<ParameterMapping> parameterList;
    /**
     * 附加的参数
     */
    private final Map<String, Object> additionalParameters;
    /**
     * 附加的参数的MetaObject包装
     */
    private final MetaObject metaParameters;
    /**
     * Sql参数值列表(有顺序)
     */
    private List<Object> parameterValueList;
    /**
     * Sql参数Map集合
     */
    private Map<String, Object> parameterMap;
    /**
     * 动态sql中的表达式变量
     */
    @Getter
    private final Set<String> parameterExpressionSet = new LinkedHashSet<>();

    /**
     * @param sql               生成的SQL语句(参数使用“?”占位)
     * @param namedParameterSql 参数名称形式的sql
     * @param parameterList     Sql参数名称列表(有顺序)
     * @param parameterObject   原始参数对象
     */
    public BoundSql(String sql, String namedParameterSql, List<ParameterMapping> parameterList, Object parameterObject) {
        this.sql = sql;
        this.rawNamedParameterSql = namedParameterSql;
        this.parameterList = parameterList;
        this.parameterObject = parameterObject;
        this.additionalParameters = new HashMap<>();
        this.metaParameters = MetaObject.newMetaObject(additionalParameters);
    }

    /**
     * 参数值列表(有顺序)
     */
    public List<Object> getParameterValueList() {
        if (parameterValueList != null) {
            return parameterValueList;
        }
        initSqlParameter();
        return parameterValueList;
    }

    /**
     * Sql参数Map集合
     */
    public Map<String, Object> getParameterMap() {
        if (parameterMap != null) {
            return parameterMap;
        }
        initSqlParameter();
        return parameterMap;
    }

    /**
     * 参数名称形式的sql(参数名中无特殊字符)
     */
    public String getNamedParameterSql() {
        if (namedParameterSql != null) {
            return namedParameterSql;
        }
        initSqlParameter();
        return namedParameterSql;
    }

    private void initSqlParameter() {
        if (parameterList == null || parameterList.isEmpty()) {
            parameterValueList = Collections.emptyList();
            parameterMap = new HashMap<>();
            namedParameterSql = rawNamedParameterSql;
            return;
        }
        Map<String, String> renameParameter = new HashMap<>(parameterList.size());
        parameterValueList = new ArrayList<>(parameterList.size());
        parameterMap = new HashMap<>(parameterList.size());
        parameterList.forEach(parameterMapping -> {
            final String name = parameterMapping.getProperty();
            Object value;
            if (hasAdditionalParameter(name)) {
                value = getAdditionalParameter(name);
            } else if (this.parameterObject == null) {
                value = null;
            } else {
                MetaObject metaObject = MetaObject.newMetaObject(parameterObject);
                value = metaObject.getValue(name);
            }
            String javaType = parameterMapping.getJavaType();
            value = JavaType.conv(javaType, value);
            parameterValueList.add(value);
            String newName = name;
            if (needRename(name)) {
                newName = renameParameter(name);
                renameParameter.put(name, newName);
            }
            parameterMap.put(newName, value);
        });
        String namedSql = rawNamedParameterSql;
        for (Map.Entry<String, String> entry : renameParameter.entrySet()) {
            String name = ":" + entry.getKey();
            String newName = ":" + entry.getValue();
            namedSql = namedSql.replace(name, newName);
        }
        namedParameterSql = namedSql;
    }

    private boolean needRename(String parameterName) {
        return parameterName != null && parameterName.contains(".");
    }

    private String renameParameter(String parameterName) {
        return parameterName.replace('.', '$');
    }

    private boolean hasAdditionalParameter(String name) {
        String paramName = new PropertyTokenizer(name).getName();
        return additionalParameters.containsKey(paramName);
    }

    public void setAdditionalParameter(String name, Object value) {
        metaParameters.setValue(name, value);
    }

    private Object getAdditionalParameter(String name) {
        return metaParameters.getValue(name);
    }
}
