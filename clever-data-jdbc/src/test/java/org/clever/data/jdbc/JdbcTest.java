package org.clever.data.jdbc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/06 16:39 <br/>
 */
@Slf4j
public class JdbcTest {
    private Jdbc newJdbc() {
        // return BaseTest.newMysql();
        return BaseTest.newPostgresql();
    }

    @Test
    public void t01() {
        final String idName = "t01";
        Jdbc jdbc = newJdbc();
        final int count = 100;
        List<Future<?>> futures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Future<?> future = BaseTest.EXECUTOR.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        jdbc.nextId(idName);
                    }
                } catch (Exception e) {
                    log.info("", e);
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
            Future<?> future = BaseTest.EXECUTOR.submit(() -> {
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
    public void t09() {
        final String lockName = "t09";
        Jdbc jdbc = newJdbc();
        jdbc.beginTX(status -> {
            jdbc.getJdbcTemplate().getJdbcOperations().execute((ConnectionCallback<Void>) connection -> {
                log.info("connection -> {}", connection);
                jdbc.lock(lockName, () -> log.info("lock_1"));
                jdbc.lock(lockName, () -> log.info("lock_2"));
                return null;
            });
        });
        jdbc.close();
    }

    @Test
    public void t10() {
        Jdbc jdbc = newJdbc();
        final long startTime = System.currentTimeMillis();
        final int count = 10000;
        for (int i = 0; i < count; i++) {
            Long id = jdbc.queryLong("select 100");
            if (i % 500 == 0) {
                log.info("-> {}", id);
            }
        }
        final long endTime = System.currentTimeMillis();
        // 1ms/次 | 总时间:11443ms
        log.info("{}ms/次 | 总时间:{}ms", (endTime - startTime) / count, (endTime - startTime));
        jdbc.close();
    }

    @Test
    public void t11() {
        Jdbc jdbc = newJdbc();
        int c = jdbc.startBatch()
            .update("update test set a=:p1 where id=1", new HashMap<>() {{
                put("p1", "1");
            }})
            .update("update test set a=:p1 where id=1", new HashMap<>() {{
                put("p1", "2");
            }})
            .update("update test set a=:p1 where id=2", new HashMap<>() {{
                put("p1", "3");
            }})
            .update("update test set a=:p1 where id=1", new HashMap<>() {{
                put("p1", "4");
            }})
            .execute();
        log.info("-> {}", c);
        int[] cs = jdbc.batchUpdate("update test set a=:p1 where id=1", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<>() {{
                put("p1", "1");
            }});
            add(new HashMap<>() {{
                put("p1", "2");
            }});
            add(new HashMap<>() {{
                put("p1", "3");
            }});
            add(new HashMap<>() {{
                put("p1", "4");
            }});
        }});
        log.info("-> {}", cs);
        jdbc.close();
    }
}
