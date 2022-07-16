package org.clever.core;

/**
 * 任何对象都可以实现该接口以提供其实际的ResolvableType<br/>
 * 当确定实例是否与通用签名匹配时，这些信息非常有用，因为Java在运行时不会传递该签名<br/>
 * 在复杂的层次结构场景中，此接口的用户应该小心，尤其是当类的泛型类型签名在子类中更改时<br/>
 * 对于默认行为，总是可以返回null作为fallback
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 11:55 <br/>
 */
public interface ResolvableTypeProvider {
    /**
     * 返回描述此实例的{@link ResolvableType}(如果应该应用某种默认值，则返回{@code null})
     */
    ResolvableType getResolvableType();
}
