package org.clever.util.unit;

/**
 * 一组标准的{@link DataSize}单位
 *
 * <p>该类中使用的单位前缀是<a href="https://en.wikipedia.org/wiki/Binary_prefix">二进制前缀</a>，表示乘以2的幂。
 * 下表显示了此类中定义的枚举常数和相应的值。
 *
 * <p>
 * <table border="1">
 * <tr><th>常数</th><th>数据大小</th><th>Power&nbsp;of&nbsp;2</th><th>Size in Bytes</th></tr>
 * <tr><td>{@link #BYTES}</td><td>1B</td><td>2^0</td><td>1</td></tr>
 * <tr><td>{@link #KILOBYTES}</td><td>1KB</td><td>2^10</td><td>1,024</td></tr>
 * <tr><td>{@link #MEGABYTES}</td><td>1MB</td><td>2^20</td><td>1,048,576</td></tr>
 * <tr><td>{@link #GIGABYTES}</td><td>1GB</td><td>2^30</td><td>1,073,741,824</td></tr>
 * <tr><td>{@link #TERABYTES}</td><td>1TB</td><td>2^40</td><td>1,099,511,627,776</td></tr>
 * </table>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:51 <br/>
 *
 * @see DataSize
 */
public enum DataUnit {
    /**
     * B，由后缀表示 {@code B}.
     */
    BYTES("B", DataSize.ofBytes(1)),
    /**
     * KB，由后缀表示 {@code KB}.
     */
    KILOBYTES("KB", DataSize.ofKilobytes(1)),
    /**
     * MB，由后缀表示 {@code MB}.
     */
    MEGABYTES("MB", DataSize.ofMegabytes(1)),
    /**
     * GB，由后缀表示 {@code GB}.
     */
    GIGABYTES("GB", DataSize.ofGigabytes(1)),
    /**
     * TB，由后缀表示 {@code TB}.
     */
    TERABYTES("TB", DataSize.ofTerabytes(1));

    private final String suffix;
    private final DataSize size;

    DataUnit(String suffix, DataSize size) {
        this.suffix = suffix;
        this.size = size;
    }

    DataSize size() {
        return this.size;
    }

    /**
     * 返回与指定后缀匹配的 {@link DataUnit}。
     *
     * @param suffix 标准后缀之一
     * @return 与指定后缀匹配的 {@link DataUnit}
     * @throws IllegalArgumentException 如果后缀与此枚举的任何常量的后缀不匹配
     */
    public static DataUnit fromSuffix(String suffix) {
        for (DataUnit candidate : values()) {
            if (candidate.suffix.equals(suffix)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown data unit suffix '" + suffix + "'");
    }
}
