package org.clever.js.api.internal;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.clever.core.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/25 21:50 <br/>
 */
public class OutputStreamConsole extends AbstractConsole implements Closeable {
    public static final OutputStreamConsole INSTANCE = new OutputStreamConsole(System.out, System.err);

    /**
     * 读写文件使用的编码格式
     */
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    /**
     * 换行符
     */
    public static final String LINE_BREAK = "\r\n";
    /**
     * 输出流
     */
    private final OutputStream out;
    /**
     * 错误输出流
     */
    private final OutputStream err;

    /**
     * @param out 输出流
     * @param err 错误输出流
     */
    public OutputStreamConsole(OutputStream out, OutputStream err) {
        Assert.notNull(out, "参数 out 不能为 null");
        Assert.notNull(err, "参数 err 不能为 null");
        this.out = out;
        this.err = err;
    }

    @Override
    protected boolean isTraceEnabled() {
        return true;
    }

    @Override
    protected boolean isDebugEnabled() {
        return true;
    }

    @Override
    protected boolean isInfoEnabled() {
        return true;
    }

    @Override
    protected boolean isWarnEnabled() {
        return true;
    }

    @Override
    protected boolean isErrorEnabled() {
        return true;
    }

    @Override
    protected void doLog(String logsText, Object[] args) {
        println(out, logsText, args);
    }

    @Override
    protected void doTrace(String logsText, Object[] args) {
        println(out, logsText, args);
    }

    @Override
    protected void doDebug(String logsText, Object[] args) {
        println(out, logsText, args);
    }

    @Override
    protected void doInfo(String logsText, Object[] args) {
        println(out, logsText, args);
    }

    @Override
    protected void doWarn(String logsText, Object[] args) {
        println(err, logsText, args);
    }

    @Override
    protected void doError(String logsText, Object[] args) {
        println(err, logsText, args);
    }

    @Override
    protected void doPrint(String logsText, Object[] args) {
        println(out, logsText, args);
    }

    @Override
    public void close() throws IOException {
        out.close();
        err.close();
    }

    @SneakyThrows
    protected void println(OutputStream stream, String logsText, Object[] args) {
        if (stream instanceof PrintStream) {
            PrintStream printStream = (PrintStream) stream;
            printStream.println(logsText);
        } else {
            IOUtils.write(logsText, stream, CHARSET);
            IOUtils.write(LINE_BREAK, stream, CHARSET);
            stream.flush();
        }
    }
}
