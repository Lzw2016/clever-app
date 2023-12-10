package org.clever.data.jdbc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.tuples.TupleOne;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/10 15:09 <br/>
 */
@Slf4j
public class JdbcLockTest {
    private Jdbc newJdbc() {
        //return BaseTest.newMysql();
        return BaseTest.newPostgresql();
    }

    @Test
    public void t01() {
        final String lockName = "abc1234567890";
        TupleOne<Integer> sum = new TupleOne<>(0);
        Jdbc jdbc = newJdbc();
        final long startTime = System.currentTimeMillis();
        final int count = 100;
        List<Future<?>> futures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Future<?> future = BaseTest.EXECUTOR.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    jdbc.lock(lockName, () -> {
                        sum.setValue1(sum.getValue1() + 1);
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ignored) {
                        }
                    });
                }
            });
            futures.add(future);
        }
        // 等待结束
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.info("异常", e);
            }
        }
        // sum -> 10000
        log.info("sum -> {} | 耗时: {}ms", sum.getValue1(), System.currentTimeMillis() - startTime);
        // mysql        耗时: 17604ms | 耗时: 16990ms
        // postgresql   耗时: 25071ms | 耗时: 24616ms
        jdbc.close();
    }

    @SneakyThrows
    @Test
    public void t02() {
        final String lockName = "abc1234567890";
        Jdbc jdbc = newJdbc();
        Thread thread = new Thread(() -> {
            jdbc.tryLock(lockName, -1, locked -> {
                log.info("### 1 locked={}", locked);
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ignored) {
                    Thread.yield();
                }
            });
        });
        thread.start();
        Thread.sleep(1_000);
        Thread thread2 = new Thread(() -> {
            jdbc.tryLock(lockName, 3, locked -> {
                log.info("### 2 locked={}", locked);
            });
        });
        thread2.start();
        thread.join();
        thread2.join();
        jdbc.close();
    }
}
