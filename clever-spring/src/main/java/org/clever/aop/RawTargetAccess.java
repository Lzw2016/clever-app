package org.clever.aop;

/**
 * AOP代理接口（特别是：简介接口）的标记，该接口明确打算返回原始目标对象（从方法调用返回时，通常会被代理对象替换）。
 *
 * <p>注意，这是一个java风格的标记接口{@link java.io.Serializable}，语义上应用于已声明的接口，而不是具体对象的完整类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/26 13:31 <br/>
 *
 * @see org.clever.aop.scope.ScopedObject
 */
public interface RawTargetAccess {
}
