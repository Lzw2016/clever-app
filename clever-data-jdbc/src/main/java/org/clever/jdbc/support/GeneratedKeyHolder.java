package org.clever.jdbc.support;

import org.clever.dao.DataRetrievalFailureException;
import org.clever.dao.InvalidDataAccessApiUsageException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * {@link KeyHolder} 接口的标准实现，用于保存自动生成的键（可能由 JDBC 插入语句返回）
 *
 * <p>为每个插入操作创建一个此类的实例，并将其传递给相应的{@link org.clever.jdbc.core.JdbcTemplate}方法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 20:02 <br/>
 */
public class GeneratedKeyHolder implements KeyHolder {
    private final List<Map<String, Object>> keyList;

    /**
     * 使用默认列表创建一个新的 GeneratedKeyHolder
     */
    public GeneratedKeyHolder() {
        this.keyList = new ArrayList<>(1);
    }

    /**
     * 使用给定列表创建一个新的 GeneratedKeyHolder
     *
     * @param keyList 保存键映射的列表
     */
    public GeneratedKeyHolder(List<Map<String, Object>> keyList) {
        this.keyList = keyList;
    }

    @Override
    public Number getKey() throws InvalidDataAccessApiUsageException, DataRetrievalFailureException {
        return getKeyAs(Number.class);
    }

    @Override
    public <T> T getKeyAs(Class<T> keyType) throws InvalidDataAccessApiUsageException, DataRetrievalFailureException {
        if (this.keyList.isEmpty()) {
            return null;
        }
        if (this.keyList.size() > 1 || this.keyList.get(0).size() > 1) {
            throw new InvalidDataAccessApiUsageException(
                    "The getKey method should only be used when a single key is returned. " +
                            "The current key entry contains multiple keys: " +
                            this.keyList
            );
        }
        Iterator<Object> keyIter = this.keyList.get(0).values().iterator();
        if (keyIter.hasNext()) {
            Object key = keyIter.next();
            if (key == null || !(keyType.isAssignableFrom(key.getClass()))) {
                throw new DataRetrievalFailureException(
                        "The generated key type is not supported. " +
                                "Unable to cast [" + (key != null ? key.getClass().getName() : null) +
                                "] to [" + keyType.getName() + "]."
                );
            }
            return keyType.cast(key);
        } else {
            throw new DataRetrievalFailureException(
                    "Unable to retrieve the generated key. " +
                            "Check that the table has an identity column enabled."
            );
        }
    }

    @Override
    public Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException {
        if (this.keyList.isEmpty()) {
            return null;
        }
        if (this.keyList.size() > 1) {
            throw new InvalidDataAccessApiUsageException(
                    "The getKeys method should only be used when keys for a single row are returned. " +
                            "The current key list contains keys for multiple rows: " + this.keyList
            );
        }
        return this.keyList.get(0);
    }

    @Override
    public List<Map<String, Object>> getKeyList() {
        return this.keyList;
    }
}
