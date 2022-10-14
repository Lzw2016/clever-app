package org.clever.data.dynamic.sql.test;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;
import org.clever.data.dynamic.sql.domain.Author;
import org.clever.data.dynamic.sql.ognl.OgnlCache;
import org.clever.data.dynamic.sql.ognl.OgnlClassResolver;
import org.clever.data.dynamic.sql.ognl.OgnlMemberAccess;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/09/01 14:14 <br/>
 */
@Slf4j
public class OgnlTest {

    @SneakyThrows
    @Test
    public void t01() {
        Author author = new Author();
        author.setId(11);
        author.setUsername("lzw");
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(author, new OgnlMemberAccess(), new OgnlClassResolver(), null);
        context.put("author", author);
        PropertyAccessor propertyAccessor = new MyPropertyAccessor();
        OgnlRuntime.setPropertyAccessor(Author.class, propertyAccessor);
        Object username = Ognl.getValue(Ognl.parseExpression("#author.username"), context, context.getRoot());
        log.info("username -> {}", username);
        username = Ognl.getValue(Ognl.parseExpression("username"), context, context.getRoot());
        log.info("username -> {}", username);
    }

    @SneakyThrows
    @Test
    public void t02() {
        Author author = new Author();
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(author, new OgnlMemberAccess(), new OgnlClassResolver(), null);
        context.setRoot(author);
        context.setValues(new HashMap<>());
        context.put("author", author);
        Ognl.getValue("#author.setId(123456)", context, context.getRoot());
        log.info("author -> {}", author);
        Ognl.getValue("#author.setLongId(123456789L)", context, context.getRoot());
        log.info("author -> {}", author);
    }

    @Test
    public void t03() {
        Author author = new Author();
        author.setId(111222333);
        author.setUsername("lzw");
        Map<String, Object> root = new HashMap<>();
        root.put("author", author);
        Object id = OgnlCache.getValue("author.id", root);
        log.info("id -> {}", id);
    }
}

class MyPropertyAccessor implements PropertyAccessor {
    @SneakyThrows
    @Override
    public Object getProperty(Map context, Object target, Object name) {
        String nameTmo = name.toString();
        OgnlContext ognlContext = (OgnlContext) context;
        Object result = OgnlRuntime.getMethodValue(ognlContext, target, nameTmo, true);
        return result + "##123";
    }

    @Override
    public void setProperty(Map context, Object target, Object name, Object value) {
        System.out.println("setProperty");
    }

    @Override
    public String getSourceAccessor(OgnlContext ognlContext, Object o, Object o1) {
        System.out.println("getSourceAccessor");
        return null;
    }

    @Override
    public String getSourceSetter(OgnlContext ognlContext, Object o, Object o1) {
        System.out.println("getSourceSetter");
        return null;
    }
}