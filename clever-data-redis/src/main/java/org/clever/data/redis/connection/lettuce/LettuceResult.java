package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.protocol.RedisCommand;
import org.clever.core.convert.converter.Converter;
import org.clever.data.redis.connection.FutureResult;

import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Lettuce特定的 {@link FutureResult} 实现 <br />
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:54 <br/>
 */
class LettuceResult<T, R> extends FutureResult<RedisCommand<?, T, ?>> {
    private final boolean convertPipelineAndTxResults;

    @SuppressWarnings({"unchecked", "rawtypes"})
    LettuceResult(Future<T> resultHolder) {
        this(resultHolder, false, (Converter) val -> val);
    }

    LettuceResult(Future<T> resultHolder, boolean convertPipelineAndTxResults, Converter<T, R> converter) {
        this(resultHolder, () -> null, convertPipelineAndTxResults, converter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    LettuceResult(Future<T> resultHolder, Supplier<R> defaultReturnValue, boolean convertPipelineAndTxResults, Converter<T, R> converter) {
        super((RedisCommand) resultHolder, converter, defaultReturnValue);
        this.convertPipelineAndTxResults = convertPipelineAndTxResults;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        return (T) getResultHolder().getOutput().get();
    }

    @Override
    public boolean conversionRequired() {
        return convertPipelineAndTxResults;
    }

    /**
     * Lettuce特定的 {@link FutureResult} 实现丢弃状态结果
     */
    static class LettuceStatusResult<T, R> extends LettuceResult<T, R> {
        LettuceStatusResult(Future<T> resultHolder) {
            super(resultHolder);
            setStatus(true);
        }
    }

    /**
     * 用于构造 {@link LettuceResult} 的生成器
     */
    static class LettuceResultBuilder<T, R> {
        private final Future<T> response;
        private Converter<T, R> converter;
        private boolean convertPipelineAndTxResults = false;
        private Supplier<R> nullValueDefault = () -> null;

        @SuppressWarnings("unchecked")
        LettuceResultBuilder(Future<T> response) {
            this.response = response;
            this.converter = (source) -> (R) source;
        }

        /**
         * 创建一个新的 {@link LettuceResultBuilder} 给定 {@link Future}
         *
         * @param response 不得为 {@literal null}
         * @param <T>      本机响应类型
         * @param <R>      生成的响应类型
         * @return 新的 {@link LettuceResultBuilder}
         */
        static <T, R> LettuceResultBuilder<T, R> forResponse(Future<T> response) {
            return new LettuceResultBuilder<>(response);
        }

        /**
         * 配置 {@link Converter} 以在 {@code T} 和 {@code R} 类型之间进行转换
         *
         * @param converter 不得为 {@literal null}
         * @return {@code this} 生成器
         */
        LettuceResultBuilder<T, R> mappedWith(Converter<T, R> converter) {
            this.converter = converter;
            return this;
        }

        /**
         * 配置 {@link Supplier} 以将 {@literal null} 响应映射到其他值
         *
         * @param supplier 不得为 {@literal null}
         * @return {@code this} 生成器
         */
        LettuceResultBuilder<T, R> defaultNullTo(Supplier<R> supplier) {
            this.nullValueDefault = supplier;
            return this;
        }

        LettuceResultBuilder<T, R> convertPipelineAndTxResults(boolean flag) {
            convertPipelineAndTxResults = flag;
            return this;
        }

        /**
         * @return 一个新的 {@link LettuceResult} 包装器，其中包含从此构建器应用的配置
         */
        LettuceResult<T, R> build() {
            return new LettuceResult<>(response, nullValueDefault, convertPipelineAndTxResults, converter);
        }

        /**
         * @return 一个新的 {@link LettuceResult} 包装器，用于从此构建器应用配置的状态结果
         */
        LettuceResult<T, R> buildStatusResult() {
            return new LettuceStatusResult<>(response);
        }
    }
}
