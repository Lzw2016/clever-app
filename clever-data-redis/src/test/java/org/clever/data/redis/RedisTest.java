package org.clever.data.redis;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.SystemClock;
import org.clever.data.redis.config.RedisProperties;
import org.clever.data.redis.connection.stream.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 22:43 <br/>
 */
@Slf4j
public class RedisTest {
    public static RedisProperties getProperties() {
        // 配置
        RedisProperties properties = new RedisProperties();
        properties.setMode(RedisProperties.Mode.Standalone);
        properties.setClientName("test");
        properties.getStandalone().setHost("192.168.1.201");
        properties.getStandalone().setPort(30007);
        properties.getStandalone().setDatabase(0);
        properties.getStandalone().setPassword("admin123456");
        properties.getPool().setMaxActive(30000);
        return properties;
    }

    @Test
    public void t01() {
        RedisProperties properties = getProperties();
        // data
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("data_01", (byte) 1);
        data.put("data_02", (short) 2);
        data.put("data_03", 3);
        data.put("data_04", 4.1F);
        data.put("data_05", 5L);
        data.put("data_06", 6.1D);
        data.put("data_07", "abc");
        data.put("data_08", false);
        data.put("data_09", new Date());
        long timeout = 600_000;
        // Redis
        Redis redis = new Redis("test", properties);
        // Value 操作
        log.info("kHasKey -> {}", redis.kHasKey("test_01"));
        redis.vSet("test_01", "abc");
        redis.kExpire("test_01", timeout);
        log.info("vGet -> {}", redis.vGet("test_01", String.class));
        redis.vSet("test_02", data, timeout);
        log.info("vGet -> {}", redis.vGet("test_02"));
        // List 操作
        redis.lLeftPushAll("test_03", data.values());
        log.info("lLeftPop -> {}", redis.lLeftPop("test_03"));
        // Hash 操作
        redis.hPutAll("test_04", data);
        log.info("lLeftPop -> {}", redis.hEntries("test_04"));
        // ZSet
        data.forEach((key, value) -> redis.zsAdd("test_05", value, SystemClock.now()));
        log.info("zsReverseRange -> {}", redis.zsReverseRangeWithScores("test_05", 0, 10));
        redis.close();
    }

    @SuppressWarnings("BusyWait")
    @Test
    public void t02() throws InterruptedException {
        RedisProperties properties = getProperties();
        Redis redis = new Redis("test", properties);
        final String streamKey = "stream_001";
        final int count = 100;
        final AtomicInteger countAtomic = new AtomicInteger(0);
        List<Thread> list = new ArrayList<>();
        Thread thread = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                if (countAtomic.get() >= count) {
                    break;
                }
                ObjectRecord<String, String> record = StreamRecords.newRecord()
                        .in(streamKey)
                        .ofObject(String.format("abc_%s", i))
                        .withId(RecordId.autoGenerate());
                RecordId recordId = redis.getRedisTemplate().opsForStream().add(record);
                log.info("### -> recordId={}", recordId);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
        });
        list.add(thread);
        StreamReadOptions streamReadOptions = StreamReadOptions.empty()
                // 自动ACK
                // .autoAcknowledge()
                // 如果没有数据，则阻塞1s 阻塞时间需要小于`spring.redis.timeout`配置的时间
                .block(Duration.ofMillis(100))
                // 一直阻塞直到获取数据，可能会报超时异常
                // .block(Duration.ofMillis(0))
                // 1次获取10个数据
                .count(10);
        // Consumer consumer = Consumer.from("test", "test");
        // redis.getRedisTemplate().opsForStream().createGroup(streamKey, consumer.getGroup());
        thread = new Thread(() -> {
            RecordId readOffset = RecordId.of(0, 0);
            while (countAtomic.get() < count) {
                @SuppressWarnings("unchecked")
                List<ObjectRecord<String, String>> records = redis.getRedisTemplate()
                        .opsForStream()
                        .read(String.class, streamReadOptions, StreamOffset.create(streamKey, ReadOffset.from(readOffset)));
                if (records.isEmpty()) {
                    log.warn("没有获取到数据");
                }
                for (ObjectRecord<String, String> record : records) {
                    log.info("@@@ -> getId={} | getValue={}", record.getId(), record.getValue());
                    readOffset = record.getId();
                    countAtomic.incrementAndGet();
                    // redis.getRedisTemplate().opsForStream().acknowledge(streamKey, consumer.getGroup(), record.getId());
                }
            }
        });
        list.add(thread);
        list.forEach(Thread::start);
        // 等待停止
        while (countAtomic.get() < count) {
            Thread.sleep(100);
        }
        redis.kDelete(streamKey);
        redis.close();
    }

    @SuppressWarnings("BusyWait")
    @Test
    public void t03() throws InterruptedException {
        RedisProperties properties = getProperties();
        Redis redis = new Redis("test", properties);
        final String streamKey = "stream_002";
        final int count = 100;
        final AtomicInteger countAtomic = new AtomicInteger(0);
        List<Thread> list = new ArrayList<>();
        Thread thread = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                ObjectRecord<String, String> record = StreamRecords.newRecord()
                        .in(streamKey)
                        .ofObject(String.format("abc_%s", i))
                        .withId(RecordId.autoGenerate());
                RecordId recordId = redis.getRedisTemplate().opsForStream().add(record);
                log.info("### -> recordId={}", recordId);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
        });
        list.add(thread);

        list.forEach(Thread::start);
        // 等待停止
        while (countAtomic.get() < count) {
            Thread.sleep(100);
        }
        redis.close();
    }
}
