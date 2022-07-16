package org.clever.dao.support;

import org.clever.dao.DataAccessException;
import org.clever.dao.EmptyResultDataAccessException;
import org.clever.dao.IncorrectResultSizeDataAccessException;
import org.clever.dao.TypeMismatchDataAccessException;
import org.clever.util.Assert;
import org.clever.util.CollectionUtils;
import org.clever.util.NumberUtils;

import java.util.Collection;

/**
 * DAO实现的各种实用工具方法。适用于任何数据访问技术。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:41 <br/>
 */
public abstract class DataAccessUtils {
    /**
     * 从给定集合返回单个结果对象。
     * <p>如果找到0个结果对象，则返回null；如果找到多个元素，则引发异常。
     *
     * @param results 结果集合（可以为空）
     * @return 单个结果对象，如果没有，则为null
     * @throws IncorrectResultSizeDataAccessException 如果在给定集合中找到多个元素
     */
    public static <T> T singleResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
        if (CollectionUtils.isEmpty(results)) {
            return null;
        }
        if (results.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(1, results.size());
        }
        return results.iterator().next();
    }

    /**
     * 从给定集合返回单个结果对象。
     * <p>如果找到0个或多个元素，则引发异常。
     *
     * @param results 结果集合（可以为null，但不应包含null元素）
     * @return 单个结果对象
     * @throws IncorrectResultSizeDataAccessException 如果在给定集合中找到多个元素
     * @throws EmptyResultDataAccessException         如果在给定集合中找不到任何元素
     */
    public static <T> T requiredSingleResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
        if (CollectionUtils.isEmpty(results)) {
            throw new EmptyResultDataAccessException(1);
        }
        if (results.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(1, results.size());
        }
        return results.iterator().next();
    }

    /**
     * 从给定集合返回单个结果对象。
     * <p>如果找到0个或多个元素，则引发异常。
     *
     * @param results 结果集合（可以为null，也应包含null元素）
     * @return 单个结果对象
     * @throws IncorrectResultSizeDataAccessException 如果在给定集合中找到多个元素
     * @throws EmptyResultDataAccessException         如果在给定集合中找不到任何元素
     */
    public static <T> T nullableSingleResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
        // This is identical to the requiredSingleResult implementation but differs in the
        // semantics of the incoming Collection (which we currently can't formally express)
        if (CollectionUtils.isEmpty(results)) {
            throw new EmptyResultDataAccessException(1);
        }
        if (results.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(1, results.size());
        }
        return results.iterator().next();
    }

    /**
     * 从给定集合返回唯一的结果对象。
     * <p>如果找到0个结果对象，则返回null；如果找到多个实例，则引发异常。
     *
     * @param results 结果集合（可以为空）
     * @return 唯一的结果对象，如果没有，则为null
     * @throws IncorrectResultSizeDataAccessException 如果在给定集合中找到多个结果对象
     * @see org.clever.util.CollectionUtils#hasUniqueObject
     */
    public static <T> T uniqueResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
        if (CollectionUtils.isEmpty(results)) {
            return null;
        }
        if (!CollectionUtils.hasUniqueObject(results)) {
            throw new IncorrectResultSizeDataAccessException(1, results.size());
        }
        return results.iterator().next();
    }

    /**
     * 从给定集合返回唯一的结果对象。
     * <p>如果找到0个或多个实例，则引发异常。
     *
     * @param results 结果集合（可以为null，但不应包含null元素）
     * @return 唯一结果对象
     * @throws IncorrectResultSizeDataAccessException 如果在给定集合中找到多个结果对象
     * @throws EmptyResultDataAccessException         如果在给定集合中找不到任何结果对象
     * @see org.clever.util.CollectionUtils#hasUniqueObject
     */
    public static <T> T requiredUniqueResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
        if (CollectionUtils.isEmpty(results)) {
            throw new EmptyResultDataAccessException(1);
        }
        if (!CollectionUtils.hasUniqueObject(results)) {
            throw new IncorrectResultSizeDataAccessException(1, results.size());
        }
        return results.iterator().next();
    }

    /**
     * 从给定集合返回唯一的结果对象。如果找到0个或多个结果对象，或者唯一结果对象无法转换为指定的必需类型，则引发异常。
     *
     * @param results 结果集合（可以为null，但不应包含null元素）
     * @return 唯一结果对象
     * @throws IncorrectResultSizeDataAccessException 如果在给定集合中找到多个结果对象
     * @throws EmptyResultDataAccessException         如果在给定集合中找不到任何结果对象
     * @throws TypeMismatchDataAccessException        如果唯一对象与指定的必需类型不匹配
     */
    @SuppressWarnings("unchecked")
    public static <T> T objectResult(Collection<?> results, Class<T> requiredType) throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {
        Object result = requiredUniqueResult(results);
        if (requiredType != null && !requiredType.isInstance(result)) {
            if (String.class == requiredType) {
                result = result.toString();
            } else if (Number.class.isAssignableFrom(requiredType) && result instanceof Number) {
                try {
                    result = NumberUtils.convertNumberToTargetClass(((Number) result), (Class<? extends Number>) requiredType);
                } catch (IllegalArgumentException ex) {
                    throw new TypeMismatchDataAccessException(ex.getMessage());
                }
            } else {
                throw new TypeMismatchDataAccessException(
                        "Result object is of type [" + result.getClass().getName() +
                                "] and could not be converted to required type [" + requiredType.getName() + "]"
                );
            }
        }
        return (T) result;
    }

    /**
     * 从给定集合返回唯一的int结果。如果找到0个或多个结果对象，或者如果唯一结果对象不能转换为int，则引发异常。
     *
     * @param results 结果集合（可以为null，但不应包含null元素）
     * @return 唯一的int结果
     * @throws IncorrectResultSizeDataAccessException 如果在给定集合中找到多个结果对象
     * @throws EmptyResultDataAccessException         如果在给定集合中找不到任何结果对象
     * @throws TypeMismatchDataAccessException        如果集合中的唯一对象不能转换为int
     */
    public static int intResult(Collection<?> results) throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {
        return objectResult(results, Number.class).intValue();
    }

    /**
     * 从给定集合返回唯一的长结果。如果找到0个或多个结果对象，或者如果唯一结果对象无法转换为long，则引发异常。
     *
     * @param results 结果集合（可以为null，但不应包含null元素）
     * @return 唯一的long结果
     * @throws IncorrectResultSizeDataAccessException 如果在给定集合中找到多个结果对象
     * @throws EmptyResultDataAccessException         如果在给定集合中找不到任何结果对象
     * @throws TypeMismatchDataAccessException        如果集合中的唯一对象无法转换为long
     */
    public static long longResult(Collection<?> results) throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {
        return objectResult(results, Number.class).longValue();
    }

    /**
     * 如果合适，请返回已翻译的异常，否则按原样返回给定的异常。
     *
     * @param rawException 我们可能希望翻译的例外情况
     * @param pet          用于执行转换的PersistenceExceptionTranslator
     * @return 如果可以转换，则为已转换的持久性异常；如果不能转换，则为原始异常
     */
    public static RuntimeException translateIfNecessary(RuntimeException rawException, PersistenceExceptionTranslator pet) {
        Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
        DataAccessException dae = pet.translateExceptionIfPossible(rawException);
        return (dae != null ? dae : rawException);
    }
}
