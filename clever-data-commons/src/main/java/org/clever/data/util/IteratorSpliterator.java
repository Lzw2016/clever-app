package org.clever.data.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 使用给定 Iterator 进行元素操作的 Spliterator。拆分器实现 {@code trySplit} 以允许有限的并行性
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:45 <br/>
 */
class IteratorSpliterator<T> implements Spliterator<T> {
    private static final int BATCH_UNIT = 1 << 10; // batch array size increment
    private static final int MAX_BATCH = 1 << 25; // max batch array size;
    private final Iterator<? extends T> it;
    private long est; // size estimate
    private int batch; // batch size for splits

    /**
     * 使用给定的遍历迭代器创建一个拆分器，并报告给定的初始大小和特征
     *
     * @param iterator 源的迭代器
     */
    public IteratorSpliterator(Iterator<? extends T> iterator) {
        this.it = iterator;
        this.est = Long.MAX_VALUE;
    }

    @Override
    public Spliterator<T> trySplit() {
        /*
         * 分成按算术递增的批量大小的数组。
         * 如果每个元素的消费者操作比将它们转移到数组中成本更高，这只会提高并行性能。
         * 在拆分大小中使用算术级数提供了开销与并行度的界限，这些界限并不特别有利于或惩罚轻量级元素操作与重量级元素操作的情况，跨元素与核心的组合，无论是否已知。
         * 我们生成 O(sqrt(#elements)) 拆分，允许 O(sqrt(#cores)) 潜在加速。
         */
        Iterator<? extends T> i = it;
        long s = est;
        if (s > 1 && i.hasNext()) {
            int n = batch + BATCH_UNIT;
            if (n > s) {
                n = (int) s;
            }
            if (n > MAX_BATCH) {
                n = MAX_BATCH;
            }
            Object[] a = new Object[n];
            int j = 0;
            do {
                a[j] = i.next();
            } while (++j < n && i.hasNext());
            batch = j;
            if (est != Long.MAX_VALUE) {
                est -= j;
            }
            return Spliterators.spliterator(a, 0, j, 0);
        }
        return null;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        it.forEachRemaining(action);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (it.hasNext()) {
            action.accept(it.next());
            return true;
        }
        return false;
    }

    @Override
    public long estimateSize() {
        return -1;
    }

    @Override
    public int characteristics() {
        return 0;
    }

    @Override
    public Comparator<? super T> getComparator() {
        if (hasCharacteristics(Spliterator.SORTED)) {
            return null;
        }
        throw new IllegalStateException();
    }
}
