package org.clever.dao;

/**
 * 查询超时时引发的异常。这可能有不同的原因，具体取决于所使用的数据库API，
 * 但很可能是在数据库中断或停止查询处理后，在查询完成之前抛出的。
 *
 * <p>此异常可以由捕获本机数据库异常的用户代码或异常转换引发。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:11 <br/>
 */
public class QueryTimeoutException extends TransientDataAccessException {
    public QueryTimeoutException(String msg) {
        super(msg);
    }

    public QueryTimeoutException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
