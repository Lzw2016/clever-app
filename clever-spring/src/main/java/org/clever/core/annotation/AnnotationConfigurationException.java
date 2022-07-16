package org.clever.core.annotation;

import org.clever.core.NestedRuntimeException;

/**
 * 如果注解配置不正确，则由{@link AnnotationUtils}和合成注解抛出当前异常
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:42 <br/>
 */
public class AnnotationConfigurationException extends NestedRuntimeException {

    public AnnotationConfigurationException(String message) {
        super(message);
    }

    public AnnotationConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
