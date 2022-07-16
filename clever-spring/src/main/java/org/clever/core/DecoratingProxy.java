package org.clever.core;

/**
 * 接口将通过修饰代理来实现，特别是AOP代理，但也可能是具有修饰器语义的自定义代理。
 *
 * <p>注意，如果修饰类不在代理类的层次结构中，则应该实现该接口。
 * 特别是，“target-class”代理（如AOP CGLIB代理）不应实现它，因为对目标类的任何查找都可以简单地在那里的代理类上执行。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:34 <br/>
 */
public interface DecoratingProxy {
    /**
     * 返回此代理后面的（最终）装饰类。
     * <p>对于AOP代理，这将是最终目标类，而不仅仅是直接目标（对于多个嵌套代理）。
     *
     * @return 修饰类（从不为null）
     */
    Class<?> getDecoratedClass();
}
