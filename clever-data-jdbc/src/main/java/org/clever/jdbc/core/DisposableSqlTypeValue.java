package org.clever.jdbc.core;

/**
 * {@link SqlTypeValue}的子接口，添加清理回调，在设置值并执行相应语句后调用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:22 <br/>
 */
public interface DisposableSqlTypeValue extends SqlTypeValue {
    /**
     * 清理由该类型值持有的资源，例如，对于SqlLobValue，则清理LobCreator。
     *
     * @see org.clever.jdbc.support.SqlValue#cleanup()
     */
    void cleanup();
}
