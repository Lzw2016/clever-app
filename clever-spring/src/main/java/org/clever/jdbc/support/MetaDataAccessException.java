package org.clever.jdbc.support;

import org.clever.core.NestedCheckedException;

/**
 * 异常指示在JDBC元数据查找过程中出错。
 *
 * <p>这是一个已检查的异常，因为我们希望捕获、记录和处理它，而不是导致应用程序失败。
 * 读取JDBC元数据失败通常不是一个致命的问题。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:18 <br/>
 */
public class MetaDataAccessException extends NestedCheckedException {
    public MetaDataAccessException(String msg) {
        super(msg);
    }

    public MetaDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
