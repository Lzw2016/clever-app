package org.clever.data.dynamic.sql.builder;

import lombok.Getter;
import org.clever.data.dynamic.sql.exception.BuilderException;
import org.clever.data.dynamic.sql.parsing.TokenHandler;
import org.clever.data.dynamic.sql.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/24 21:08 <br/>
 */
public class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {
    private static final String PARAMETER_PROPERTIES = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

    @Getter
    protected final List<String> parameterList = new ArrayList<>();

    public ParameterMappingTokenHandler() {
    }

    @Override
    public String handleToken(String content) {
        String parameterName = buildParameterMapping(content);
        parameterList.add(parameterName);
        return "?";
    }

    protected String buildParameterMapping(String content) {
        Map<String, String> propertiesMap = parseParameterMapping(content);
        String property = null;
        for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if ("property".equals(name)) {
                // Do Nothing
                property = value;
            } else if ("expression".equals(name)) {
                throw new BuilderException("Expression based parameters are not supported yet");
            } else {
                throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + PARAMETER_PROPERTIES);
            }
        }
        if (StringUtils.Instance.isBlank(property)) {
            throw new BuilderException("parameters is not empty");
        }
        return property;
    }

    private Map<String, String> parseParameterMapping(String content) {
        try {
            return new ParameterExpression(content);
        } catch (BuilderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuilderException("Parsing error was found in mapping #{" + content + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
        }
    }
}