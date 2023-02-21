package org.clever.security.utils;

import lombok.extern.slf4j.Slf4j;
import ognl.Ognl;
import ognl.OgnlException;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.data.dynamic.sql.ognl.OgnlClassResolver;
import org.clever.data.dynamic.sql.ognl.OgnlMemberAccess;
import org.clever.security.annotation.CheckPermission;
import org.clever.security.model.SecurityContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 13:05 <br/>
 */
@Slf4j
public class CheckPermissionUtils {
    private static final Map<String, Object> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    private static Object getValue(String expression, SecurityContext securityContext) {
        try {
            Map<String, Object> root = new HashMap<>(1);
            root.put("sc", securityContext);
            Map<?, ?> context = Ognl.createDefaultContext(root, new OgnlMemberAccess(), new OgnlClassResolver(), null);
            return Ognl.getValue(parseExpression(expression), context, root);
        } catch (Throwable e) {
            log.warn("Error evaluating expression '{}'.", expression);
            return null;
        }
    }

    private static Object parseExpression(String expression) throws OgnlException {
        Object node = EXPRESSION_CACHE.get(expression);
        if (node == null) {
            node = Ognl.parseExpression(expression);
            EXPRESSION_CACHE.put(expression, node);
        }
        return node;
    }

    /**
     * 判断方法是否有权限
     */
    public static boolean hasPermission(Method checkMethod, SecurityContext securityContext) {
        CheckPermission checkPermission = checkMethod.getAnnotation(CheckPermission.class);
        if (checkPermission == null) {
            return true;
        }
        if (securityContext == null) {
            return false;
        }
        if (checkPermission.roles() != null
                && checkPermission.roles().length > 0
                && !securityContext.hasRoles(checkPermission.roles())) {
            return false;
        }
        if (checkPermission.permissions() != null
                && checkPermission.permissions().length > 0
                && !securityContext.hasPermissions(checkPermission.permissions())) {
            return false;
        }
        if (checkPermission.anyRoles() != null
                && checkPermission.anyRoles().length > 0
                && !securityContext.hasAnyRoles(checkPermission.anyRoles())) {
            return false;
        }
        if (checkPermission.anyPermissions() != null
                && checkPermission.anyPermissions().length > 0
                && !securityContext.hasAnyPermissions(checkPermission.anyPermissions())) {
            return false;
        }
        if (StringUtils.isNotBlank(checkPermission.expr())) {
            Object result = getValue(checkPermission.expr(), securityContext);
            return Conv.asBoolean(result);
        }
        return true;
    }
}
