package org.clever.core;

import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/03/01 16:15 <br/>
 */
public class BatchDataUtils {
    public static final int DEF_BATCH_SIZE = 1000;

    public static <T> List<List<T>> toBatch(Collection<T> data, int batchSize) {
        if (data == null) {
            return new ArrayList<>();
        }
        Assert.isTrue(batchSize > 0, "参数batchSize必须大于0");
        List<List<T>> batchData = new ArrayList<>();
        List<T> batch = new ArrayList<>();
        for (T item : data) {
            if (batch.size() >= batchSize) {
                batchData.add(batch);
                batch = new ArrayList<>();
            }
            batch.add(item);
        }
        if (!batch.isEmpty()) {
            batchData.add(batch);
        }
        return batchData;
    }

    public static <T> List<List<T>> toBatch(Collection<T> data) {
        return toBatch(data, DEF_BATCH_SIZE);
    }

    public static <T> List<List<T>> toBatch(T[] data, int batchSize) {
        return toBatch(Arrays.asList(data), batchSize);
    }

    public static <T> List<List<T>> toBatch(T[] data) {
        return toBatch(data, DEF_BATCH_SIZE);
    }
}
