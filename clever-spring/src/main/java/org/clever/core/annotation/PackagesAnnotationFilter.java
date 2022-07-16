package org.clever.core.annotation;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.util.Arrays;

/**
 * 根据包路径过滤注解的注解过滤器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:09 <br/>
 */
final class PackagesAnnotationFilter implements AnnotationFilter {
    private final String[] prefixes;
    private final int hashCode;

    /**
     * 能匹配的包前缀
     */
    PackagesAnnotationFilter(String... packages) {
        Assert.notNull(packages, "Packages array must not be null");
        this.prefixes = new String[packages.length];
        for (int i = 0; i < packages.length; i++) {
            String pkg = packages[i];
            Assert.hasText(pkg, "Packages array must not have empty elements");
            this.prefixes[i] = pkg + ".";
        }
        Arrays.sort(this.prefixes);
        this.hashCode = Arrays.hashCode(this.prefixes);
    }

    @Override
    public boolean matches(String annotationType) {
        for (String prefix : this.prefixes) {
            if (annotationType.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Arrays.equals(this.prefixes, ((PackagesAnnotationFilter) other).prefixes);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return "Packages annotation filter: " + StringUtils.arrayToCommaDelimitedString(this.prefixes);
    }
}
