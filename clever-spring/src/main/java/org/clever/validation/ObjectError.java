package org.clever.validation;

import org.clever.context.support.DefaultMessageSourceResolvable;
import org.clever.util.Assert;

/**
 * 封装对象错误，即拒绝对象的全局原因。
 * <p>有关如何为 {@code ObjectError} 构建消息代码列表的详细信息，请参阅 {@link DefaultMessageCodesResolver}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:13 <br/>
 *
 * @see FieldError
 * @see DefaultMessageCodesResolver
 */
public class ObjectError extends DefaultMessageSourceResolvable {
    private final String objectName;
    private transient Object source;

    /**
     * @param objectName     受影响对象的名称
     * @param defaultMessage 用于解析此消息的默认消息
     */
    public ObjectError(String objectName, String defaultMessage) {
        this(objectName, null, null, defaultMessage);
    }

    /**
     * @param objectName     受影响对象的名称
     * @param codes          用于解析此消息的代码
     * @param arguments      用于解析此消息的参数数组
     * @param defaultMessage 用于解析此消息的默认消息
     */
    public ObjectError(String objectName, String[] codes, Object[] arguments, String defaultMessage) {
        super(codes, arguments, defaultMessage);
        Assert.notNull(objectName, "Object name must not be null");
        this.objectName = objectName;
    }

    /**
     * 返回受影响对象的名称
     */
    public String getObjectName() {
        return this.objectName;
    }

    /**
     * 保留此错误背后的来源：可能是 {@link Exception}（通常是 {@link org.clever.beans.PropertyAccessException}）或 Bean 验证 {@link javax.validation.ConstraintViolation}。
     * <p>请注意，任何此类源对象都被存储为瞬态：也就是说，它不会成为序列化错误表示的一部分。
     *
     * @param source 源对象
     */
    public void wrap(Object source) {
        if (this.source != null) {
            throw new IllegalStateException("Already wrapping " + this.source);
        }
        this.source = source;
    }

    /**
     * 解开此错误背后的来源：可能是 {@link Exception}（通常是 {@link org.clever.beans.PropertyAccessException}）或 Bean 验证 {@link javax.validation.ConstraintViolation}。
     * <p>最外层异常的原因也将被内省，例如底层转换异常或从 setter 抛出的异常（而不是必须依次解包 {@code PropertyAccessException}）。
     *
     * @return 给定类型的源对象
     * @throws IllegalArgumentException 如果没有这样的源对象可用（即没有指定或反序列化后不再可用）
     */
    public <T> T unwrap(Class<T> sourceType) {
        if (sourceType.isInstance(this.source)) {
            return sourceType.cast(this.source);
        } else if (this.source instanceof Throwable) {
            Throwable cause = ((Throwable) this.source).getCause();
            if (sourceType.isInstance(cause)) {
                return sourceType.cast(cause);
            }
        }
        throw new IllegalArgumentException("No source object of the given type available: " + sourceType);
    }

    /**
     * 检查此错误背后的来源：可能是 {@link Exception}（通常是 {@link org.clever.beans.PropertyAccessException}）或 Bean 验证 {@link javax.validation.ConstraintViolation}。
     * <p>最外层异常的原因也将被内省，例如底层转换异常或从 setter 抛出的异常（而不是必须依次解包 {@code PropertyAccessException}）。
     *
     * @return 此错误是否由给定类型的源对象引起
     */
    public boolean contains(Class<?> sourceType) {
        return (sourceType.isInstance(this.source) || (this.source instanceof Throwable && sourceType.isInstance(((Throwable) this.source).getCause())));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || other.getClass() != getClass() || !super.equals(other)) {
            return false;
        }
        ObjectError otherError = (ObjectError) other;
        return getObjectName().equals(otherError.getObjectName());
    }

    @Override
    public int hashCode() {
        return (29 * super.hashCode() + getObjectName().hashCode());
    }

    @Override
    public String toString() {
        return "Error in object '" + this.objectName + "': " + resolvableToString();
    }
}
