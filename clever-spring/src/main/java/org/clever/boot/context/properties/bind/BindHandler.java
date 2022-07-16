package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.source.ConfigurationPropertyName;

/**
 * 回调接口，可用于在元素{@link Binder 绑定}期间处理附加逻辑
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:06 <br/>
 */
public interface BindHandler {
    /**
     * 默认无操作绑定处理程序。
     */
    BindHandler DEFAULT = new BindHandler() {
    };

    /**
     * 在元素绑定开始但尚未确定任何结果时调用。
     *
     * @param <T>     可绑定源类型
     * @param name    被绑定元素的名称
     * @param target  正在绑定的项目
     * @param context 绑定上下文
     * @return 应用于绑定的实际项（可以为空）
     */
    default <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        return target;
    }

    /**
     * 当元素的绑定以成功结果结束时调用。实现可能会更改最终返回的结果或执行加法验证。
     *
     * @param name    被绑定元素的名称
     * @param target  正在绑定的项目
     * @param context 绑定上下文
     * @param result  绑定结果（从不为null）
     * @return 应使用的实际结果（可能为空）
     */
    default Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        return result;
    }

    /**
     * 当元素的绑定以未绑定的结果结束并且即将返回新创建的实例时调用。实现可能会更改最终返回的结果或执行加法验证。
     *
     * @param name    被绑定元素的名称
     * @param target  正在绑定的项目
     * @param context 绑定上下文
     * @param result  新创建的实例（从不为null）
     * @return 应使用的实际结果（不得为空）
     */
    default Object onCreate(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        return result;
    }

    /**
     * 在绑定因任何原因失败时调用 (包括来自 {@link #onSuccess} 或 {@link #onCreate} 调用的故障)。
     * 实现可以选择接受异常并返回替代结果。
     *
     * @param name    被绑定元素的名称
     * @param target  正在绑定的项目
     * @param context 绑定上下文
     * @param error   错误原因（如果异常仍然存在，则可能会重新引发）
     * @return 应使用的实际结果（可能为空）
     * @throws Exception 如果绑定无效
     */
    default Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error) throws Exception {
        throw error;
    }

    /**
     * 当绑定完成并产生绑定或未绑定结果时调用。
     * 绑定失败时，即使处理程序从 {@link #onFailure} 返回结果，也不会调用此方法。
     *
     * @param name    被绑定元素的名称
     * @param target  正在绑定的项目
     * @param context 绑定上下文
     * @param result  绑定结果（可能为空）
     * @throws Exception 如果绑定无效
     */
    default void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
    }
}
