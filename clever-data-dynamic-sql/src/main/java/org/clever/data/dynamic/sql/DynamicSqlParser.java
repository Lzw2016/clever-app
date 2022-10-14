package org.clever.data.dynamic.sql;

import org.clever.data.dynamic.sql.builder.DynamicSqlSource;
import org.clever.data.dynamic.sql.builder.RawSqlSource;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.node.TextSqlNode;
import org.clever.data.dynamic.sql.node.XMLScriptBuilder;
import org.clever.data.dynamic.sql.parsing.PropertyParser;
import org.clever.data.dynamic.sql.parsing.XNode;
import org.clever.data.dynamic.sql.parsing.XPathParser;
import org.clever.data.dynamic.sql.parsing.xml.XMLMapperEntityResolver;
import org.clever.data.dynamic.sql.utils.StringUtils;

import java.util.Properties;

/**
 * 动态SQL解析器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/09/01 12:26 <br/>
 */
public class DynamicSqlParser {
    private DynamicSqlParser() {
    }

    public static SqlSource parserSql(String dynamicSql) {
        final Properties variables = new Properties();
        dynamicSql = StringUtils.Instance.trim(dynamicSql);
        if (dynamicSql.startsWith("<script>")) {
            XPathParser parser = new XPathParser(dynamicSql, false, variables, new XMLMapperEntityResolver());
            return parserSql(parser.evalNode("/script"));
        } else {
            dynamicSql = PropertyParser.parse(dynamicSql, variables);
            TextSqlNode textSqlNode = new TextSqlNode(dynamicSql);
            if (textSqlNode.isDynamic()) {
                return new DynamicSqlSource(textSqlNode);
            } else {
                return new RawSqlSource(dynamicSql);
            }
        }
    }

    public static SqlSource parserSql(XNode script) {
        XMLScriptBuilder builder = new XMLScriptBuilder(script);
        return builder.parseScriptNode();
    }
}
