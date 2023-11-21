package org.clever.data.dynamic.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.dynamic.sql.dialect.func.JoinFuncTransform;
import org.clever.data.dynamic.sql.dialect.func.ToDateFuncTransform;
import org.clever.data.dynamic.sql.dialect.utils.SqlFuncTransformUtils;
import org.clever.data.dynamic.sql.domain.EntityA;
import org.clever.data.dynamic.sql.domain.EntityB;
import org.clever.data.dynamic.sql.domain.EntityMixin;
import org.clever.data.dynamic.sql.parsing.XNode;
import org.clever.data.dynamic.sql.parsing.XPathParser;
import org.clever.data.dynamic.sql.parsing.xml.XMLMapperEntityResolver;
import org.clever.data.dynamic.sql.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/10 10:23 <br/>
 */
@Slf4j
public class MapperXmlParserTest {

    private static final String mapperXml = "src/test/resources/sql.xml";
    private static final List<XNode> nodes = new ArrayList<>();

    public XNode getXNode(String id) {
        for (XNode node : nodes) {
            if (Objects.equals(id, node.getStringAttribute("id"))) {
                return node;
            }
        }
        return null;
    }

    @BeforeAll
    public static void init() throws Exception {
        final Properties variables = new Properties();
        final File xml = new File(mapperXml);
        final XPathParser parser = new XPathParser(FileUtils.openInputStream(xml), true, variables, new XMLMapperEntityResolver());
        final XNode mapper = parser.evalNode("/mapper");
        nodes.addAll(mapper.evalNodes("sql|select|insert|update|delete"));
    }

    @Test
    public void t01() throws Exception {
        for (XNode node : nodes) {
            Node n = node.getNode();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(n), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            log.info("\n\n{}\n\n", output);
        }
    }

    @Test
    public void t02() {
        SqlSource sqlSource = DynamicSqlParser.parserSql(getXNode("t02"));
        Map<String, Object> params = new HashMap<>();
        params.put("f1", "123");
        params.put("f2", "   ");
        params.put("f3", "bbb");
        params.put("f4", new int[]{1, 2, 3});
        params.put("f5", new ArrayList<String>() {{
            add("f5-aaa");
            add("f5-bbb");
            add("f5-ccc");
        }});
        params.put("f6", new BigDecimal("999"));
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    @Test
    public void t03() {
        EntityA entityA = new EntityA();
        entityA.setA("abc");
        entityA.setB(123);
        entityA.setC(123.456);
        entityA.setD(new BigDecimal("456.789"));
        entityA.setE(true);
        SqlSource sqlSource = DynamicSqlParser.parserSql(getXNode("t03"));
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, entityA);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    @Test
    public void t04() {
        EntityA entityA = new EntityA();
        entityA.setA("abc");
        entityA.setB(123);
        entityA.setC(123.456);
        entityA.setD(new BigDecimal("456.789"));
        entityA.setE(true);

        EntityB entityB = new EntityB();
        entityB.setA("abc");
        entityB.setB(123);
        entityB.setC(123.456);
        entityB.setD(new BigDecimal("456.789"));
        entityB.setE(true);
        entityB.setF(Arrays.asList("aaa", "bbb", "ccc"));
        entityB.setG(entityA);

        EntityMixin entityMixin = new EntityMixin();
        entityMixin.setA("abc");
        entityMixin.setB(123);
        entityMixin.setC(123.456);
        entityMixin.setD(new BigDecimal("456.789"));
        entityMixin.setE(true);
        entityMixin.setF(Arrays.asList(1L, 2L, 3L));
        entityMixin.setG(entityA);
        entityMixin.setH(entityB);
        entityMixin.setI(Arrays.asList(entityA, entityA, entityA));
        SqlSource sqlSource = DynamicSqlParser.parserSql(getXNode("t04"));
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, entityMixin);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    @Test
    public void t05() {
        SqlFuncTransformUtils.register(new ToDateFuncTransform());

        SqlSource sqlSource = DynamicSqlParser.parserSql(getXNode("t05"));
        Map<String, Object> params = new HashMap<>();
        params.put("today1", new Date());
        params.put("today2", new Date());
        params.put("a", "aaa");
        params.put("b", "bbb");
        params.put("c", "ccc");
        BoundSql boundSql = sqlSource.getBoundSql(DbType.ORACLE, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    @Test
    public void t06() {
        SqlFuncTransformUtils.register(new JoinFuncTransform());

        SqlSource sqlSource = DynamicSqlParser.parserSql(getXNode("t06"));
        Map<String, Object> params = new HashMap<>();
        params.put("a", "aaa");
        params.put("b", "bbb");
        params.put("c", "ccc");
        params.put("arr", Arrays.asList("1", "2", "8", "4", "5", "6"));

        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterValueList());
        log.info("--> {} ", TestUtils.deleteWhitespace(boundSql.getNamedParameterSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }

    @Test
    public void t07() {
        SqlFuncTransformUtils.register(new JoinFuncTransform());
        SqlSource sqlSource = DynamicSqlParser.parserSql(getXNode("t07"));
        Map<String, Object> params = new HashMap<>();
        params.put("a", "123");
        BoundSql boundSql = sqlSource.getBoundSql(DbType.MYSQL, params);
        log.info("--> {}", TestUtils.deleteWhitespace(boundSql.getSql()));
        log.info("--> {}", boundSql.getParameterMap());
    }
}
