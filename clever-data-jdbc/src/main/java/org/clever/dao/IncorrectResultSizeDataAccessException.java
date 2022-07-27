package org.clever.dao;

/**
 * 当结果的大小不符合预期时引发数据访问异常，例如，当需要一行但得到0行或多行时。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:15 <br/>
 *
 * @see EmptyResultDataAccessException
 */
public class IncorrectResultSizeDataAccessException extends DataRetrievalFailureException {
    private final int expectedSize;
    private final int actualSize;

    public IncorrectResultSizeDataAccessException(int expectedSize) {
        super("Incorrect result size: expected " + expectedSize);
        this.expectedSize = expectedSize;
        this.actualSize = -1;
    }

    public IncorrectResultSizeDataAccessException(int expectedSize, int actualSize) {
        super("Incorrect result size: expected " + expectedSize + ", actual " + actualSize);
        this.expectedSize = expectedSize;
        this.actualSize = actualSize;
    }

    public IncorrectResultSizeDataAccessException(String msg, int expectedSize) {
        super(msg);
        this.expectedSize = expectedSize;
        this.actualSize = -1;
    }

    public IncorrectResultSizeDataAccessException(String msg, int expectedSize, Throwable ex) {
        super(msg, ex);
        this.expectedSize = expectedSize;
        this.actualSize = -1;
    }

    public IncorrectResultSizeDataAccessException(String msg, int expectedSize, int actualSize) {
        super(msg);
        this.expectedSize = expectedSize;
        this.actualSize = actualSize;
    }

    public IncorrectResultSizeDataAccessException(String msg, int expectedSize, int actualSize, Throwable ex) {
        super(msg, ex);
        this.expectedSize = expectedSize;
        this.actualSize = actualSize;
    }

    /**
     * 返回预期的结果大小
     */
    public int getExpectedSize() {
        return this.expectedSize;
    }

    /**
     * 返回实际结果大小（如果未知，则返回-1）
     */
    public int getActualSize() {
        return this.actualSize;
    }
}
