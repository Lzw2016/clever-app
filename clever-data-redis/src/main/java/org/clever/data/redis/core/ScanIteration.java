package org.clever.data.redis.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * {@link ScanIteration} 保存执行 {@literal SCAN} 命令时Redis {@literal Multibulk reply} 中包含的值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:38 <br/>
 */
public class ScanIteration<T> implements Iterable<T> {
    private final long cursorId;
    private final Collection<T> items;

    public ScanIteration(long cursorId, Collection<T> items) {
        this.cursorId = cursorId;
        this.items = (items != null ? new ArrayList<>(items) : Collections.emptyList());
    }

    /**
     * 用于后续请求的游标id
     */
    public long getCursorId() {
        return cursorId;
    }

    /**
     * 获取返回的项目
     */
    public Collection<T> getItems() {
        return items;
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}
