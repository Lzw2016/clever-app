package org.clever.data.jdbc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.core.tuples.TupleOne;
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

    private Jdbc newJdbc() {
        return BaseTest.newMysql();
    }

    @Test
    public void t01() {
        final String idName = "t01";
        Jdbc jdbc = newJdbc();
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
        // 等待结束
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
        final String idName = "t02";
        Jdbc jdbc = newJdbc();
        final long startTime = System.currentTimeMillis();
        final int count = 1000;
        for (int i = 0; i < count; i++) {
            Long id = jdbc.nextId(idName);
            log.info("-> {}", id);
        }
        final long endTime = System.currentTimeMillis();
        // 2ms/次 | 总时间:2340ms
        log.info("{}ms/次 | 总时间:{}ms", (endTime - startTime) / count, (endTime - startTime));
        jdbc.close();
    }

    @Test
    public void t03() {
        final String idName = "t03";
        Jdbc jdbc = newJdbc();
        List<Long> ids = jdbc.nextIds(idName, 100);
        log.info("-> {}", ids);
        jdbc.close();
    }

    @SneakyThrows
    @Test
    public void t04() {
        // t04 EXT${yyyyMMdd}_${seq3} yyyy-MM-dd HH:mm:ss
        final String codeName = "t04";
        Jdbc jdbc = newJdbc();
        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);
            String code = jdbc.nextCode(codeName);
            log.info("-> {}", code);
        }
        jdbc.close();
    }

    @Test
    public void t05() {
        // t05 EXT${yyMMdd}_${seq8} yyyy-MM-dd
        final String codeName = "t05";
        Jdbc jdbc = newJdbc();
        final int count = 100;
        List<Future<?>> futures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Future<?> future = EXECUTOR.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    jdbc.nextCode(codeName);
                }
            });
            futures.add(future);
        }
        // 等待结束
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception ignored) {
            }
        }
        // current -> EXT221210_00010000 EXT221210_00020000 EXT221210_00030000
        log.info("current -> {}", jdbc.currentCode(codeName));
        jdbc.close();
    }

    @Test
    public void t06() {
        // t06 EXT${yyMMdd}_${seq8} yyyy-MM-dd
        final String codeName = "t06";
        Jdbc jdbc = newJdbc();
        final long startTime = System.currentTimeMillis();
        final int count = 1000;
        for (int i = 0; i < count; i++) {
            String code = jdbc.nextCode(codeName);
            log.info("-> {}", code);
        }
        final long endTime = System.currentTimeMillis();
        // 2ms/次 | 总时间:2046ms
        log.info("{}ms/次 | 总时间:{}ms", (endTime - startTime) / count, (endTime - startTime));
        jdbc.close();
    }

    @Test
    public void t07() {
        final String lockName = "t07";
        TupleOne<Integer> sum = new TupleOne<>(0);
        Jdbc jdbc = newJdbc();
        final int count = 100;
        List<Future<?>> futures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Future<?> future = EXECUTOR.submit(() -> {
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
            } catch (Exception ignored) {
            }
        }
        // sum -> 10000
        log.info("sum -> {}", sum.getValue1());
        jdbc.close();
    }
}
