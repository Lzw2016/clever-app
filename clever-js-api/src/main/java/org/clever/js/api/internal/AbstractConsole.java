package org.clever.js.api.internal;

import lombok.Getter;
import lombok.Setter;
import org.clever.core.Assert;
import org.clever.js.api.support.ObjectToString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/25 20:39 <br/>
 */
public abstract class AbstractConsole implements PrintOutput, Console {
    /**
     * 输出最大长度
     */
    public static final int MAX_LEN = 1024 * 8;
    /**
     * 日志溢出时的后缀
     */
    public static final String OVERFLOW_SUFFIX = "...";

    public static final String DEFAULT_LABEL = "default";

    public static final Map<String, AtomicLong> LABEL_COUNT_MAP = new ConcurrentHashMap<>(8);
    public static final Map<String, Long> LABEL_TIME_MAP = new ConcurrentHashMap<>(8);

    /**
     * 是否启用溢出处理(默认启用)
     */
    @Setter
    @Getter
    protected boolean overflowEnable = false;

    /**
     * toString实现
     */
    @Getter
    protected ObjectToString objectToString = ObjectToString.INSTANCE;

    public void setObjectToString(ObjectToString objectToString) {
        Assert.notNull(objectToString, "参数 objectToString 不能为 null");
        this.objectToString = objectToString;
    }

    @Override
    public void count() {
        count(null);
    }

    @Override
    public void count(String label) {
        if (label == null) {
            label = DEFAULT_LABEL;
        }
        AtomicLong count = LABEL_COUNT_MAP.computeIfAbsent(label, s -> new AtomicLong(0));
        long sum = count.incrementAndGet();
        String log = label + ": " + sum;
        info(log);
    }

    @Override
    public void countReset() {
        countReset(null);
    }

    @Override
    public void countReset(String label) {
        if (label == null) {
            label = DEFAULT_LABEL;
        }
        LABEL_COUNT_MAP.remove(label);
    }

    @Override
    public void time() {
        time(null);
    }

    @Override
    public void time(String label) {
        if (label == null) {
            label = DEFAULT_LABEL;
        }
        LABEL_TIME_MAP.computeIfAbsent(label, s -> System.currentTimeMillis());
    }

    @Override
    public void timeLog(String label, Object... args) {
        if (label == null) {
            label = DEFAULT_LABEL;
        }
        Long startTime = LABEL_TIME_MAP.get(label);
        if (startTime == null) {
            warn("No such label '" + label + "' for console.timeEnd()");
            return;
        }
        Long endTime = System.currentTimeMillis();
        String log = label + ": " + (endTime - startTime) + "ms ";
        int length = args == null ? 0 : args.length;
        Object[] array = new Object[length + 1];
        array[0] = log;
        if (args != null) {
            System.arraycopy(args, 0, array, 1, args.length);
        }
        info(array);
    }

    @Override
    public void timeEnd() {
        timeEnd(null);
    }

    @Override
    public void timeEnd(String label) {
        if (label == null) {
            label = DEFAULT_LABEL;
        }
        timeLog(label);
        LABEL_TIME_MAP.remove(label);
    }

    @Override
    public void log(Object... args) {
        if (!isDebugEnabled()) {
            return;
        }
        String logsText = logString(args);
        doLog(logsText, args);
    }

    @Override
    public void trace(Object... args) {
        if (!isTraceEnabled()) {
            return;
        }
        String logsText = logString(args);
        doTrace(logsText, args);
    }

    @Override
    public void debug(Object... args) {
        if (!isDebugEnabled()) {
            return;
        }
        String logsText = logString(args);
        doDebug(logsText, args);
    }

    @Override
    public void info(Object... args) {
        if (!isInfoEnabled()) {
            return;
        }
        String logsText = logString(args);
        doInfo(logsText, args);
    }

    @Override
    public void warn(Object... args) {
        if (!isWarnEnabled()) {
            return;
        }
        String logsText = logString(args);
        doWarn(logsText, args);
    }

    @Override
    public void error(Object... args) {
        if (!isErrorEnabled()) {
            return;
        }
        String logsText = logString(args);
        doError(logsText, args);
    }

    @Override
    public void print(Object... args) {
        String logsText = logString(args);
        // if (args != null && args.length > 0 && args[0] instanceof String) {
        //     String format = (String) args[0];
        //     int length = args.length - 1;
        //     Object[] array = new Object[length];
        //     System.arraycopy(args, 1, array, 0, length);
        //     logsText = String.format(format, array);
        // } else {
        //     logsText = logString(args);
        // }
        doPrint(logsText, args);
    }

    /**
     * 根据日志输出参数得到日志字符串
     */
    protected String logString(Object... args) {
        if (args == null || args.length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(args.length * 32);
        for (Object arg : args) {
            String str = objectToString.toString(arg);
            sb.append(str);
            if (overflowEnable && overflow(sb)) {
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 字符串溢出处理
     */
    protected boolean overflow(StringBuilder sb) {
        final int length = sb.length();
        boolean overflow = false;
        if (MAX_LEN < length) {
            int end = MAX_LEN - OVERFLOW_SUFFIX.length();
            sb.delete(end, length - end).append(OVERFLOW_SUFFIX);
            overflow = true;
        }
        return overflow;
    }

    /**
     * 是否启用Trace日志
     */
    protected abstract boolean isTraceEnabled();

    /**
     * 是否启用Debug日志
     */
    protected abstract boolean isDebugEnabled();

    /**
     * 是否启用Info日志
     */
    protected abstract boolean isInfoEnabled();

    /**
     * 是否启用Warn日志
     */
    protected abstract boolean isWarnEnabled();

    /**
     * 是否启用Error日志
     */
    protected abstract boolean isErrorEnabled();

    /**
     * @param logsText 已处理的输出字符串
     * @param args     原始参数
     */
    protected abstract void doLog(String logsText, Object[] args);

    /**
     * @param logsText 已处理的输出字符串
     * @param args     原始参数
     */
    protected abstract void doTrace(String logsText, Object[] args);

    /**
     * @param logsText 已处理的输出字符串
     * @param args     原始参数
     */
    protected abstract void doDebug(String logsText, Object[] args);

    /**
     * @param logsText 已处理的输出字符串
     * @param args     原始参数
     */
    protected abstract void doInfo(String logsText, Object[] args);

    /**
     * @param logsText 已处理的输出字符串
     * @param args     原始参数
     */
    protected abstract void doWarn(String logsText, Object[] args);

    /**
     * @param logsText 已处理的输出字符串
     * @param args     原始参数
     */
    protected abstract void doError(String logsText, Object[] args);

    /**
     * @param logsText 已处理的输出字符串
     * @param args     原始参数
     */
    protected abstract void doPrint(String logsText, Object[] args);
}
