package org.clever.data.dynamic.sql.builder;

import lombok.Getter;
import org.clever.data.dynamic.sql.ParameterMapping;
import org.clever.data.dynamic.sql.exception.BuilderException;
import org.clever.data.dynamic.sql.parsing.TokenHandler;
import org.clever.data.dynamic.sql.utils.JavaType;
import org.clever.data.dynamic.sql.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/24 21:08 <br/>
 */
@Getter
public class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {
    private static final String PARAMETER_PROPERTIES = "javaType";


    protected final List<ParameterMapping> parameterList = new ArrayList<>();

    public ParameterMappingTokenHandler() {
    }

    @Override
    public String handleToken(String content) {
        ParameterMapping parameterMapping = buildParameterMapping(content);
        parameterList.add(parameterMapping);
        return "?";
    }

    protected ParameterMapping buildParameterMapping(String content) {
        Map<String, String> propertiesMap = parseParameterMapping(content);
        ParameterMapping parameterMapping = new ParameterMapping();
        for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if ("property".equals(name)) {
                parameterMapping.setProperty(value);
            } else if ("javaType".equals(name)) {
                if (value != null) {
                    value = value.toLowerCase().trim();
                }
                if (!JavaType.supportType(value)) {
                    throw new BuilderException("An invalid javaType #{" + content + "}.  Valid properties are " + JavaType.allType());
                }
                parameterMapping.setJavaType(value);
            } else if ("expression".equals(name)) {
                throw new BuilderException("Expression based parameters are not supported yet");
            } else {
                throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + PARAMETER_PROPERTIES);
            }
        }
        if (StringUtils.Instance.isBlank(parameterMapping.getProperty())) {
            throw new BuilderException("parameters is not empty");
        }
        return parameterMapping;
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
