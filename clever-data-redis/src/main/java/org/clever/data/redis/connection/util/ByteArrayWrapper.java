package org.clever.data.redis.connection.util;

import java.util.Arrays;

/**
 * 用于包装数组的简单包装类，以便它们可以用作映射中的键
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 23:03 <br/>
 */
public class ByteArrayWrapper {
    private final byte[] array;
    private final int hashCode;

    public ByteArrayWrapper(byte[] array) {
        this.array = array;
        this.hashCode = Arrays.hashCode(array);
    }

    public boolean equals(Object obj) {
        if (obj instanceof ByteArrayWrapper) {
            return Arrays.equals(array, ((ByteArrayWrapper) obj).array);
        }
        return false;
    }

    public int hashCode() {
        return hashCode;
    }

    /**
     * 返回数组
     *
     * @return 返回数组
     */
    public byte[] getArray() {
        return array;
    }
}
