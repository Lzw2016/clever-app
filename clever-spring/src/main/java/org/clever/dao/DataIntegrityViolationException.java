package org.clever.dao;

/**
 * 尝试插入或更新数据导致违反完整性约束时引发异常。
 * 请注意，这不仅仅是一个关系概念；大多数数据库类型都需要唯一的主键。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:08 <br/>
 */
public class DataIntegrityViolationException extends NonTransientDataAccessException {
    public DataIntegrityViolationException(String msg) {
        super(msg);
    }

    public DataIntegrityViolationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
