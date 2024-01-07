package org.clever.core.flow;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 17:29 <br/>
 */
@Slf4j
public class WorkerFlowTest {
    @SneakyThrows
    @Test
    public void t01() {
        CompletableFuture.runAsync(() -> {
            log.info("### 1.1");
            log.info("### 1.2");
        }).thenRun(() -> {
            log.info("### 2.1");
            log.info("### 2.2");
        });

        Thread.sleep(3_000);
    }
}
