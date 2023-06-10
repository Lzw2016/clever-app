package org.clever.validation;

/**
 * 特定于应用程序的对象的验证器。
 *
 * <p>该接口完全脱离任何基础设施或上下文；也就是说，它不会耦合到仅验证 Web 层、数据访问层或其他层中的对象。
 * 因此，它适合在应用程序的任何层中使用，并支持将验证逻辑封装为一等公民。
 *
 * <p>在下面找到一个简单但完整的 {@code Validator} 实现，
 * 它验证 {@code UserLogin} 实例的各种 {@link String} 属性不为空（即它们不是 {@code null} 并且不包含完全由空格组成），
 * 并且存在的任何密码的长度至少为 {@code 'MINIMUM_PASSWORD_LENGTH'} 个字符。
 *
 * <pre>{@code
 * public class UserLoginValidator implements Validator {
 *     private static final int MINIMUM_PASSWORD_LENGTH = 6;
 *
 *     public boolean supports(Class clazz) {
 *         return UserLogin.class.isAssignableFrom(clazz);
 *     }
 *
 *     public void validate(Object target, Errors errors) {
 *         ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "field.required");
 *         ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required");
 *         UserLogin login = (UserLogin) target;
 *         if (login.getPassword() != null && login.getPassword().trim().length() < MINIMUM_PASSWORD_LENGTH) {
 *             errors.rejectValue(
 *                     "password",
 *                     "field.min.length",
 *                     new Object[]{Integer.valueOf(MINIMUM_PASSWORD_LENGTH)},
 *                     "The password must be at least [" + MINIMUM_PASSWORD_LENGTH + "] characters in length."
 *             );
 *         }
 *     }
 * }
 * }</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 20:47 <br/>
 *
 * @see SmartValidator
 * @see Errors
 */
public interface Validator {
    /**
     * 这个{@link Validator} {@link #validate(Object, Errors) validate} 提供的{@code clazz} 实例吗？
     * <p>此方法<i>通常</i>是这样实现的：
     * <pre>{@code
     *  // 其中 {@code Foo} 是要进行 {@link #validate(Object, Errors) 验证} 的实际对象实例的类（或超类）。
     *  return Foo.class.isAssignableFrom(clazz);
     * }</pre>
     *
     * @param clazz {@link Class}，这个 {@link Validator} 被询问是否可以 {@link #validate(Object, Errors) validate}
     * @return {@code true} 如果这个 {@link Validator} 确实可以 {@link #validate(Object, Errors) validate} 提供的 {@code clazz} 实例
     */
    boolean supports(Class<?> clazz);

    /**
     * 验证提供的 {@code target} 对象，该对象必须属于 {@link Class}，{@link #supports(Class)} 方法通常（或将）返回 {@code true}。
     * <p>提供的 {@link Errors errors} 实例可用于报告任何由此产生的验证错误。
     *
     * @param target 要验证的对象
     * @param errors 关于验证过程的上下文状态
     */
    void validate(Object target, Errors errors);
}
