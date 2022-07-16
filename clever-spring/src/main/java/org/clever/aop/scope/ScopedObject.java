package org.clever.aop.scope;

import org.clever.aop.RawTargetAccess;

/**
 * 作用域对象的AOP介绍接口。
 *
 * <p>从ScopedProxyFactoryBean创建的对象可以强制转换到此接口，从而可以访问原始目标对象并通过编程删除目标对象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/26 13:30 <br/>
 */
public interface ScopedObject extends RawTargetAccess {
    /**
     * 以原始形式（存储在目标作用域中）返回此作用域对象代理后面的当前目标对象。
     * <p>例如，可以将原始目标对象传递给持久性提供程序，而持久性提供程序将无法处理作用域代理对象。
     *
     * @return 此作用域对象代理后面的当前目标对象
     */
    Object getTargetObject();

    /**
     * 将此对象从其目标作用域中删除，例如从备份会话中删除。
     * <p>请注意，以后不能再调用作用域对象（至少在当前线程内，即在目标作用域中使用完全相同的目标对象）。
     */
    void removeFromScope();
}
