package org.clever.beans.factory;

/**
 * 由想要在销毁时释放资源的 bean 实现的接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/06 15:47 <br/>
 */
public interface DisposableBean {
    /**
     * 在销毁 bean 时由包含的 {@code BeanFactory} 调用。
     *
     * @throws Exception 在关机错误的情况下。异常将被记录但不会重新抛出以允许其他 bean 也释放它们的资源。
     */
    void destroy() throws Exception;
}
