package org.clever.data.dynamic.sql.reflection.wrapper;

import org.clever.data.dynamic.sql.exception.ReflectionException;
import org.clever.data.dynamic.sql.reflection.MetaObject;

public class DefaultObjectWrapperFactory implements ObjectWrapperFactory {

    @Override
    public boolean hasWrapperFor(Object object) {
        return false;
    }

    @Override
    public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
        throw new ReflectionException("The DefaultObjectWrapperFactory should never be called to provide an ObjectWrapper.");
    }
}
