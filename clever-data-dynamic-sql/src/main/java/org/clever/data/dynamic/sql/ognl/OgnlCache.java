package org.clever.data.dynamic.sql.ognl;

import lombok.extern.slf4j.Slf4j;
import ognl.Ognl;
import ognl.OgnlException;
import org.clever.data.dynamic.sql.utils.ObjectUtils;
import org.clever.data.dynamic.sql.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class OgnlCache {
    public static final Map<String, Object> Default_Context = new HashMap<String, Object>() {{
        put("Str", StringUtils.Instance);
        put("str", StringUtils.Instance);
        put("STR", StringUtils.Instance);
        put("Obj", ObjectUtils.Instance);
        put("obj", ObjectUtils.Instance);
        put("OBJ", ObjectUtils.Instance);
    }};

    private static final OgnlMemberAccess MEMBER_ACCESS = new OgnlMemberAccess();
    private static final OgnlClassResolver CLASS_RESOLVER = new OgnlClassResolver();
    private static final Map<String, Object> expressionCache = new ConcurrentHashMap<>();

    private OgnlCache() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object getValue(String expression, Object root) {
        try {
            Map context = Ognl.createDefaultContext(root, MEMBER_ACCESS, CLASS_RESOLVER, null);
            context.putAll(Default_Context);
            return Ognl.getValue(parseExpression(expression), context, root);
        } catch (Throwable e) {
            log.warn("Error evaluating expression '{}'.", expression);
            // log.warn("Error evaluating expression '{}'. Cause: ", expression, e);
            // throw new BuilderException("Error evaluating expression '" + expression + "'. Cause: " + e, e);
            return null;
        }
    }

    private static Object parseExpression(String expression) throws OgnlException {
        Object node = expressionCache.get(expression);
        if (node == null) {
            node = Ognl.parseExpression(expression);
            expressionCache.put(expression, node);
        }
        return node;
    }
}
