package org.clever.core.io.support;

import org.clever.core.io.VfsUtils;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;

/**
 * 用于访问{@link VfsUtils}方法的人工类，而无需将其公开给整个世界
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/05 12:16 <br/>
 */
abstract class VfsPatternUtils extends VfsUtils {
    static Object getVisitorAttributes() {
        return doGetVisitorAttributes();
    }

    static String getPath(Object resource) {
        String path = doGetPath(resource);
        return (path != null ? path : "");
    }

    static Object findRoot(URL url) throws IOException {
        return getRoot(url);
    }

    static void visit(Object resource, InvocationHandler visitor) throws IOException {
        Object visitorProxy = Proxy.newProxyInstance(
                VIRTUAL_FILE_VISITOR_INTERFACE.getClassLoader(),
                new Class<?>[]{VIRTUAL_FILE_VISITOR_INTERFACE},
                visitor
        );
        invokeVfsMethod(VIRTUAL_FILE_METHOD_VISIT, resource, visitorProxy);
    }
}
