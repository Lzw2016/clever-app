package org.clever.dao;

/**
 * 当预期结果至少有一行（或元素），但实际返回了零行（或元素）时引发数据访问异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:16 <br/>
 *
 * @see IncorrectResultSizeDataAccessException
 */
public class EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException {
    public EmptyResultDataAccessException(int expectedSize) {
        super(expectedSize, 0);
    }

    public EmptyResultDataAccessException(String msg, int expectedSize) {
        super(msg, expectedSize, 0);
    }

    public EmptyResultDataAccessException(String msg, int expectedSize, Throwable ex) {
        super(msg, expectedSize, 0, ex);
    }
}
