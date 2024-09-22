package org.clever.core.watch;

import lombok.Getter;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 黑白名单文件过滤器
 * 作者：lizw <br/>
 * 创建时间：2020/08/30 11:26 <br/>
 */
public class BlackWhiteFileFilter implements FileFilter {
    private final WildcardFileFilter includeFilter;

    private final WildcardFileFilter excludeFilter;

    @Getter
    private final IOCase caseSensitivity;

    /**
     * @param include         包含的文件通配符(白名单)
     * @param exclude         排除的文件通配符(黑名单)
     * @param caseSensitivity 文件大小写敏感设置
     */
    public BlackWhiteFileFilter(String[] include, String[] exclude, IOCase caseSensitivity) {
        this.caseSensitivity = Objects.requireNonNullElse(caseSensitivity, IOCase.SYSTEM);
        include = trim(include);
        if (include == null || include.length == 0) {
            this.includeFilter = null;
        } else {
            this.includeFilter = WildcardFileFilter.builder().setWildcards(include).setIoCase(this.caseSensitivity).get();
        }
        exclude = trim(exclude);
        if (exclude == null || exclude.length == 0) {
            this.excludeFilter = null;
        } else {
            this.excludeFilter = WildcardFileFilter.builder().setWildcards(exclude).setIoCase(this.caseSensitivity).get();
        }
    }

    /**
     * @param include 包含的文件通配符(白名单)
     * @param exclude 排除的文件通配符(黑名单)
     */
    public BlackWhiteFileFilter(String[] include, String[] exclude) {
        this(include, exclude, null);
    }

    /**
     * @param includeSet      包含的文件通配符(白名单)
     * @param excludeSet      排除的文件通配符(黑名单)
     * @param caseSensitivity 文件大小写敏感设置
     */
    public BlackWhiteFileFilter(Set<String> includeSet, Set<String> excludeSet, IOCase caseSensitivity) {
        this.caseSensitivity = Objects.requireNonNullElse(caseSensitivity, IOCase.SYSTEM);
        String[] include = trim(includeSet);
        if (include == null || include.length == 0) {
            this.includeFilter = null;
        } else {
            this.includeFilter = WildcardFileFilter.builder().setWildcards(include).setIoCase(this.caseSensitivity).get();
        }
        String[] exclude = trim(excludeSet);
        if (exclude == null || exclude.length == 0) {
            this.excludeFilter = null;
        } else {
            this.excludeFilter = WildcardFileFilter.builder().setWildcards(exclude).setIoCase(this.caseSensitivity).get();
        }
    }

    /**
     * @param includeSet 包含的文件通配符(白名单)
     * @param excludeSet 排除的文件通配符(黑名单)
     */
    public BlackWhiteFileFilter(Set<String> includeSet, Set<String> excludeSet) {
        this(includeSet, excludeSet, null);
    }

    @Override
    public boolean accept(File pathname) {
        // 白名单存在，文件不在白名单里
        if (includeFilter != null && !includeFilter.accept(pathname)) {
            return false;
        }
        // 黑名单存在，文件在黑名单里
        // noinspection RedundantIfStatement
        if (excludeFilter != null && excludeFilter.accept(pathname)) {
            return false;
        }
        return true;
    }

    private String[] trim(String[] array) {
        if (array == null) {
            return null;
        }
        Set<String> set = new HashSet<>(array.length);
        for (String str : array) {
            str = StringUtils.trim(str);
            if (StringUtils.isNotBlank(str)) {
                set.add(str);
            }
        }
        return set.toArray(new String[0]);
    }

    private String[] trim(Set<String> set) {
        if (set == null) {
            return null;
        }
        return trim(set.toArray(new String[0]));
    }
}
