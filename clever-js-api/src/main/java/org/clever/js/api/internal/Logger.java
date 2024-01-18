package org.clever.js.api.internal;

import lombok.Getter;
import org.clever.core.tuples.TupleTwo;
import org.clever.js.api.support.ObjectToString;
import org.clever.util.Assert;
import org.slf4j.LoggerFactory;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/26 11:26 <br/>
 */
public class Logger {
    /**
     * 日志记录器
     */
    protected final org.slf4j.Logger logger;

    /**
     * toString实现
     */
    @Getter
    protected ObjectToString objectToString = ObjectToString.Instance;

    public Logger(String name) {
        logger = LoggerFactory.getLogger(name);
    }

    public void setObjectToString(ObjectToString objectToString) {
        Assert.notNull(objectToString, "参数objectToString不能为空");
        this.objectToString = objectToString;
    }

    public void trace(String msg) {
        Object[] args = new Object[]{};
        trace(msg, args);
    }

    /**
     * 打印输出
     *
     * @param args 输出数据
     */
    public void trace(String format, Object... args) {
        if (logger.isTraceEnabled()) {
            if (args == null) {
                args = new Object[]{null};
            }
            TupleTwo<String, Throwable> tuple = logString(format, args);
            if (tuple.getValue2() == null) {
                logger.trace(tuple.getValue1());
            } else {
                logger.trace(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    public void debug(String msg) {
        Object[] args = new Object[]{};
        debug(msg, args);
    }

    /**
     * debug打印输出
     *
     * @param args 输出数据
     */
    public void debug(String format, Object... args) {
        if (logger.isDebugEnabled()) {
            if (args == null) {
                args = new Object[]{null};
            }
            TupleTwo<String, Throwable> tuple = logString(format, args);
            if (tuple.getValue2() == null) {
                logger.debug(tuple.getValue1());
            } else {
                logger.debug(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    public void info(String msg) {
        Object[] args = new Object[]{};
        info(msg, args);
    }

    /**
     * info打印输出
     *
     * @param args 输出数据
     */
    public void info(String format, Object... args) {
        if (logger.isInfoEnabled()) {
            if (args == null) {
                args = new Object[]{null};
            }
            TupleTwo<String, Throwable> tuple = logString(format, args);
            if (tuple.getValue2() == null) {
                logger.info(tuple.getValue1());
            } else {
                logger.info(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    public void warn(String msg) {
        Object[] args = new Object[]{};
        warn(msg, args);
    }

    /**
     * warn打印输出
     *
     * @param args 输出数据
     */
    public void warn(String format, Object... args) {
        if (logger.isWarnEnabled()) {
            if (args == null) {
                args = new Object[]{null};
            }
            TupleTwo<String, Throwable> tuple = logString(format, args);
            if (tuple.getValue2() == null) {
                logger.warn(tuple.getValue1());
            } else {
                logger.warn(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    public void error(String msg) {
        Object[] args = new Object[]{};
        error(msg, args);
    }

    /**
     * error打印输出
     *
     * @param args 输出数据
     */
    public void error(String format, Object... args) {
        if (logger.isErrorEnabled()) {
            if (args == null) {
                args = new Object[]{null};
            }
            TupleTwo<String, Throwable> tuple = logString(format, args);
            if (tuple.getValue2() == null) {
                logger.error(tuple.getValue1());
            } else {
                logger.error(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    /**
     * 根据日志输出参数得到日志字符串
     */
    protected TupleTwo<String, Throwable> logString(String format, Object... args) {
        if (args == null || args.length <= 0) {
            return TupleTwo.creat(format, null);
        }
        Throwable throwable = null;
        if (args[args.length - 1] instanceof Throwable) {
            throwable = (Throwable) args[args.length - 1];
        }
        String logsText;
        if (throwable == null) {
            logsText = objectToString.format(format, args);
        } else {
            int length = args.length - 1;
            Object[] array = new Object[length];
            System.arraycopy(args, 0, array, 0, length);
            logsText = objectToString.format(format, array);
        }
        return TupleTwo.creat(logsText, throwable);
    }
}
