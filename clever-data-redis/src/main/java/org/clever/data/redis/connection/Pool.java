//package org.clever.data.redis.connection;
//
///**
// * 资源池
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/29 23:08 <br/>
// */
//public interface Pool<T> {
//    /**
//     * @return 资源（如果可用）
//     */
//    T getResource();
//
//    /**
//     * @param resource 失效的损坏资源
//     */
//    void returnBrokenResource(final T resource);
//
//    /**
//     * @param resource 要返回到池的资源
//     */
//    void returnResource(final T resource);
//
//    /**
//     * 销毁池
//     */
//    void destroy();
//}
