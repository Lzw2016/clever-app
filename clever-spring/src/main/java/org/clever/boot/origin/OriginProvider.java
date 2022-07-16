package org.clever.boot.origin;

/**
 * 接口以提供对项目来源的访问。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:55 <br/>
 *
 * @see Origin
 */
@FunctionalInterface
public interface OriginProvider {
    /**
     * 返回源原点，如果原点未知，则返回null。
     *
     * @return 原点或null
     */
    Origin getOrigin();
}
