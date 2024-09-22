package org.clever.data.redis;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.SystemClock;
import org.clever.core.random.RandomUtil;
import org.clever.data.redis.config.RedisProperties;
import org.clever.data.redis.support.RedissonClientFactory;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/17 21:36 <br/>
 */
@Slf4j
public class RedissonTest {

    @SuppressWarnings("BusyWait")
    @SneakyThrows
    @Test
    public void t02() {
        RedisProperties properties = RedisTest.getProperties();
        Config config = RedissonClientFactory.createConfig(properties, null);
        RedissonClient redisson = Redisson.create(config);
        final int count = 5;
        AtomicInteger atomicInt = new AtomicInteger(0);
        // RLock rLock = redisson.getLock(name) | RLock.lock()
//        t_RLock_lock(redisson, count, atomicInt);
        // RLock rLock = redisson.getLock(name) | RLock.tryLock()
//        t_RLock_tryLock(redisson, count, atomicInt);
//        t_RLock_tryLock2(redisson, count, atomicInt);
        t_RLock_lock3(redisson, count, atomicInt);
        // 等待停止
        while (atomicInt.get() < count) {
            Thread.sleep(100);
        }
        redisson.shutdown();
        // 可重入互斥锁 - 非公平锁
        // redisson.getLock()
        // 公平锁 - 其它与 redisson.getLock() 一样
        // redisson.getFairLock()
        // 自旋锁 - 非公平锁,其它与 redisson.getLock() 一样
        // redisson.getSpinLock()
        // 读写锁 - 读锁共享,写锁互斥,读写互斥
        // redisson.getReadWriteLock()
        // 合并多个 RLock - 高可靠性
        // redisson.getMultiLock();
        // 分布式闩锁 - CountDownLatch
        // redisson.getCountDownLatch()
        // 分布式信号量
        // redisson.getSemaphore()
        // 分布式有效期的信号量
        // redisson.getPermitExpirableSemaphore()
        // 分布式对象
        // redisson.getAtomicLong()
        // redisson.getAtomicDouble()
        // redisson.getList()
        // redisson.getSet()
        // redisson.getMap()
        // redisson.getMapCache()
        // redisson.getQueue()
    }

    public void t_RLock_lock(RedissonClient redisson, int count, AtomicInteger atomicInt) {
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(() -> {
                RLock lock = redisson.getLock("lock_001");
                log.info("### -> RLock 创建成功");
                lock.lock();
                for (int j = 0; j < 10; j++) {
                    lock.lock();
                }
                log.info("### -> {}", atomicInt.get());
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    log.info(e.getMessage(), e);
                }
                for (int j = 0; j < 10; j++) {
                    lock.unlock();
                }
                lock.unlock();
                atomicInt.incrementAndGet();
            });
            list.add(thread);
        }
        list.forEach(Thread::start);
    }

    public void t_RLock_tryLock(RedissonClient redisson, int count, AtomicInteger atomicInt) {
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(() -> {
                RLock lock = redisson.getLock("lock_002");
                log.info("### -> RLock 创建成功");
                boolean flag = lock.tryLock();
                if (!flag) {
                    log.info("### -> RLock 获取锁失败");
                    atomicInt.incrementAndGet();
                    return;
                }
                for (int j = 0; j < 10; j++) {
                    flag = flag && lock.tryLock();
                }
                log.info("### -> {}", atomicInt.get());
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    log.info(e.getMessage(), e);
                }
                for (int j = 0; j < 10; j++) {
                    lock.unlock();
                }
                lock.unlock();
                atomicInt.incrementAndGet();
            });
            list.add(thread);
        }
        list.forEach(Thread::start);
    }

    public void t_RLock_tryLock2(RedissonClient redisson, int count, AtomicInteger atomicInt) {
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(() -> {
                RLock lock = redisson.getLock("lock_003");
                log.info("### -> RLock 创建成功");
                atomicInt.incrementAndGet();
                boolean flag;
                try {
                    long time = atomicInt.get() * 3_000L;
                    log.info("### -> time={}", time);
                    flag = lock.tryLock(time, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (!flag) {
                    log.info("### -> RLock 获取锁失败");
                    return;
                }
                atomicInt.decrementAndGet();
                for (int j = 0; j < 10; j++) {
                    flag = flag && lock.tryLock();
                }
                log.info("### -> {}", atomicInt.get());
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    log.info(e.getMessage(), e);
                }
                for (int j = 0; j < 10; j++) {
                    lock.unlock();
                }
                lock.unlock();
                atomicInt.incrementAndGet();
            });
            list.add(thread);
        }
        list.forEach(Thread::start);
    }

    public void t_RLock_lock3(RedissonClient redisson, int count, AtomicInteger atomicInt) {
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(() -> {
                RLock lock = redisson.getLock("lock_001");
                log.info("### -> RLock 创建成功");
                lock.lock(3_000, TimeUnit.MILLISECONDS);
                log.info("### -> {}", atomicInt.get());
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    log.info(e.getMessage(), e);
                }
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    log.warn("已自动释放Redis锁: {}", e.getMessage());
                }
                atomicInt.incrementAndGet();
            });
            list.add(thread);
        }
        list.forEach(Thread::start);
    }

    @SuppressWarnings("BusyWait")
    @SneakyThrows
    @Test
    public void t03() {
        RedisProperties properties = RedisTest.getProperties();
        Config config = RedissonClientFactory.createConfig(properties, null);
        RedissonClient redisson = Redisson.create(config);
        final int count = 10;
        AtomicInteger atomicInt = new AtomicInteger(0);
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("lock_004");
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int idx = i;
            Thread thread = new Thread(() -> {
                RLock lock = idx < 5 ? readWriteLock.writeLock() : readWriteLock.readLock();
                String type = idx < 5 ? "写锁" : "读锁";
                lock.lock();
                for (int j = 0; j < 10; j++) {
                    lock.lock();
                }
                log.info("### -> {} | type={}", atomicInt.get(), type);
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    log.info(e.getMessage(), e);
                }
                for (int j = 0; j < 10; j++) {
                    lock.unlock();
                }
                lock.unlock();
                log.info("### <- {} | type={}", atomicInt.get(), type);
                atomicInt.incrementAndGet();
            });
            list.add(thread);
        }
        list.forEach(Thread::start);
        // 等待停止
        while (atomicInt.get() < count) {
            Thread.sleep(100);
        }
        redisson.shutdown();
    }

    @SuppressWarnings("BusyWait")
    @SneakyThrows
    @Test
    public void t04() {
        RedisProperties properties = RedisTest.getProperties();
        Config config = RedissonClientFactory.createConfig(properties, null);
        RedissonClient redisson = Redisson.create(config);
        final int count = 30;
        AtomicInteger atomicInt = new AtomicInteger(0);
        RFencedLock fencedLock = redisson.getFencedLock("lock_004");
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(() -> {
                // token 会从0开始一直增长
                Long token = fencedLock.lockAndGetToken();
                log.info("### -> token={} | {}", token, fencedLock.getToken());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    log.info(e.getMessage(), e);
                }
                fencedLock.unlock();
                atomicInt.incrementAndGet();
            });
            list.add(thread);
        }
        list.forEach(Thread::start);
        // 等待停止
        while (atomicInt.get() < count) {
            Thread.sleep(100);
        }
        redisson.shutdown();
    }

    @SuppressWarnings("BusyWait")
    @SneakyThrows
    @Test
    public void t05() {
        RedisProperties properties = RedisTest.getProperties();
        Config config = RedissonClientFactory.createConfig(properties, null);
        RedissonClient redisson = Redisson.create(config);
        final int count = 30;
        AtomicInteger atomicInt = new AtomicInteger(0);
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int idx = i;
            Thread thread = new Thread(() -> {
                RCountDownLatch countDownLatch = redisson.getCountDownLatch("lock_005");
                if (idx == 0) {
                    boolean success = countDownLatch.trySetCount(count - 1);
                    if (!success) {
                        throw new RuntimeException("设置锁闩值失败");
                    }
                    try {
                        countDownLatch.await();
                        log.info("闩值归零!");
                    } catch (InterruptedException e) {
                        log.info("闩值归零,被中断");
                    }
                    atomicInt.incrementAndGet();
                    return;
                }
                try {
                    Thread.sleep(RandomUtil.randomLong(3, 30) * 1000);
                    countDownLatch.countDown();
                    log.info("### -> getCount={}", countDownLatch.getCount());
                } catch (InterruptedException e) {
                    log.info(e.getMessage(), e);
                }
                atomicInt.incrementAndGet();
            });
            list.add(thread);
        }
        list.forEach(Thread::start);
        // 等待停止
        while (atomicInt.get() < count) {
            Thread.sleep(100);
        }
        redisson.shutdown();
    }

    @SuppressWarnings("BusyWait")
    @SneakyThrows
    @Test
    public void t06() {
        RedisProperties properties = RedisTest.getProperties();
        Config config = RedissonClientFactory.createConfig(properties, null);
        RedissonClient redisson = Redisson.create(config);
        final int count = 30;
        AtomicInteger atomicInt = new AtomicInteger(0);
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int idx = i;
            Thread thread = new Thread(() -> {
                RSemaphore semaphore = redisson.getSemaphore("lock_006");
                if (idx == 0) {
                    if (!semaphore.isExists()) {
                        boolean success = semaphore.trySetPermits(count / 5 + 1);
                        if (!success) {
                            throw new RuntimeException("设置信号量失败");
                        }
                    }
                    atomicInt.incrementAndGet();
                    return;
                }
                try {
                    Thread.sleep(100);
                    semaphore.acquire();
                    log.info("### -> availablePermits={}", semaphore.availablePermits());
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    log.info(e.getMessage(), e);
                } finally {
                    // 如果没有调用 release 不会自动 release
                    semaphore.release();
                }
                atomicInt.incrementAndGet();
            });
            list.add(thread);
        }
        list.forEach(Thread::start);
        // 等待停止
        while (atomicInt.get() < count) {
            Thread.sleep(100);
        }
        redisson.shutdown();
    }

    @Test
    public void t07() {
        RedisProperties properties = RedisTest.getProperties();
        Config config = RedissonClientFactory.createConfig(properties, null);
        RedissonClient redisson = Redisson.create(config);
        final long count = 5_000;
        final long startTime = SystemClock.now();
        for (int i = 0; i < count; i++) {
            RLock lock = redisson.getLock(String.format("lock_%s", i));
            lock.lock();
            lock.unlock();
        }
        final long endTime = SystemClock.now();
        // 0.8112932013629726
        log.info("### --> {} ms/次", count * 1.0 / (endTime - startTime));
    }
}
