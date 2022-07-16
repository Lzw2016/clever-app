package org.clever.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * 简单LRU（最近使用最少的）缓存，由指定的缓存限制限定。
 *
 * <p>此实现由一个用于存储缓存值的{@code ConcurrentHashMap}和
 * 一个用于排序键并在缓存满容量时选择最近使用最少的键的{@code ConcurrentLinkedDeque}支持。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:59 <br/>
 *
 * @param <K> 用于缓存检索的key类型
 * @param <V> 缓存值的类型
 * @see #get
 */
public class ConcurrentLruCache<K, V> {
    private final int sizeLimit;
    private final Function<K, V> generator;
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile int size;

    /**
     * 使用给定的限制和生成器函数创建一个新的缓存实例。
     *
     * @param sizeLimit 缓存中的最大项数（0表示没有缓存，始终生成新值）
     * @param generator 为给定键生成新值的函数
     */
    public ConcurrentLruCache(int sizeLimit, Function<K, V> generator) {
        Assert.isTrue(sizeLimit >= 0, "Cache size limit must not be negative");
        Assert.notNull(generator, "Generator function must not be null");
        this.sizeLimit = sizeLimit;
        this.generator = generator;
    }

    /**
     * 从缓存中检索条目，可能会触发值的生成。
     *
     * @param key 检索条目的键
     * @return 缓存或新生成的值
     */
    public V get(K key) {
        if (this.sizeLimit == 0) {
            return this.generator.apply(key);
        }
        V cached = this.cache.get(key);
        if (cached != null) {
            if (this.size < this.sizeLimit) {
                return cached;
            }
            this.lock.readLock().lock();
            try {
                if (this.queue.removeLastOccurrence(key)) {
                    this.queue.offer(key);
                }
                return cached;
            } finally {
                this.lock.readLock().unlock();
            }
        }
        this.lock.writeLock().lock();
        try {
            // Retrying in case of concurrent reads on the same key
            cached = this.cache.get(key);
            if (cached != null) {
                if (this.queue.removeLastOccurrence(key)) {
                    this.queue.offer(key);
                }
                return cached;
            }
            // Generate value first, to prevent size inconsistency
            V value = this.generator.apply(key);
            if (this.size == this.sizeLimit) {
                K leastUsed = this.queue.poll();
                if (leastUsed != null) {
                    this.cache.remove(leastUsed);
                }
            }
            this.queue.offer(key);
            this.cache.put(key, value);
            this.size = this.cache.size();
            return value;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * 确定给定密钥是否存在于该缓存中。
     *
     * @param key 要检查的关键
     * @return 如果密钥存在，则为true；如果没有匹配的密钥，则为false
     */
    public boolean contains(K key) {
        return this.cache.containsKey(key);
    }

    /**
     * 立即删除给定键和任何相关值。
     *
     * @param key 退出项的键
     * @return 如果之前存在密钥，则为true；如果没有匹配的密钥，则为false
     */
    public boolean remove(K key) {
        this.lock.writeLock().lock();
        try {
            boolean wasPresent = (this.cache.remove(key) != null);
            this.queue.remove(key);
            this.size = this.cache.size();
            return wasPresent;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * 立即从此缓存中删除所有条目。
     */
    public void clear() {
        this.lock.writeLock().lock();
        try {
            this.cache.clear();
            this.queue.clear();
            this.size = 0;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * 返回缓存的当前大小。
     *
     * @see #sizeLimit()
     */
    public int size() {
        return this.size;
    }

    /**
     * 返回缓存中的最大项数（0表示没有缓存，始终生成新值）。
     *
     * @see #size()
     */
    public int sizeLimit() {
        return this.sizeLimit;
    }
}
