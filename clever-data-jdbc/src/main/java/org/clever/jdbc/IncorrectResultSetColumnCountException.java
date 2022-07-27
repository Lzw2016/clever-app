package org.clever.jdbc;

import org.clever.dao.DataRetrievalFailureException;

/**
 * 当结果集没有正确的列计数时引发数据访问异常，例如，当需要一个列但得到0个或多个列时。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:07 <br/>
 *
 * @see org.clever.dao.IncorrectResultSizeDataAccessException
 */
public class IncorrectResultSetColumnCountException extends DataRetrievalFailureException {
    private final int expectedCount;
    private final int actualCount;

    public IncorrectResultSetColumnCountException(int expectedCount, int actualCount) {
        super("Incorrect column count: expected " + expectedCount + ", actual " + actualCount);
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
    }

    public IncorrectResultSetColumnCountException(String msg, int expectedCount, int actualCount) {
        super(msg);
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
    }

    /**
     * 返回预期的列计数
     */
    public int getExpectedCount() {
        return this.expectedCount;
    }

    /**
     * 返回实际列计数
     */
    public int getActualCount() {
        return this.actualCount;
    }
}
