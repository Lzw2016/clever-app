package org.clever.data.redis.connection.convert;

import org.clever.core.convert.converter.Converter;
import org.clever.dao.DataAccessException;
import org.clever.data.redis.RedisSystemException;
import org.clever.data.redis.connection.FutureResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * 使用提供的 {@link FutureResult} 队列转换事务执行的结果。
 * 使用提供的异常 {@link Converter} 转换列表中返回的任何异常对象
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:51 <br/>
 *
 * @param <T> 单个 tx 操作的 {@link FutureResult} 的类型
 */
public class TransactionResultConverter<T> implements Converter<List<Object>, List<Object>> {
    private final Queue<FutureResult<T>> txResults;
    private final Converter<Exception, DataAccessException> exceptionConverter;

    public TransactionResultConverter(Queue<FutureResult<T>> txResults, Converter<Exception, DataAccessException> exceptionConverter) {
        this.txResults = txResults;
        this.exceptionConverter = exceptionConverter;
    }

    @Override
    public List<Object> convert(List<Object> execResults) {
        if (execResults.size() != txResults.size()) {
            throw new IllegalArgumentException("Incorrect number of transaction results. Expected: " + txResults.size() + " Actual: " + execResults.size());
        }
        List<Object> convertedResults = new ArrayList<>();
        for (Object result : execResults) {
            FutureResult<T> futureResult = txResults.remove();
            if (result instanceof Exception) {
                Exception source = (Exception) result;
                DataAccessException convertedException = exceptionConverter.convert(source);
                throw convertedException != null ? convertedException : new RedisSystemException("Error reading future result.", source);
            }
            if (!(futureResult.isStatus())) {
                convertedResults.add(futureResult.conversionRequired() ? futureResult.convert(result) : result);
            }
        }
        return convertedResults;
    }
}
