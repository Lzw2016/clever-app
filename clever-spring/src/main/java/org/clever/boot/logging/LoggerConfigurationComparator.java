package org.clever.boot.logging;

import org.clever.util.Assert;

import java.util.Comparator;

/**
 * 用于比较 {@link LoggerConfiguration 日志配置} 的 {@link Comparator 比较器} 的实现。将“root”记录器排序为第一个记录器，然后按名称进行词汇排序。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:50 <br/>
 */
class LoggerConfigurationComparator implements Comparator<LoggerConfiguration> {
    private final String rootLoggerName;

    /**
     * 创建一个新的{@link LoggerConfigurationComparator}实例
     *
     * @param rootLoggerName “root”记录器的名称
     */
    LoggerConfigurationComparator(String rootLoggerName) {
        Assert.notNull(rootLoggerName, "RootLoggerName must not be null");
        this.rootLoggerName = rootLoggerName;
    }

    @Override
    public int compare(LoggerConfiguration o1, LoggerConfiguration o2) {
        if (this.rootLoggerName.equals(o1.getName())) {
            return -1;
        }
        if (this.rootLoggerName.equals(o2.getName())) {
            return 1;
        }
        return o1.getName().compareTo(o2.getName());
    }
}
