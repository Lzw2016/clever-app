package org.clever.data.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/06 16:39 <br/>
 */
@Slf4j
public class JdbcTest {
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            16, 16, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),
            new BasicThreadFactory.Builder()
                    .namingPattern("test-%d")
                    .daemon(true)
                    .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Test
    public void t01() {
        final String idName = "t01";
        Jdbc jdbc = BaseTest.newMysql();
        final int count = 100;
        List<Future<?>> futures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Future<?> future = EXECUTOR.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    jdbc.nextId(idName);
                }
            });
            futures.add(future);
        }
        // 等待reload结束
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception ignored) {
            }
        }
        // current -> 9999 19999 29999
        log.info("current -> {}", jdbc.currentId(idName));
        jdbc.close();
    }

    @Test
    public void t02() {
        Jdbc jdbc = BaseTest.newMysql();
        final long startTime = System.currentTimeMillis();
        final int count = 1000;
        for (int i = 0; i < count; i++) {
            Long id = jdbc.nextId("t02");
            log.info("-> {}", id);
        }
        final long endTime = System.currentTimeMillis();
        // 2ms/次 | 总时间:2340ms
        log.info("{}ms/次 | 总时间:{}ms", (endTime - startTime) / count, (endTime - startTime));
        jdbc.close();
    }
}
