package org.clever.data.redis.connection;

import org.clever.core.convert.converter.Converter;

import java.util.function.Supplier;

/**
 * 异步操作的结果
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:52 <br/>
 *
 * @param <T> 保存未来结果的对象的数据类型（通常是 {@link java.util.concurrent.Future} 或响应包装器的类型）
 */
public abstract class FutureResult<T> {
    private final T resultHolder;
    private final Supplier<?> defaultConversionResult;
    private boolean status = false;
    @SuppressWarnings("rawtypes")
    protected Converter converter;

    /**
     * 为实际保存结果本身的给定对象创建新的 {@link FutureResult}
     *
     * @param resultHolder 不得为 {@literal null}
     */
    public FutureResult(T resultHolder) {
        this(resultHolder, val -> val);
    }

    /**
     * 为实际保存结果本身的给定对象创建新的 {@link FutureResult}，以及能够通过 {@link #convert(Object)} 生成结果的转换器。
     *
     * @param resultHolder 不得为 {@literal null}
     * @param converter    可以是 {@literal null}，并将默认为标识转换器 {@code value -> value} 以保留原始值。
     */
    @SuppressWarnings("rawtypes")
    public FutureResult(T resultHolder, Converter converter) {
        this(resultHolder, converter, () -> null);
    }

    /**
     * 为实际保存结果本身的给定对象创建新的 {@link FutureResult}，以及能够通过 {@link #convert(Object)} 转换结果的转换器。
     *
     * @param resultHolder            不得为 {@literal null}
     * @param converter               可以是 {@literal null} 并将默认为标识转换器 {@code value -> value} 到保留原始值
     * @param defaultConversionResult 不得为 {@literal null}
     */
    @SuppressWarnings("rawtypes")
    public FutureResult(T resultHolder, Converter converter, Supplier<?> defaultConversionResult) {
        this.resultHolder = resultHolder;
        this.converter = converter != null ? converter : val -> val;
        this.defaultConversionResult = defaultConversionResult;
    }

    /**
     * 获取保存实际结果的对象
     *
     * @return 从不 {@literal null}
     */
    public T getResultHolder() {
        return resultHolder;
    }

    /**
     * 如果指定了转换器，则转换给定的结果，否则返回结果
     *
     * @param result 要转换的结果。可以是 {@literal null}
     * @return 转换后的结果或 {@literal null}
     */
    @SuppressWarnings("unchecked")
    public Object convert(Object result) {
        if (result == null) {
            return computeDefaultResult(null);
        }
        return computeDefaultResult(converter.convert(result));
    }

    private Object computeDefaultResult(Object source) {
        return source != null ? source : defaultConversionResult.get();
    }

    @SuppressWarnings("rawtypes")
    public Converter getConverter() {
        return converter;
    }

    /**
     * 指示此结果是否为操作的状态。通常，状态结果将在转换时被丢弃。
     *
     * @return 如果这是状态结果，则为 true（即“OK”）
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * 指示此结果是否为操作的状态。通常，状态结果将在转换时被丢弃。
     */
    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * @return 操作的结果。可以是 {@literal null}
     */
    public abstract Object get();

    /**
     * 在移交之前，请指出实际结果是否需要 {@link #convert(Object) converted}
     *
     * @return {@literal true} 如果需要结果转换
     */
    public abstract boolean conversionRequired();
}
