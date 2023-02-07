package org.clever.data.redis;

import org.clever.dao.DataAccessException;

/**
 * 可能将 {@link Exception} 转换为适当的 {@link DataAccessException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:33 <br/>
 */
public interface ExceptionTranslationStrategy {
    /**
     * 可能将给定的 {@link Exception} 转换为 {@link DataAccessException}
     *
     * @param e 不得为 {@literal null}
     * @return 如果给定 {@link Exception} 无法翻译，则可以是 {@literal null}。
     */
    DataAccessException translate(Exception e);
}
