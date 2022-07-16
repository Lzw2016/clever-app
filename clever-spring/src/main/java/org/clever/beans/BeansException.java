package org.clever.beans;

import org.clever.core.NestedRuntimeException;

/**
 * beans package和子package中抛出的所有异常的抽象超类。
 * 请注意，这是一个运行时(unchecked)异常。
 * bean异常常通常是致命的；没有理由对其进行检查
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 15:59 <br/>
 */
public abstract class BeansException extends NestedRuntimeException {
    public BeansException(String msg) {
        super(msg);
    }

    public BeansException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
