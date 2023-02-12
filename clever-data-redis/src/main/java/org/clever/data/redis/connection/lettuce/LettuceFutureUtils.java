package org.clever.data.redis.connection.lettuce;

import org.clever.util.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 与 {@link CompletableFuture} 和 {@link CompletionStage} 交互的实用程序方法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:53 <br/>
 */
class LettuceFutureUtils {
    /**
     * 创建一个 {@link CompletableFuture}，该 {@link CompletableFuture#exceptionally(Function) exceptionally} 给定 {@link Throwable} 完成。
     * 此实用程序方法允许通过单个调用进行例外的未来创建。
     *
     * @param throwable 不得为 {@literal null}
     * @return 完成的 {@link CompletableFuture future}
     */
    static <T> CompletableFuture<T> failed(Throwable throwable) {
        Assert.notNull(throwable, "Throwable must not be null!");
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    /**
     * 通过 {@link CompletableFuture} 同步 {@link CompletableFuture#join() 结果，直到未来完成}。
     * 此方法保留 {@link RuntimeException}，这些 { 运行时异常} 可能会因将来完成而引发。
     * 选中的异常被封装在 {@link java.util.concurrent.CompletionException} 中。
     *
     * @param future 不得为 {@literal null}
     * @return 如果正常完成，则为将来的结果
     * @throws RuntimeException    如果将来使用 {@link RuntimeException} 完成，则抛出
     * @throws CompletionException 如果未来完成并带有检查异常，则抛出
     */
    static <T> T join(CompletionStage<T> future) throws RuntimeException, CompletionException {
        Assert.notNull(future, "CompletableFuture must not be null!");
        try {
            return future.toCompletableFuture().join();
        } catch (Exception e) {
            Throwable exceptionToUse = e;
            if (e instanceof CompletionException) {
                exceptionToUse = new LettuceExceptionConverter().convert((Exception) e.getCause());
                if (exceptionToUse == null) {
                    exceptionToUse = e.getCause();
                }
            }
            if (exceptionToUse instanceof RuntimeException) {
                throw (RuntimeException) exceptionToUse;
            }
            throw new CompletionException(exceptionToUse);
        }
    }

    /**
     * 返回一个 {@link Function}，该函数通过恢复到 {@code null} 来忽略 {@link CompletionStage#exceptionally(Function) exceptional completion}
     * 这允许使用以前失败的 {@link CompletionStage} 进行，而不考虑实际的 success/exception 状态。
     */
    static <T> Function<Throwable, T> ignoreErrors() {
        return ignored -> null;
    }
}
