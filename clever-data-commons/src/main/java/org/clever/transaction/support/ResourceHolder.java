package org.clever.transaction.support;

/**
 * 由资源持有者实现的通用接口。允许事务基础架构在必要时对持有人进行内省和重置。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:37 <br/>
 *
 * @see ResourceHolderSupport
 */
public interface ResourceHolder {
    /**
     * 重置此持有者的事务状态。
     */
    void reset();

    /**
     * 通知此持有者其已从事务同步中解除绑定。
     */
    void unbound();

    /**
     * 确定该持有人是否被视为 'void',即：作为前一个线程的剩余。
     */
    boolean isVoid();
}
