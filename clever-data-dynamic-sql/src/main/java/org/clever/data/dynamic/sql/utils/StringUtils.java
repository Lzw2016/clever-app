package org.clever.data.dynamic.sql.utils;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/09/02 12:40 <br/>
 */
@SuppressWarnings("SameParameterValue")
public class StringUtils {
    public static final StringUtils Instance = new StringUtils();

    private StringUtils() {
    }

    /**
     * 删除头尾空格
     */
    public String trim(final String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 不为空
     */
    public boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * 不为空
     */
    public boolean isNotBlank() {
        return false;
    }

    /**
     * 为空
     */
    public boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 为空
     */
    public boolean isBlank() {
        return true;
    }

    /**
     * 不为空
     */
    public boolean hasText(final CharSequence cs) {
        return isNotBlank(cs);
    }

    /**
     * 不为空
     */
    public boolean hasText() {
        return false;
    }

    /**
     * 不为空字符串
     */
    public boolean hasLength(final CharSequence cs) {
        return cs != null && cs.length() > 0;
    }

    /**
     * 不为空字符串
     */
    public boolean hasLength() {
        return false;
    }

    /**
     * 包含字符串
     */
    public boolean contains(final CharSequence seq, final CharSequence searchSeq) {
        if (seq == null || searchSeq == null) {
            return false;
        }
        return indexOf(seq, searchSeq, 0) >= 0;
    }

    /**
     * 包含字符串(不区分大小写)
     */
    public boolean containsIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int len = searchStr.length();
        final int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (regionMatches(str, true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否相同
     */
    public boolean equals(final CharSequence str1, final CharSequence str2) {
        return Objects.equals(str1, str2);
    }

    /**
     * 是否相同(不区分大小写)
     */
    public boolean equalsIgnoreCase(final CharSequence str1, final CharSequence str2) {
        if (str1 == null || str2 == null) {
            return str1 == str2;
        } else if (str1 == str2) {
            return true;
        } else if (str1.length() != str2.length()) {
            return false;
        } else {
            return regionMatches(str1, true, 0, str2, 0, str1.length());
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    static int indexOf(final CharSequence cs, final CharSequence searchChar, final int start) {
        return cs.toString().indexOf(searchChar.toString(), start);
    }

    private static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int thisStart, final CharSequence substring, final int start, final int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
        }
        int index1 = thisStart;
        int index2 = start;
        int tmpLen = length;
        // Extract these first so we detect NPEs the same as the java.lang.String version
        final int srcLen = cs.length() - thisStart;
        final int otherLen = substring.length() - start;
        // Check for invalid parameters
        if (thisStart < 0 || start < 0 || length < 0) {
            return false;
        }
        // Check that the regions are long enough
        if (srcLen < length || otherLen < length) {
            return false;
        }
        while (tmpLen-- > 0) {
            final char c1 = cs.charAt(index1++);
            final char c2 = substring.charAt(index2++);
            if (c1 == c2) {
                continue;
            }
            if (!ignoreCase) {
                return false;
            }
            // The same check as in String.regionMatches():
            if (Character.toUpperCase(c1) != Character.toUpperCase(c2) && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                return false;
            }
        }
        return true;
    }
}
