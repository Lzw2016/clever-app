//package org.clever.data.redis.core.convert;
//
//import org.clever.data.mapping.PersistentProperty;
//import org.clever.data.util.TypeInformation;
//
//import java.util.Set;
//
///**
// * {@link IndexResolver} 提取要应用于给定路径、{@link PersistentProperty} 和值的二级索引结构
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/02/01 15:27 <br/>
// */
//public interface IndexResolver {
//    /**
//     * 解析给定类型信息值组合的所有索引
//     *
//     * @param typeInformation 不能是 {@literal null}
//     * @param value           实际值。可以是 {@literal null}。
//     * @return 从不为 {@literal null}
//     */
//    Set<IndexedData> resolveIndexesFor(TypeInformation<?> typeInformation, Object value);
//
//    /**
//     * 解析给定类型信息值组合的所有索引
//     *
//     * @param keyspace        不能是 {@literal null}
//     * @param path            不能是 {@literal null}
//     * @param typeInformation 不能是 {@literal null}
//     * @param value           实际值。可以是 {@literal null}
//     * @return 从不为 {@literal null}
//     */
//    Set<IndexedData> resolveIndexesFor(String keyspace, String path, TypeInformation<?> typeInformation, Object value);
//}
