package org.clever.validation;

/**
 * {@link Validator} 接口的扩展变体，添加了对验证“提示”的支持。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/08 21:10 <br/>
 */
public interface SmartValidator extends Validator {
    /**
     * 验证提供的 {@code target} 对象，该对象必须是 {@link Class} 类型，{@link #supports(Class)} 方法通常返回 {@code true}。
     * <p>提供的 {@link Errors errors} 实例可用于报告任何由此产生的验证错误。
     * <p><b>{@code validate()} 的这种变体支持验证提示，例如针对 JSR-303 提供程序的验证组</b>（在这种情况下，提供的提示对象需要是类型的注解参数 {@code Class}）。
     * <p>注意：实际目标 {@code Validator} 可能会忽略验证提示，在这种情况下，此方法的行为应与其常规 {@link #validate(Object, Errors)} 兄弟一样。
     *
     * @param target          要验证的对象
     * @param errors          关于验证过程的上下文状态
     * @param validationHints 要传递给验证引擎的一个或多个提示对象
     * @see javax.validation.Validator#validate(Object, Class[])
     */
    void validate(Object target, Errors errors, Object... validationHints);

    /**
     * 验证目标类型上指定字段的提供值，报告相同的验证错误，就好像该值将绑定到目标类实例上的字段一样。
     *
     * @param targetType      目标类型
     * @param fieldName       字段名称
     * @param value           候选值
     * @param errors          关于验证过程的上下文状态
     * @param validationHints 要传递给验证引擎的一个或多个提示对象
     * @see javax.validation.Validator#validateValue(Class, String, Object, Class[])
     */
    default void validateValue(Class<?> targetType, String fieldName, Object value, Errors errors, Object... validationHints) {
        throw new IllegalArgumentException("Cannot validate individual value for " + targetType);
    }
}
