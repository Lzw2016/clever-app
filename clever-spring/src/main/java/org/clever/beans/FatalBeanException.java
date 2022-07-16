package org.clever.beans;

/**
 * 引发beans package和子package中遇到的不可恢复问题，例如坏类或字段
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 17:05 <br/>
 */
public class FatalBeanException extends BeansException {
    public FatalBeanException(String msg) {
        super(msg);
    }

    public FatalBeanException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
