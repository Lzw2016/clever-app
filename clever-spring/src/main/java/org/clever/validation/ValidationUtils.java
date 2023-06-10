package org.clever.validation;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 实用程序类提供方便的方法来调用 {@link Validator} 和拒绝空字段。
 * <p>在使用 {@link #rejectIfEmpty} 或 {@link #rejectIfEmptyOrWhitespace} 时，检查 {@code Validator} 实现中的空字段可以变成一行。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/10 21:29 <br/>
 *
 * @see Validator
 * @see Errors
 */
public abstract class ValidationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

    /**
     * 为提供的对象和 {@link Errors} 实例调用给定的 {@link Validator}。
     *
     * @param validator 要调用的 {@code Validator}
     * @param target    绑定参数的对象
     * @param errors    应该存储错误的 {@link Errors} 实例
     * @throws IllegalArgumentException 如果 {@code Validator} 或 {@code Errors} 参数中的任何一个是 {@code null}，或者如果提供的 {@code Validator} 不 {@link Validator#supports(Class) 支持}提供的验证对象的类型
     */
    public static void invokeValidator(Validator validator, Object target, Errors errors) {
        invokeValidator(validator, target, errors, (Object[]) null);
    }

    /**
     * 为提供的对象和 {@link Errors} 实例调用给定的 {@link Validator}/{@link SmartValidator}。
     *
     * @param validator       要调用的 {@code Validator}
     * @param target          绑定参数的对象
     * @param errors          应该存储错误的 {@link Errors} 实例
     * @param validationHints 要传递给验证引擎的一个或多个提示对象
     * @throws IllegalArgumentException 如果 {@code Validator} 或 {@code Errors} 参数中的任何一个是 {@code null}，或者如果提供的 {@code Validator} 不 {@link Validator#supports(Class) 支持}提供的验证对象的类型
     */
    public static void invokeValidator(Validator validator, Object target, Errors errors, Object... validationHints) {
        Assert.notNull(validator, "Validator must not be null");
        Assert.notNull(target, "Target object must not be null");
        Assert.notNull(errors, "Errors object must not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("Invoking validator [" + validator + "]");
        }
        if (!validator.supports(target.getClass())) {
            throw new IllegalArgumentException("Validator [" + validator.getClass() + "] does not support [" + target.getClass() + "]");
        }
        if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
            ((SmartValidator) validator).validate(target, errors, validationHints);
        } else {
            validator.validate(target, errors);
        }
        if (logger.isDebugEnabled()) {
            if (errors.hasErrors()) {
                logger.debug("Validator found " + errors.getErrorCount() + " errors");
            } else {
                logger.debug("Validator found no errors");
            }
        }
    }

    /**
     * 如果值为空，则拒绝具有给定错误代码的给定字段。
     * <p>此上下文中的“空”值表示 {@code null} 或空字符串“”。
     * <p>不需要传入其字段正在验证的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标对象的内部引用）。
     *
     * @param errors    {@code Errors} 实例来注册错误
     * @param field     要检查的字段名称
     * @param errorCode 错误代码，可解释为消息键
     */
    public static void rejectIfEmpty(Errors errors, String field, String errorCode) {
        rejectIfEmpty(errors, field, errorCode, null, null);
    }

    /**
     * 如果值为空，则拒绝具有给定错误代码和默认消息的给定字段。
     * <p>此上下文中的“空”值表示 {@code null} 或空字符串“”。
     * <p>不需要传入其字段正在验证的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标对象的内部引用）。
     *
     * @param errors         {@code Errors} 实例来注册错误
     * @param field          要检查的字段名称
     * @param errorCode      错误代码，可解释为消息键
     * @param defaultMessage 后备默认消息
     */
    public static void rejectIfEmpty(Errors errors, String field, String errorCode, String defaultMessage) {
        rejectIfEmpty(errors, field, errorCode, null, defaultMessage);
    }

    /**
     * 如果值为空，则拒绝具有给定错误代码和错误参数的给定字段。
     * <p>此上下文中的“空”值表示 {@code null} 或空字符串“”。
     * <p>不需要传入其字段正在验证的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标对象的内部引用）。
     *
     * @param errors    {@code Errors} 实例来注册错误
     * @param field     要检查的字段名称
     * @param errorCode 错误代码，可解释为消息键
     * @param errorArgs 错误参数，用于通过 MessageFormat 进行参数绑定（可以是 {@code null}）
     */
    public static void rejectIfEmpty(Errors errors, String field, String errorCode, Object[] errorArgs) {
        rejectIfEmpty(errors, field, errorCode, errorArgs, null);
    }

    /**
     * 如果值为空，则拒绝具有给定错误代码、错误参数和默认消息的给定字段。
     * <p>此上下文中的“空”值表示 {@code null} 或空字符串“”。
     * <p>不需要传入其字段正在验证的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标对象的内部引用）。
     *
     * @param errors         {@code Errors} 实例来注册错误
     * @param field          要检查的字段名称
     * @param errorCode      错误代码，可解释为消息键
     * @param errorArgs      错误参数，用于通过 MessageFormat 进行参数绑定（可以是 {@code null}）
     * @param defaultMessage 后备默认消息
     */
    public static void rejectIfEmpty(Errors errors, String field, String errorCode, Object[] errorArgs, String defaultMessage) {
        Assert.notNull(errors, "Errors object must not be null");
        Object value = errors.getFieldValue(field);
        if (value == null || !StringUtils.hasLength(value.toString())) {
            errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
        }
    }

    /**
     * 如果值为空或仅包含空格，则拒绝具有给定错误代码的给定字段。
     * <p>此上下文中的“空”值表示 {@code null}、空字符串“”或完全由空格组成。
     * <p>不需要传入其字段正在验证的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标对象的内部引用）。
     *
     * @param errors    {@code Errors} 实例来注册错误
     * @param field     要检查的字段名称
     * @param errorCode 错误代码，可解释为消息键
     */
    public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode) {
        rejectIfEmptyOrWhitespace(errors, field, errorCode, null, null);
    }

    /**
     * 如果值为空或仅包含空格，则拒绝具有给定错误代码和默认消息的给定字段。
     * <p>此上下文中的“空”值表示 {@code null}、空字符串“”或完全由空格组成。
     * <p>不需要传入其字段正在验证的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标对象的内部引用）。
     *
     * @param errors         {@code Errors} 实例来注册错误
     * @param field          要检查的字段名称
     * @param errorCode      错误代码，可解释为消息键
     * @param defaultMessage 后备默认消息
     */
    public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode, String defaultMessage) {
        rejectIfEmptyOrWhitespace(errors, field, errorCode, null, defaultMessage);
    }

    /**
     * 如果值为空或仅包含空格，则拒绝具有给定错误代码和错误参数的给定字段。
     * <p>此上下文中的“空”值表示 {@code null}、空字符串“”或完全由空格组成。
     * <p>不需要传入其字段正在验证的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标对象的内部引用）。
     *
     * @param errors    {@code Errors} 实例来注册错误
     * @param field     要检查的字段名称
     * @param errorCode 错误代码，可解释为消息键
     * @param errorArgs 错误参数，用于通过 MessageFormat 进行参数绑定（可以是 {@code null}）
     */
    public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode, Object[] errorArgs) {
        rejectIfEmptyOrWhitespace(errors, field, errorCode, errorArgs, null);
    }

    /**
     * 如果值为空或仅包含空格，则拒绝具有给定错误代码、错误参数和默认消息的给定字段。
     * <p>此上下文中的“空”值表示 {@code null}、空字符串“”或完全由空格组成。
     * <p>不需要传入其字段正在验证的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标对象的内部引用）。
     *
     * @param errors         {@code Errors} 实例来注册错误
     * @param field          要检查的字段名称
     * @param errorCode      错误代码，可解释为消息键
     * @param errorArgs      错误参数，用于通过 MessageFormat 进行参数绑定（可以是 {@code null}）
     * @param defaultMessage 后备默认消息
     */
    public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode, Object[] errorArgs, String defaultMessage) {
        Assert.notNull(errors, "Errors object must not be null");
        Object value = errors.getFieldValue(field);
        if (value == null || !StringUtils.hasText(value.toString())) {
            errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
        }
    }
}
