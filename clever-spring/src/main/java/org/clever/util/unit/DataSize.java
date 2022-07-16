package org.clever.util.unit;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据大小，例如 '12MB'.
 *
 * <p>该类以字节为单位对数据大小进行建模，并且是不可变的和线程安全的。
 *
 * <p>此类中使用的术语和单位基于<a href="https://en.wikipedia.org/wiki/Binary_prefix">二进制前缀</a>，表示2的幂乘法。
 * 有关详细信息，请参阅下表和{@link DataUnit}的Javadoc。
 *
 * <p>
 * <table border="1">
 * <tr><th>Term</th><th>Data Size</th><th>Size in Bytes</th></tr>
 * <tr><td>byte</td><td>1B</td><td>1</td></tr>
 * <tr><td>kilobyte</td><td>1KB</td><td>1,024</td></tr>
 * <tr><td>megabyte</td><td>1MB</td><td>1,048,576</td></tr>
 * <tr><td>gigabyte</td><td>1GB</td><td>1,073,741,824</td></tr>
 * <tr><td>terabyte</td><td>1TB</td><td>1,099,511,627,776</td></tr>
 * </table>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:52 <br/>
 *
 * @see DataUnit
 */
public final class DataSize implements Comparable<DataSize>, Serializable {
    /**
     * KB节字节数。
     */
    private static final long BYTES_PER_KB = 1024;
    /**
     * MB节字节数。
     */
    private static final long BYTES_PER_MB = BYTES_PER_KB * 1024;
    /**
     * GB节字节数。
     */
    private static final long BYTES_PER_GB = BYTES_PER_MB * 1024;
    /**
     * TB字节数。
     */
    private static final long BYTES_PER_TB = BYTES_PER_GB * 1024;

    private final long bytes;

    private DataSize(long bytes) {
        this.bytes = bytes;
    }

    /**
     * B
     */
    public static DataSize ofBytes(long bytes) {
        return new DataSize(bytes);
    }

    /**
     * KB
     */
    public static DataSize ofKilobytes(long kilobytes) {
        return new DataSize(Math.multiplyExact(kilobytes, BYTES_PER_KB));
    }

    /**
     * MB
     */
    public static DataSize ofMegabytes(long megabytes) {
        return new DataSize(Math.multiplyExact(megabytes, BYTES_PER_MB));
    }

    /**
     * GB
     */
    public static DataSize ofGigabytes(long gigabytes) {
        return new DataSize(Math.multiplyExact(gigabytes, BYTES_PER_GB));
    }

    /**
     * TB
     */
    public static DataSize ofTerabytes(long terabytes) {
        return new DataSize(Math.multiplyExact(terabytes, BYTES_PER_TB));
    }

    public static DataSize of(long amount, DataUnit unit) {
        Assert.notNull(unit, "Unit must not be null");
        return new DataSize(Math.multiplyExact(amount, unit.size().toBytes()));
    }

    /**
     * 示例：
     * <pre>
     * "12KB" -- 解析为 "12 kilobytes"
     * "5MB"  -- 解析为 "5 megabytes"
     * "20"   -- 解析为 "20 bytes"
     * </pre>
     *
     * @param text 要分析的文本
     * @return 已解析的 {@link DataSize}
     * @see #parse(CharSequence, DataUnit)
     */
    public static DataSize parse(CharSequence text) {
        return parse(text, null);
    }

    /**
     * 示例：
     * <pre>
     * "12KB" -- 解析为 "12 kilobytes"
     * "5MB"  -- 解析为 "5 megabytes"
     * "20"   -- 解析为 "20 kilobytes" (其中{@code defaultUnit}为{@link DataUnit#KILOBYTES})
     * </pre>
     *
     * @param text the text to parse
     * @return the parsed {@link DataSize}
     */
    public static DataSize parse(CharSequence text, DataUnit defaultUnit) {
        Assert.notNull(text, "Text must not be null");
        try {
            Matcher matcher = DataSizeUtils.PATTERN.matcher(text);
            Assert.state(matcher.matches(), "Does not match data size pattern");
            DataUnit unit = DataSizeUtils.determineDataUnit(matcher.group(2), defaultUnit);
            long amount = Long.parseLong(matcher.group(1));
            return DataSize.of(amount, unit);
        } catch (Exception ex) {
            throw new IllegalArgumentException("'" + text + "' is not a valid data size", ex);
        }
    }

    /**
     * 检查此大小是否为负，不包括零。
     *
     * @return 如果此大小的大小小于零字节，则为true
     */
    public boolean isNegative() {
        return this.bytes < 0;
    }

    /**
     * 返回此实例中的字节数。
     *
     * @return 字节数
     */
    public long toBytes() {
        return this.bytes;
    }

    /**
     * 返回此实例中的KB数。
     *
     * @return 千字节数
     */
    public long toKilobytes() {
        return this.bytes / BYTES_PER_KB;
    }

    /**
     * 返回此实例中的兆字节数。
     *
     * @return 兆字节数
     */
    public long toMegabytes() {
        return this.bytes / BYTES_PER_MB;
    }

    /**
     * 返回此实例中的GB数。
     *
     * @return GB数
     */
    public long toGigabytes() {
        return this.bytes / BYTES_PER_GB;
    }

    /**
     * 返回此实例中的TB数。
     *
     * @return TB数
     */
    public long toTerabytes() {
        return this.bytes / BYTES_PER_TB;
    }

    @Override
    public int compareTo(DataSize other) {
        return Long.compare(this.bytes, other.bytes);
    }

    @Override
    public String toString() {
        return String.format("%dB", this.bytes);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        DataSize otherSize = (DataSize) other;
        return (this.bytes == otherSize.bytes);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.bytes);
    }

    /**
     * 支持延迟加载的静态嵌套类 {@link #PATTERN}.
     */
    private static class DataSizeUtils {
        /**
         * 用于解析的模式。
         */
        private static final Pattern PATTERN = Pattern.compile("^([+\\-]?\\d+)([a-zA-Z]{0,2})$");

        private static DataUnit determineDataUnit(String suffix, DataUnit defaultUnit) {
            DataUnit defaultUnitToUse = (defaultUnit != null ? defaultUnit : DataUnit.BYTES);
            return (StringUtils.hasLength(suffix) ? DataUnit.fromSuffix(suffix) : defaultUnitToUse);
        }
    }
}
