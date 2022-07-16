package org.clever.dao;

/**
 * 尝试插入或更新数据导致违反主键或唯一约束时引发异常。
 * 请注意，这不一定是一个纯粹的关系概念；大多数数据库类型都需要唯一的主键。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:25 <br/>
 */
public class DuplicateKeyException extends DataIntegrityViolationException {
    public DuplicateKeyException(String msg) {
        super(msg);
    }

    public DuplicateKeyException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
