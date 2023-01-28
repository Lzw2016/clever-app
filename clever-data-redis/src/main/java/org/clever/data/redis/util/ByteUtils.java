package org.clever.data.redis.util;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 一些处理 {@code byte} 数组的简便方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:53 <br/>
 */
public final class ByteUtils {
    private ByteUtils() {
    }

    /**
     * 将给定的 {@code byte} 数组连接为一个数组，重叠的数组元素包含两次<br/>
     * 保留原始数组中元素的顺序
     *
     * @param array1 第一个数组
     * @param array2 第二个数组
     * @return 新数组
     */
    public static byte[] concat(byte[] array1, byte[] array2) {
        byte[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    /**
     * 将给定的 {@code byte} 数组连接为一个数组，重叠的数组元素包含两次。
     * 如果 {@code arrays} 为空，则返回一个新的空数组；如果 {@code array} 仅包含一个数组，则返回第一个数组。
     * <p>
     * 保留原始数组中元素的顺序
     *
     * @param arrays 数组
     * @return 新数组
     */
    public static byte[] concatAll(byte[]... arrays) {
        if (arrays.length == 0) {
            return new byte[]{};
        }
        if (arrays.length == 1) {
            return arrays[0];
        }
        byte[] cur = concat(arrays[0], arrays[1]);
        for (int i = 2; i < arrays.length; i++) {
            cur = concat(cur, arrays[i]);
        }
        return cur;
    }

    /**
     * 使用分隔符 {@code c} 将 {@code source} 拆分为分区数组
     *
     * @param source 源数组
     * @param c      分隔符
     * @return 分区数组
     */
    public static byte[][] split(byte[] source, int c) {
        if (ObjectUtils.isEmpty(source)) {
            return new byte[][]{};
        }
        List<byte[]> bytes = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i <= source.length; i++) {
            if (i == source.length) {
                bytes.add(Arrays.copyOfRange(source, offset, i));
                break;
            }
            if (source[i] == c) {
                bytes.add(Arrays.copyOfRange(source, offset, i));
                offset = i + 1;
            }
        }
        return bytes.toArray(new byte[bytes.size()][]);
    }

    /**
     * 将多个 {@code byte} 数组合并为一个数组
     *
     * @param firstArray       不得为 {@literal null}
     * @param additionalArrays 不得为 {@literal null}
     */
    public static byte[][] mergeArrays(byte[] firstArray, byte[]... additionalArrays) {
        Assert.notNull(firstArray, "first array must not be null");
        Assert.notNull(additionalArrays, "additional arrays must not be null");
        byte[][] result = new byte[additionalArrays.length + 1][];
        result[0] = firstArray;
        System.arraycopy(additionalArrays, 0, result, 1, additionalArrays.length);
        return result;
    }

    /**
     * 从 {@link ByteBuffer} 中提取字节数组，而不使用它。生成的 {@code byte[]} 是缓冲区内容的副本，不会在缓冲区内发生更改时更新
     *
     * @param byteBuffer 不能是 {@literal null}
     */
    public static byte[] getBytes(ByteBuffer byteBuffer) {
        Assert.notNull(byteBuffer, "ByteBuffer must not be null!");
        ByteBuffer duplicate = byteBuffer.duplicate();
        byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return bytes;
    }

    /**
     * 测试 {@code haystack} 是否以给定的 {@code prefix} 开头
     *
     * @param haystack 要扫描的源
     * @param prefix   要查找的前缀
     * @return 如果位置 {@code offset} 处的 {@code haystack} 以 {@code prefix} 开头，则 {@literal true}
     * @see #startsWith(byte[], byte[], int)
     */
    public static boolean startsWith(byte[] haystack, byte[] prefix) {
        return startsWith(haystack, prefix, 0);
    }

    /**
     * 测试以指定的 {@code offset} 开始的 {@code haystack} 是否以给定的 {@code prefix} 开始
     *
     * @param haystack 要扫描的源
     * @param prefix   要查找的前缀
     * @param offset   开始的偏移量
     * @return 如果位置 {@code offset} 处的 {@code haystack} 以 {@code prefix} 开头，则 {@literal true}
     */
    public static boolean startsWith(byte[] haystack, byte[] prefix, int offset) {
        int to = offset;
        int prefixOffset = 0;
        int prefixLength = prefix.length;
        if ((offset < 0) || (offset > haystack.length - prefixLength)) {
            return false;
        }
        while (--prefixLength >= 0) {
            if (haystack[to++] != prefix[prefixOffset++]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 在指定的字节数组中搜索指定的值。返回 {@code haystack} 的自然顺序中第一个匹配值的索引，或者找不到 {@code needle} 的 {@code -1}
     *
     * @param haystack 要扫描的源
     * @param needle   要扫描的值
     * @return 第一次出现的索引，如果找不到，则为-1
     */
    public static int indexOf(byte[] haystack, byte needle) {
        for (int i = 0; i < haystack.length; i++) {
            if (haystack[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 使用 {@link java.nio.charset.StandardCharsets#UTF_8} 将 {@link String} 转换为 {@link ByteBuffer}
     *
     * @param theString 不能是 {@literal null}
     */
    public static ByteBuffer getByteBuffer(String theString) {
        return getByteBuffer(theString, StandardCharsets.UTF_8);
    }

    /**
     * 使用给定的 {@link Charset} 将 {@link String} 转换为 {@link ByteBuffer}
     *
     * @param theString 不能是 {@literal null}
     * @param charset   不能是 {@literal null}
     */
    public static ByteBuffer getByteBuffer(String theString, Charset charset) {
        Assert.notNull(theString, "The String must not be null!");
        Assert.notNull(charset, "The String must not be null!");
        return charset.encode(theString);
    }

    /**
     * 通过复制缓冲区并获取其内容，将给定 {@link ByteBuffer} 中的字节传输到数组中
     *
     * @param buffer 不能是 {@literal null}
     * @return 提取的字节
     */
    public static byte[] extractBytes(ByteBuffer buffer) {
        ByteBuffer duplicate = buffer.duplicate();
        byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return bytes;
    }
}
