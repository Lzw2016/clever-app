package org.clever.data.dynamic.sql.ognl;

import ognl.DefaultClassResolver;
import org.clever.data.dynamic.sql.io.Resources;

public class OgnlClassResolver extends DefaultClassResolver {

    @Override
    protected Class<?> toClassForName(String className) throws ClassNotFoundException {
        return Resources.classForName(className);
    }
}
