package org.clever.util.function;

import java.util.function.Supplier;

/**
 * 方便{@link java.util.function.Supplier}处理的实用程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:23 <br/>
 *
 * @see SingletonSupplier
 */
public abstract class SupplierUtils {
    /**
     * 解析给定的{@code Supplier}，获得其结果，如果供应商本身为null，则立即返回null
     *
     * @param supplier 要解决的供应商
     * @return {@code Supplier}的结果，如果没有，则为空
     */
    public static <T> T resolve(Supplier<T> supplier) {
        return (supplier != null ? supplier.get() : null);
    }
}
