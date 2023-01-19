package org.clever.core.job;

/**
 * 全局任务处理，JVM内同一时刻只有一个线程执行，直接跳过多余调用
 * <p>
 * 作者： lzw<br/>
 * 创建时间：2018-11-10 19:03 <br/>
 */
public abstract class GlobalJob {
    private volatile boolean lock = false;

    /**
     * 全局执行的任务(JVM内同一时刻只有一个线程执行)
     */
    protected abstract void internalExecute() throws Exception;

    /**
     * 任务执行的异常处理
     */
    protected abstract void exceptionHandle(Exception e);

    /**
     * 任务处理内部逻辑
     *
     * @return 执行成功返回true
     */
    public boolean execute() {
        boolean success = false;
        if (lock) {
            return false;
        }
        try {
            lock = true;
            internalExecute();
            success = true;
        } catch (Exception e) {
            exceptionHandle(e);
        } finally {
            lock = false;
        }
        return success;
    }
}
