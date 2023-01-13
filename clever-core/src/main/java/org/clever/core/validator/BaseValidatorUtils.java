package org.clever.core.validator;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

import javax.validation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSR303 Validator工具类<br/>
 *
 * @author LiZW
 * @version 2015年5月28日 下午3:43:21
 */
public class BaseValidatorUtils {
    /**
     * Hibernate实现的ValidatorFactory
     */
    public static final ValidatorFactory HIBERNATE_VALIDATOR_FACTORY;
    /**
     * 全局共享的Bean验证器
     */
    private static final Validator INSTANCE;

    static {
        Configuration<HibernateValidatorConfiguration> configure = Validation.byProvider(HibernateValidator.class).configure();
        HIBERNATE_VALIDATOR_FACTORY = configure.buildValidatorFactory();
        INSTANCE = createHibernateValidator();
    }

    /**
     * 返回Hibernate实现的Validator单例(验证器实例是线程安全的，可以多次重用)
     */
    public static Validator getValidatorInstance() {
        return INSTANCE;
    }

    /**
     * 创建一个新的Hibernate实现的Validator
     *
     * @return Hibernate实现的Validator
     */
    public static Validator createHibernateValidator() {
        return HIBERNATE_VALIDATOR_FACTORY.getValidator();
    }

    /**
     * 验证Bean对象，验证失败时抛出异常
     *
     * @param bean   待验证的bean对象
     * @param groups 验证的组
     * @param <T>    待验证的bean对象类型
     * @throws ConstraintViolationException 验证失败
     */
    public static <T> void validateThrowException(T bean, Class<?>... groups) throws ConstraintViolationException {
        validateThrowException(INSTANCE, bean, groups);
    }

    /**
     * 验证Bean对象，验证失败时抛出异常
     *
     * @param validator JSR303验证器，可以使用不同的实现
     * @param bean      待验证的bean对象
     * @param groups    验证的组
     * @param <T>       待验证的bean对象类型
     * @throws ConstraintViolationException 验证失败
     */
    public static <T> void validateThrowException(Validator validator, T bean, Class<?>... groups) throws ConstraintViolationException {
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(bean, groups);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    /**
     * 验证Bean对象，返回验证结果
     *
     * @param bean   待验证的bean对象
     * @param groups 验证的组
     * @param <T>    待验证的bean对象类型
     */
    public static <T> List<FieldError> validate(T bean, Class<?>... groups) {
        return validate(INSTANCE, bean, groups);
    }

    /**
     * 验证Bean对象，返回验证结果
     *
     * @param validator JSR303验证器，可以使用不同的实现
     * @param bean      待验证的bean对象
     * @param groups    验证的组
     * @param <T>       待验证的bean对象类型
     */
    public static <T> List<FieldError> validate(Validator validator, T bean, Class<?>... groups) {
        Set<ConstraintViolation<T>> constraints = validator.validate(bean, groups);
        if (constraints.isEmpty()) {
            return new ArrayList<>();
        }
        return constraints.stream().map(BaseValidatorUtils::createFieldError).collect(Collectors.toList());
    }

    /**
     * 把验证的约束信息转换成FieldError对象
     */
    public static <T> FieldError createFieldError(ConstraintViolation<T> constraint) {
        return new FieldError(
                constraint.getPropertyPath().toString(),
                constraint.getInvalidValue(),
                constraint.getMessage(),
                constraint.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
        );
    }
}
