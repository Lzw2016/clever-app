package org.clever.transaction.support;

import org.clever.core.Constants;
import org.clever.transaction.TransactionDefinition;

import java.io.Serializable;

/**
 * 默认实现 {@link TransactionDefinition} 接口,
 * 提供bean风格的配置和合理的默认值(PROPAGATION_REQUIRED, ISOLATION_DEFAULT, TIMEOUT_DEFAULT, readOnly=false)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:39 <br/>
 */
public class DefaultTransactionDefinition implements TransactionDefinition, Serializable {
    /**
     * TransactionDefinition中定义的传播常量的前缀
     */
    public static final String PREFIX_PROPAGATION = "PROPAGATION_";
    /**
     * TransactionDefinition中定义的隔离常量的前缀
     */
    public static final String PREFIX_ISOLATION = "ISOLATION_";
    /**
     * 描述字符串中事务超时值的前缀
     */
    public static final String PREFIX_TIMEOUT = "timeout_";
    /**
     * 描述字符串中只读事务的标记
     */
    public static final String READ_ONLY_MARKER = "readOnly";

    /**
     * TransactionDefinition的常量实例
     */
    static final Constants constants = new Constants(TransactionDefinition.class);
    private int propagationBehavior = PROPAGATION_REQUIRED;
    private int isolationLevel = ISOLATION_DEFAULT;
    private int timeout = TIMEOUT_DEFAULT;
    private boolean readOnly = false;
    private String name;

    /**
     * 使用默认设置创建新的DefaultTransactionDefinition
     *
     * @see #setPropagationBehavior
     * @see #setIsolationLevel
     * @see #setTimeout
     * @see #setReadOnly
     * @see #setName
     */
    public DefaultTransactionDefinition() {
    }

    /**
     * 复制{@code other}对象属性的构造函数
     *
     * @see #setPropagationBehavior
     * @see #setIsolationLevel
     * @see #setTimeout
     * @see #setReadOnly
     * @see #setName
     */
    public DefaultTransactionDefinition(TransactionDefinition other) {
        this.propagationBehavior = other.getPropagationBehavior();
        this.isolationLevel = other.getIsolationLevel();
        this.timeout = other.getTimeout();
        this.readOnly = other.isReadOnly();
        this.name = other.getName();
    }

    /**
     * 使用给定的传播行为创建新的DefaultTransactionDefinition
     *
     * @param propagationBehavior TransactionDefinition接口中的一个传播常量
     * @see #setIsolationLevel
     * @see #setTimeout
     * @see #setReadOnly
     */
    public DefaultTransactionDefinition(int propagationBehavior) {
        this.propagationBehavior = propagationBehavior;
    }

    /**
     * 根据TransactionDefinition中相应常量的名称设置传播行为，例如 "PROPAGATION_REQUIRED"
     *
     * @param constantName 常量的名称
     * @throws IllegalArgumentException 如果提供的值无法解析为 {@code PROPAGATION_} 常量或 {@code null}
     * @see #setPropagationBehavior
     * @see #PROPAGATION_REQUIRED
     */
    public final void setPropagationBehaviorName(String constantName) throws IllegalArgumentException {
        if (!constantName.startsWith(PREFIX_PROPAGATION)) {
            throw new IllegalArgumentException("Only propagation constants allowed");
        }
        setPropagationBehavior(constants.asNumber(constantName).intValue());
    }

    /**
     * 设置传播行为。必须是TransactionDefinition接口中的传播常量之一。默认值为 PROPAGATION_REQUIRED.
     *
     * @throws IllegalArgumentException 如果提供的值不是 {@code PROPAGATION_} 常量
     * @see #PROPAGATION_REQUIRED
     */
    public final void setPropagationBehavior(int propagationBehavior) {
        if (!constants.getValues(PREFIX_PROPAGATION).contains(propagationBehavior)) {
            throw new IllegalArgumentException("Only values of propagation constants allowed");
        }
        this.propagationBehavior = propagationBehavior;
    }

    @Override
    public final int getPropagationBehavior() {
        return this.propagationBehavior;
    }

    /**
     * 通过TransactionDefinition中相应常量的名称设置隔离级别，例如 "ISOLATION_DEFAULT".
     *
     * @param constantName 常量的名称
     * @throws IllegalArgumentException 如果提供的值无法解析为 {@code ISOLATION_} 常量或 {@code null}
     * @see #setIsolationLevel
     * @see #ISOLATION_DEFAULT
     */
    public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
        if (!constantName.startsWith(PREFIX_ISOLATION)) {
            throw new IllegalArgumentException("Only isolation constants allowed");
        }
        setIsolationLevel(constants.asNumber(constantName).intValue());
    }

    /**
     * 设置隔离级别。必须是TransactionDefinition接口中的隔离常量之一。默认值为 ISOLATION_DEFAULT.
     *
     * @throws IllegalArgumentException 如果提供的值无法解析为 {@code ISOLATION_} 常量或 {@code null}
     * @see #ISOLATION_DEFAULT
     */
    public final void setIsolationLevel(int isolationLevel) {
        if (!constants.getValues(PREFIX_ISOLATION).contains(isolationLevel)) {
            throw new IllegalArgumentException("Only values of isolation constants allowed");
        }
        this.isolationLevel = isolationLevel;
    }

    @Override
    public final int getIsolationLevel() {
        return this.isolationLevel;
    }

    /**
     * 将应用超时设置为秒数。默认值为 TIMEOUT_DEFAULT (-1)
     *
     * @see #TIMEOUT_DEFAULT
     */
    public final void setTimeout(int timeout) {
        if (timeout < TIMEOUT_DEFAULT) {
            throw new IllegalArgumentException("Timeout must be a positive integer or TIMEOUT_DEFAULT");
        }
        this.timeout = timeout;
    }

    @Override
    public final int getTimeout() {
        return this.timeout;
    }

    /**
     * 设置是否优化为只读事务。默认值为 "false".
     */
    public final void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public final boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * 设置此事务的名称。默认值为 none.
     */
    public final void setName(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    /**
     * 此实现比较{@code toString()}结果
     *
     * @see #toString()
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof TransactionDefinition && toString().equals(other.toString())));
    }

    /**
     * 此实现返回{@code toString()}的hashCode
     *
     * @see #toString()
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * 返回此事务定义的标识描述。
     *
     * @see #getDefinitionDescription()
     */
    @Override
    public String toString() {
        return getDefinitionDescription().toString();
    }

    /**
     * 返回此事务定义的标识描述。
     */
    protected final StringBuilder getDefinitionDescription() {
        StringBuilder result = new StringBuilder();
        result.append(constants.toCode(this.propagationBehavior, PREFIX_PROPAGATION));
        result.append(',');
        result.append(constants.toCode(this.isolationLevel, PREFIX_ISOLATION));
        if (this.timeout != TIMEOUT_DEFAULT) {
            result.append(',');
            result.append(PREFIX_TIMEOUT).append(this.timeout);
        }
        if (this.readOnly) {
            result.append(',');
            result.append(READ_ONLY_MARKER);
        }
        return result;
    }
}
