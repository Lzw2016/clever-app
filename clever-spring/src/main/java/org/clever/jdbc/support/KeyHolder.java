package org.clever.jdbc.support;

import org.clever.dao.InvalidDataAccessApiUsageException;

import java.util.List;
import java.util.Map;

/**
 * 检索键的接口，通常用于自动生成的键，这些键可能由JDBC insert语句返回。
 *
 * <p>此接口的实现可以保存任意数量的键。在一般情况下，键作为一个列表返回，其中每行键包含一个Map。
 *
 * <p>大多数应用程序每行只使用一个键，并且在insert语句中一次只处理一行。
 * 在这些情况下，只需调用{@link #getKey() getKey}或{@link #getKeyAs(Class) getKeyAs}来检索密钥。
 * getKey返回的值是一个{@link Number}，这是自动生成的键的常见类型。
 * 对于任何其他自动生成的密钥类型，请改用{@code getKeyAs}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:35 <br/>
 *
 * @see org.clever.jdbc.core.JdbcTemplate
 */
public interface KeyHolder {
    /**
     * 从第一个Map中检索第一个项，假设只有一个项和一个Map，并且该项是一个数字。这是典型的情况：单个数字生成的键。
     * <p>关键点保存在地图列表中，其中列表中的每个项目表示每行的关键点。
     * 如果有多个列，则地图也将有多个条目。
     * 如果此方法在Map或列表中遇到多个条目，意味着返回了多个键，则抛出InvalidDataAccessApiUsageException。
     *
     * @return 生成的密钥作为数字
     * @throws InvalidDataAccessApiUsageException 如果遇到多个键
     * @see #getKeyAs(Class)
     */
    Number getKey() throws InvalidDataAccessApiUsageException;

    /**
     * 从第一个Map中检索第一个项，假设只有一个项和一个Map，并且该项是指定类型的实例。这是一种常见情况：指定类型的单个生成键。
     * <p>关键点保存在地图列表中，其中列表中的每个项目表示每行的关键点。
     * 如果有多个列，则地图也将有多个条目。
     * 如果此方法在Map或列表中遇到多个条目，意味着返回了多个键，则抛出InvalidDataAccessApiUsageException。
     *
     * @param keyType 自动生成密钥的类型
     * @return 作为指定类型的实例生成的密钥
     * @throws InvalidDataAccessApiUsageException 如果遇到多个键
     * @see #getKey()
     */
    <T> T getKeyAs(Class<T> keyType) throws InvalidDataAccessApiUsageException;

    /**
     * 检索第一个键Map。
     * <p>如果列表中有多个条目（意味着多行返回了键），则抛出InvalidDataAccessApiUsageException。
     *
     * @return 单行生成的键的Map
     * @throws InvalidDataAccessApiUsageException 如果遇到多行的键
     */
    Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException;

    /**
     * 返回对包含键的列表的引用。
     * <p>可以用于提取多行的键（一种罕见的情况），也可以用于添加键的新Map。
     *
     * @return 生成键的列表，每个条目通过列名和键值的Map表示一行
     */
    List<Map<String, Object>> getKeyList();
}
