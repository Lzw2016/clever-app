package org.clever.data.dynamic.sql.builder;


import org.clever.data.dynamic.sql.BoundSql;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.dynamic.sql.node.DynamicContext;
import org.clever.data.dynamic.sql.parsing.GenericTokenParser;
import org.clever.data.dynamic.sql.utils.ParseSqlFuncUtils;

import java.util.LinkedHashMap;

public class StaticSqlSource implements SqlSource {
    private final DynamicContext context;
    private final String originalSql;

    public StaticSqlSource(String originalSql, DynamicContext context) {
        this.originalSql = originalSql;
        this.context = context;
    }

    @Override
    public BoundSql getBoundSql(DbType dbType, Object parameterObject) {
        String sqlDialect = originalSql;
        LinkedHashMap<String, Object> sqlVariable = new LinkedHashMap<>();
        // 使用antlr实现的自定义函数功能
        if (ParseSqlFuncUtils.needParse(originalSql)) {
            sqlDialect = ParseSqlFuncUtils.parseSqlFunc(dbType, originalSql, parameterObject, sqlVariable);
        }
        // 普通sql(参数占位符 ?)
        ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler();
        GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
        final String sql = parser.parse(sqlDialect);
        // namedParameterSql(参数占位符 :parameterName)
        handler = new ParameterMappingTokenHandler() {
            @Override
            public String handleToken(String content) {
                context.addParameterExpression(content);
                String parameterName = buildParameterMapping(content);
                parameterList.add(parameterName);
                return ":" + parameterName;
            }
        };
        parser = new GenericTokenParser("#{", "}", handler);
        final String namedParameterSql = parser.parse(sqlDialect);
        BoundSql boundSql = new BoundSql(sql, namedParameterSql, handler.getParameterList(), parameterObject);
        sqlVariable.forEach(boundSql::setAdditionalParameter);
        return boundSql;
    }
}
