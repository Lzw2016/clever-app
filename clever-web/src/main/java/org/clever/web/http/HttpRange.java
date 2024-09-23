//package org.clever.web.http;
//
//import org.clever.core.io.InputStreamResource;
//import org.clever.core.io.Resource;
//import org.clever.core.io.support.ResourceRegion;
//import org.clever.util.Assert;
//import org.clever.util.CollectionUtils;
//import org.clever.util.ObjectUtils;
//import org.clever.util.StringUtils;
//
//import java.io.IOException;
//import java.util.*;
//
///**
// * 表示与 HTTP {@code "Range"} 标头一起使用的 HTTP（字节）范围
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2022/12/25 15:10 <br/>
// *
// * @see <a href="https://tools.ietf.org/html/rfc7233">HTTP/1.1: Range Requests</a>
// * @see HttpHeaders#setRange(List)
// * @see HttpHeaders#getRange()
// */
//public abstract class HttpRange {
//    /**
//     * 每个请求的最大范围
//     */
//    private static final int MAX_RANGES = 100;
//    private static final String BYTE_RANGE_PREFIX = "bytes=";
//
//    /**
//     * 使用当前 {@code HttpRange} 中包含的范围信息将 {@code Resource} 转换为 {@link ResourceRegion}
//     *
//     * @param resource {@code Resource} 从中选择区域
//     * @return 给定 {@code Resource} 的选定区域
//     */
//    public ResourceRegion toResourceRegion(Resource resource) {
//        // Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
//        // Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
//        Assert.isTrue(resource.getClass() != InputStreamResource.class, "Cannot convert an InputStreamResource to a ResourceRegion");
//        long contentLength = getLengthFor(resource);
//        long start = getRangeStart(contentLength);
//        long end = getRangeEnd(contentLength);
//        Assert.isTrue(start < contentLength, "'position' exceeds the resource length " + contentLength);
//        return new ResourceRegion(resource, start, end - start + 1);
//    }
//
//    /**
//     * 返回给定表示的总长度的范围起点
//     *
//     * @param length 表示的长度
//     * @return 表示的此范围的开始
//     */
//    public abstract long getRangeStart(long length);
//
//    /**
//     * 返回给定表示的总长度的范围末端（含）
//     *
//     * @param length 表示的长度
//     * @return 表示的范围结束
//     */
//    public abstract long getRangeEnd(long length);
//
//    /**
//     * 从给定位置到结尾创建一个 {@code HttpRange}
//     *
//     * @param firstBytePos 第一字节位置
//     * @return 从 {@code firstPos} 到结尾的字节范围
//     * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
//     */
//    public static HttpRange createByteRange(long firstBytePos) {
//        return new ByteRange(firstBytePos, null);
//    }
//
//    /**
//     * 从给定的第一个到最后一个位置创建一个 {@code Http Range}
//     *
//     * @param firstBytePos 第一字节位置
//     * @param lastBytePos  最后一个字节位置
//     * @return 从 {@code firstPos} 到 {@code lastPos} 的字节范围
//     * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
//     */
//    public static HttpRange createByteRange(long firstBytePos, long lastBytePos) {
//        return new ByteRange(firstBytePos, lastBytePos);
//    }
//
//    /**
//     * 创建一个范围超过最后给定字节数的 {@code HttpRange}。
//     *
//     * @param suffixLength 范围的字节数
//     * @return 一个字节范围，范围超过最后 {@code suffixLength} 个字节
//     * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
//     */
//    public static HttpRange createSuffixRange(long suffixLength) {
//        return new SuffixByteRange(suffixLength);
//    }
//
//    /**
//     * 将给定的逗号分隔字符串解析为 {@code HttpRange} 对象列表。
//     * <p>此方法可用于解析 {@code Range} 标头
//     *
//     * @param ranges 要分析的字符串
//     * @return 范围列表
//     * @throws IllegalArgumentException 如果无法解析字符串或范围数大于100
//     */
//    public static List<HttpRange> parseRanges(String ranges) {
//        if (!StringUtils.hasLength(ranges)) {
//            return Collections.emptyList();
//        }
//        if (!ranges.startsWith(BYTE_RANGE_PREFIX)) {
//            throw new IllegalArgumentException("Range '" + ranges + "' does not start with 'bytes='");
//        }
//        ranges = ranges.substring(BYTE_RANGE_PREFIX.length());
//        String[] tokens = StringUtils.tokenizeToStringArray(ranges, ",");
//        if (tokens.length > MAX_RANGES) {
//            throw new IllegalArgumentException("Too many ranges: " + tokens.length);
//        }
//        List<HttpRange> result = new ArrayList<>(tokens.length);
//        for (String token : tokens) {
//            result.add(parseRange(token));
//        }
//        return result;
//    }
//
//    private static HttpRange parseRange(String range) {
//        Assert.hasLength(range, "Range String must not be empty");
//        int dashIdx = range.indexOf('-');
//        if (dashIdx > 0) {
//            long firstPos = Long.parseLong(range.substring(0, dashIdx));
//            if (dashIdx < range.length() - 1) {
//                Long lastPos = Long.parseLong(range.substring(dashIdx + 1));
//                return new ByteRange(firstPos, lastPos);
//            } else {
//                return new ByteRange(firstPos, null);
//            }
//        } else if (dashIdx == 0) {
//            long suffixLength = Long.parseLong(range.substring(1));
//            return new SuffixByteRange(suffixLength);
//        } else {
//            throw new IllegalArgumentException("Range '" + range + "' does not contain \"-\"");
//        }
//    }
//
//    /**
//     * 将每个 {@code HttpRange} 转换为 {@code ResourceRegion}，使用 HTTP Range 信息选择给定 {@code Resource} 的适当段
//     *
//     * @param ranges   范围列表
//     * @param resource 从中选择区域的资源
//     * @return 给定资源的区域列表
//     * @throws IllegalArgumentException 如果所有范围的总和超过资源长度
//     */
//    public static List<ResourceRegion> toResourceRegions(List<HttpRange> ranges, Resource resource) {
//        if (CollectionUtils.isEmpty(ranges)) {
//            return Collections.emptyList();
//        }
//        List<ResourceRegion> regions = new ArrayList<>(ranges.size());
//        for (HttpRange range : ranges) {
//            regions.add(range.toResourceRegion(resource));
//        }
//        if (ranges.size() > 1) {
//            long length = getLengthFor(resource);
//            long total = 0;
//            for (ResourceRegion region : regions) {
//                total += region.getCount();
//            }
//            if (total >= length) {
//                throw new IllegalArgumentException("The sum of all ranges (" + total + ") should be less than the resource length (" + length + ")");
//            }
//        }
//        return regions;
//    }
//
//    private static long getLengthFor(Resource resource) {
//        try {
//            long contentLength = resource.contentLength();
//            Assert.isTrue(contentLength > 0, "Resource content length should be > 0");
//            return contentLength;
//        } catch (IOException ex) {
//            throw new IllegalArgumentException("Failed to obtain Resource content length", ex);
//        }
//    }
//
//    /**
//     * 返回给定的 {@code HttpRange} 对象列表的字符串表示形式
//     * <p>此方法可用于 {@code Range} 标头。
//     *
//     * @param ranges 创建字符串的范围
//     * @return 字符串表示
//     */
//    public static String toString(Collection<HttpRange> ranges) {
//        Assert.notEmpty(ranges, "Ranges Collection must not be empty");
//        StringJoiner builder = new StringJoiner(", ", BYTE_RANGE_PREFIX, "");
//        for (HttpRange range : ranges) {
//            builder.add(range.toString());
//        }
//        return builder.toString();
//    }
//
//    /**
//     * 表示 HTTP1.1 字节范围，具有第一个和可选的最后一个位置
//     *
//     * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
//     * @see HttpRange#createByteRange(long)
//     * @see HttpRange#createByteRange(long, long)
//     */
//    private static class ByteRange extends HttpRange {
//        private final long firstPos;
//        private final Long lastPos;
//
//        public ByteRange(long firstPos, Long lastPos) {
//            assertPositions(firstPos, lastPos);
//            this.firstPos = firstPos;
//            this.lastPos = lastPos;
//        }
//
//        private void assertPositions(long firstBytePos, Long lastBytePos) {
//            if (firstBytePos < 0) {
//                throw new IllegalArgumentException("Invalid first byte position: " + firstBytePos);
//            }
//            if (lastBytePos != null && lastBytePos < firstBytePos) {
//                throw new IllegalArgumentException("firstBytePosition=" + firstBytePos + " should be less then or equal to lastBytePosition=" + lastBytePos);
//            }
//        }
//
//        @Override
//        public long getRangeStart(long length) {
//            return this.firstPos;
//        }
//
//        @Override
//        public long getRangeEnd(long length) {
//            if (this.lastPos != null && this.lastPos < length) {
//                return this.lastPos;
//            } else {
//                return length - 1;
//            }
//        }
//
//        @Override
//        public boolean equals(Object other) {
//            if (this == other) {
//                return true;
//            }
//            if (!(other instanceof ByteRange)) {
//                return false;
//            }
//            ByteRange otherRange = (ByteRange) other;
//            return (this.firstPos == otherRange.firstPos && ObjectUtils.nullSafeEquals(this.lastPos, otherRange.lastPos));
//        }
//
//        @Override
//        public int hashCode() {
//            return (ObjectUtils.nullSafeHashCode(this.firstPos) * 31 + ObjectUtils.nullSafeHashCode(this.lastPos));
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder builder = new StringBuilder();
//            builder.append(this.firstPos);
//            builder.append('-');
//            if (this.lastPos != null) {
//                builder.append(this.lastPos);
//            }
//            return builder.toString();
//        }
//    }
//
//    /**
//     * 表示一个 HTTP1.1 后缀字节范围，带有多个后缀字节。
//     *
//     * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
//     * @see HttpRange#createSuffixRange(long)
//     */
//    private static class SuffixByteRange extends HttpRange {
//        private final long suffixLength;
//
//        public SuffixByteRange(long suffixLength) {
//            if (suffixLength < 0) {
//                throw new IllegalArgumentException("Invalid suffix length: " + suffixLength);
//            }
//            this.suffixLength = suffixLength;
//        }
//
//        @Override
//        public long getRangeStart(long length) {
//            if (this.suffixLength < length) {
//                return length - this.suffixLength;
//            } else {
//                return 0;
//            }
//        }
//
//        @Override
//        public long getRangeEnd(long length) {
//            return length - 1;
//        }
//
//        @Override
//        public boolean equals(Object other) {
//            if (this == other) {
//                return true;
//            }
//            if (!(other instanceof SuffixByteRange)) {
//                return false;
//            }
//            SuffixByteRange otherRange = (SuffixByteRange) other;
//            return (this.suffixLength == otherRange.suffixLength);
//        }
//
//        @Override
//        public int hashCode() {
//            return Long.hashCode(this.suffixLength);
//        }
//
//        @Override
//        public String toString() {
//            return "-" + this.suffixLength;
//        }
//    }
//}
