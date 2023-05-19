package org.clever.core.timer;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.PlatformOS;
import org.clever.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间轮算法实现的定时任务调度器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/05/18 15:03 <br/>
 */
@Slf4j
public class HashedWheelTimer implements Timer {
    /**
     * 创建当前类型实例的计数器
     */
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
    /**
     * 是否需要警告创建了太多实例
     */
    private static final AtomicBoolean WARNED_TOO_MANY_INSTANCES = new AtomicBoolean();
    /**
     * 当前类型实例最大数量限制
     */
    private static final int INSTANCE_COUNT_LIMIT = 64;
    /**
     * init 状态
     */
    private static final int WORKER_STATE_INIT = 0;
    /**
     * started 状态
     */
    private static final int WORKER_STATE_STARTED = 1;
    /**
     * shutdown 状态
     */
    private static final int WORKER_STATE_SHUTDOWN = 2;
    /**
     * 用于同步更新 workerState
     */
    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> WORKER_STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");

    /**
     * 创建时间轮数组
     */
    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        Assert.isTrue(ticksPerWheel > 0 && ticksPerWheel <= 1073741824, "参数 ticksPerWheel 必须 > 0 & <=2^30");
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    /**
     * 将 ticksPerWheel 标准化为 2 的幂 (结果 = 2^n >= ticksPerWheel)
     */
    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int normalizedTicksPerWheel = ticksPerWheel - 1;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 1;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 2;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 4;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 8;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 16;
        return normalizedTicksPerWheel + 1;
    }

    /**
     * 当前类型创建了太多的实例处理
     */
    private static void reportTooManyInstances() {
        String resourceType = HashedWheelTimer.class.getSimpleName();
        log.error("你创造了太多" + resourceType + " 实例。" + resourceType + "是可重用的共享资源，因此只创建少数实例。");
    }

    /**
     * 时间轮基本时间长度(纳秒)
     */
    private final long tickDuration;
    /**
     * 时间轮数组
     */
    private final HashedWheelBucket[] wheel;
    /**
     * 计算时间轮数组下标使用的位运算掩码
     */
    private final int mask;
    /**
     * 时间轮调度逻辑
     */
    private final Worker worker = new Worker();
    /**
     * 时间轮调度线程
     */
    private final Thread workerThread;
    /**
     * 定时任务执行线程池
     */
    private final ExecutorService jobExecutor;
    /**
     * 当前实例的状态: 0 - init, 1 - started, 2 - shut down
     */
    private volatile int workerState = WORKER_STATE_INIT;
    /**
     * 时间轮的启动时间(纳秒)
     */
    private volatile long startTime;
    /**
     * 同步调度线程和使用该类的线程(用于: 初始化通知)
     */
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);
    /**
     * 待处理的 HashedWheelTimeout 队列
     */
    private final Queue<HashedWheelTimeout> timeouts = new LinkedBlockingQueue<>();
    /**
     * 已取消的 HashedWheelTimeout 队列
     */
    private final Queue<HashedWheelTimeout> cancelledTimeouts = new LinkedBlockingQueue<>();
    /**
     * 待处理的 HashedWheelTimeout 数量
     */
    private final AtomicLong pendingTimeouts = new AtomicLong(0);
    /**
     * 最大支持的待处理的 HashedWheelTimeout 数量
     */
    private final long maxPendingTimeouts;

    /**
     * 创建一个新的定时任务调度器
     */
    public HashedWheelTimer() {
        this(Executors.defaultThreadFactory(), Executors.newSingleThreadExecutor());
    }

    /**
     * 创建一个新的定时任务调度器
     *
     * @param tickDuration 时间轮基本时间长度
     * @param unit         时间轮基本时间长度单位
     * @throws IllegalArgumentException 参数值校验失败
     */
    public HashedWheelTimer(long tickDuration, TimeUnit unit) {
        this(Executors.defaultThreadFactory(), Executors.newSingleThreadExecutor(), tickDuration, unit);
    }

    /**
     * 创建一个新的定时任务调度器
     *
     * @param tickDuration  时间轮基本时间长度
     * @param unit          时间轮基本时间长度单位
     * @param ticksPerWheel 时间轮数组大小
     * @throws IllegalArgumentException 参数值校验失败
     */
    public HashedWheelTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(Executors.defaultThreadFactory(), Executors.newSingleThreadExecutor(), tickDuration, unit, ticksPerWheel);
    }

    /**
     * 创建一个新的定时任务调度器
     *
     * @param threadFactory 用于创建调度线程的 {@link ThreadFactory}，只会创建一个调度线程
     * @param jobExecutor   定时任务执行线程池
     * @throws IllegalArgumentException 参数值校验失败
     */
    public HashedWheelTimer(ThreadFactory threadFactory, ExecutorService jobExecutor) {
        this(threadFactory, jobExecutor, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 创建一个新的定时任务调度器
     *
     * @param threadFactory 用于创建调度线程的 {@link ThreadFactory}，只会创建一个调度线程
     * @param jobExecutor   定时任务执行线程池
     * @param tickDuration  时间轮基本时间长度
     * @param unit          时间轮基本时间长度单位
     * @throws IllegalArgumentException 参数值校验失败
     */
    public HashedWheelTimer(ThreadFactory threadFactory, ExecutorService jobExecutor, long tickDuration, TimeUnit unit) {
        this(threadFactory, jobExecutor, tickDuration, unit, 512);
    }

    /**
     * 创建一个新的定时任务调度器
     *
     * @param threadFactory 用于创建调度线程的 {@link ThreadFactory}，只会创建一个调度线程
     * @param jobExecutor   定时任务执行线程池
     * @param tickDuration  时间轮基本时间长度
     * @param unit          时间轮基本时间长度单位
     * @param ticksPerWheel 时间轮数组大小
     * @throws IllegalArgumentException 参数值校验失败
     */
    public HashedWheelTimer(ThreadFactory threadFactory, ExecutorService jobExecutor, long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(threadFactory, jobExecutor, tickDuration, unit, ticksPerWheel, -1);
    }

    /**
     * 创建一个新的定时任务调度器
     *
     * @param threadFactory      用于创建调度线程的 {@link ThreadFactory}，只会创建一个调度线程
     * @param jobExecutor        定时任务执行线程池
     * @param tickDuration       时间轮基本时间长度
     * @param unit               时间轮基本时间长度单位
     * @param ticksPerWheel      时间轮数组大小
     * @param maxPendingTimeouts 调用 {@code newTimeout} 后挂起的最大任务数量，将导致抛出 {@link java.util.concurrent.RejectedExecutionException}。如果小于等于0表示不限制
     * @throws IllegalArgumentException 参数值校验失败
     */
    public HashedWheelTimer(ThreadFactory threadFactory, ExecutorService jobExecutor, long tickDuration, TimeUnit unit, int ticksPerWheel, long maxPendingTimeouts) {
        Assert.notNull(threadFactory, "参数 threadFactory 不能为 null");
        Assert.notNull(jobExecutor, "参数 jobExecutor 不能为 null");
        Assert.isTrue(tickDuration > 0, "参数 tickDuration 必须 > 0");
        Assert.notNull(unit, "参数 unit 不能为 null");
        Assert.isTrue(ticksPerWheel > 0 && ticksPerWheel <= 1073741824, "参数 ticksPerWheel 必须 > 0 & <=2^30");
        // 初始化时间轮数组
        this.wheel = createWheel(ticksPerWheel);
        this.mask = wheel.length - 1;
        // 转换 tickDuration 到纳秒
        this.tickDuration = unit.toNanos(tickDuration);
        // 防止溢出
        if (this.tickDuration >= Long.MAX_VALUE / wheel.length) {
            throw new IllegalArgumentException(String.format("tickDuration: %d (expected: 0 < tickDuration in nanos < %d", tickDuration, Long.MAX_VALUE / wheel.length));
        }
        this.workerThread = threadFactory.newThread(worker);
        this.jobExecutor = jobExecutor;
        this.maxPendingTimeouts = maxPendingTimeouts;
        if (INSTANCE_COUNTER.incrementAndGet() > INSTANCE_COUNT_LIMIT && WARNED_TOO_MANY_INSTANCES.compareAndSet(false, true)) {
            reportTooManyInstances();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            // 该对象将被 GC 处理，我们要确保减少活动实例计数。
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
        }
    }

    /**
     * 显式启动后台线程。即使您没有调用此方法，后台线程也会按需自动启动。
     *
     * @throws IllegalStateException 如果这个计时器已经{@linkplain #stop() stop}了
     */
    public void start() {
        switch (WORKER_STATE_UPDATER.get(this)) {
            case WORKER_STATE_INIT:
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    workerThread.start();
                }
                break;
            case WORKER_STATE_STARTED:
                break;
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState");
        }
        // 等到 startTime 被 worker 初始化
        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
                // Ignore - 它很快就会准备好
            }
        }
    }

    @Override
    public Set<Timeout> stop() {
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(HashedWheelTimer.class.getSimpleName() + ".stop() 不能从 " + TimerTask.class.getSimpleName() + " 上调用");
        }
        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            // workerState 此时可以是 0 或 2。让它一直为 2。
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
            return Collections.emptySet();
        }
        try {
            boolean interrupted = false;
            while (workerThread.isAlive()) {
                workerThread.interrupt();
                try {
                    workerThread.join(100);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } finally {
            INSTANCE_COUNTER.decrementAndGet();
        }
        return worker.unprocessedTimeouts();
    }

    @Override
    public boolean isStop() {
        return WORKER_STATE_SHUTDOWN == WORKER_STATE_UPDATER.get(this);
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        Assert.notNull(task, "参数 task 不能为 null");
        Assert.notNull(unit, "参数 unit 不能为 null");
        long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();
        if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
            pendingTimeouts.decrementAndGet();
            throw new RejectedExecutionException("挂起的任务数(" + pendingTimeoutsCount + ")大于或等于最大允许挂起任务数(" + maxPendingTimeouts + ")");
        }
        start();
        // 将 HashedWheelTimeout 添加到将在下一个 tick 时处理的 HashedWheelTimeout 队列。
        // 在处理期间，所有排队的 HashedWheelTimeout 都将添加到正确的 HashedWheelBucket
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
        // 防止溢出
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
        timeouts.add(timeout);
        return timeout;
    }

    /**
     * 返回待处理的 {@link Timeout} 数量
     */
    public long pendingTimeouts() {
        return pendingTimeouts.get();
    }

    /**
     * 存储 HashedWheelTimeouts 的桶。这些存储在类似数据结构的链表中，以便轻松删除中间的 HashedWheelTimeouts。
     * 此外，HashedWheelTimeout 本身充当节点，因此不需要创建额外的对象。
     */
    private static final class HashedWheelBucket {
        /**
         * 连表头部，用于链表数据结构
         */
        private HashedWheelTimeout head;
        /**
         * 连表尾部，用于链表数据结构
         */
        private HashedWheelTimeout tail;

        /**
         * 添加 {@link HashedWheelTimeout} 到这个桶
         */
        void addTimeout(HashedWheelTimeout timeout) {
            assert timeout.bucket == null;
            timeout.bucket = this;
            if (head == null) {
                head = tail = timeout;
            } else {
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }

        /**
         * 在给定的 {@code deadline} 内使所有 {@link HashedWheelTimeout} 过期
         */
        void expireTimeouts(long deadline) {
            HashedWheelTimeout timeout = head;
            // 处理所有的任务(HashedWheelTimeout)
            while (timeout != null) {
                HashedWheelTimeout next = timeout.next;
                if (timeout.remainingRounds <= 0) {
                    // 需要执行任务
                    next = remove(timeout);
                    if (timeout.deadline <= deadline) {
                        timeout.expire();
                    } else {
                        // 任务被放置在错误的插槽中。这永远不应该发生。
                        throw new IllegalStateException(String.format("timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
                    }
                } else if (timeout.isCancelled()) {
                    // 任务已取消
                    next = remove(timeout);
                } else {
                    // 减少时间轮圈数
                    timeout.remainingRounds--;
                }
                timeout = next;
            }
        }

        public HashedWheelTimeout remove(HashedWheelTimeout timeout) {
            HashedWheelTimeout next = timeout.next;
            // 通过更新链接列表删除已处理或取消的任务
            if (timeout.prev != null) {
                timeout.prev.next = next;
            }
            if (timeout.next != null) {
                timeout.next.prev = timeout.prev;
            }
            if (timeout == head) {
                // 如果任务在尾部我们也需要调整条目
                if (timeout == tail) {
                    tail = null;
                    head = null;
                } else {
                    head = next;
                }
            } else if (timeout == tail) {
                // 如果任务在尾部修改尾部为上一个节点
                tail = timeout.prev;
            }
            // 清空 prev、next 和 bucket 以允许 GC
            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
            timeout.timer.pendingTimeouts.decrementAndGet();
            return next;
        }

        /**
         * 清除所有的任务，并返回未执行且未取消的任务
         */
        void clearTimeouts(Set<Timeout> set) {
            while (true) {
                HashedWheelTimeout timeout = pollTimeout();
                if (timeout == null) {
                    return;
                }
                if (timeout.isExpired() || timeout.isCancelled()) {
                    continue;
                }
                set.add(timeout);
            }
        }

        private HashedWheelTimeout pollTimeout() {
            HashedWheelTimeout head = this.head;
            if (head == null) {
                return null;
            }
            HashedWheelTimeout next = head.next;
            if (next == null) {
                tail = this.head = null;
            } else {
                this.head = next;
                next.prev = null;
            }
            // 清空 prev 和 next 以允许 GC
            head.next = null;
            head.prev = null;
            head.bucket = null;
            return head;
        }
    }

    private final class Worker implements Runnable {
        /**
         * 需要执行但未执行的任务
         */
        private final Set<Timeout> unprocessedTimeouts = new HashSet<>();
        /**
         * 时间轮数组的刻度指向
         */
        private long tick;

        @Override
        public void run() {
            // 初始化 startTime
            startTime = System.nanoTime();
            if (startTime == 0) {
                // 我们这里使用0作为未初始化值的指示符，所以要保证初始化的时候不是0
                startTime = 1;
            }
            // 在 start() 时通知等待初始化的其他线程
            startTimeInitialized.countDown();
            do {
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    // 计算时间轮数组的下标
                    int idx = (int) (tick & mask);
                    // 清空已取消的任务
                    processCancelledTasks();
                    // 分配待处理的任务到时间轮里
                    transferTimeoutsToBuckets();
                    // 调度执行当前到期的任务
                    HashedWheelBucket bucket = wheel[idx];
                    bucket.expireTimeouts(deadline);
                    tick++;
                }
            } while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);
            // 填充 unprocessedTimeouts 以便我们可以从 stop() 方法返回它们
            for (HashedWheelBucket bucket : wheel) {
                bucket.clearTimeouts(unprocessedTimeouts);
            }
            while (true) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (!timeout.isCancelled()) {
                    unprocessedTimeouts.add(timeout);
                }
            }
            processCancelledTasks();
        }

        private void transferTimeoutsToBuckets() {
            // 每个 tick 最多读取 10W 个 HashedWheelTimeout，以防止线程在循环中添加新 HashedWheelTimeout 时使 workerThread 过时
            for (int i = 0; i < 100000; i++) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    // 已处理完成
                    break;
                }
                if (timeout.state() == HashedWheelTimeout.ST_CANCELLED) {
                    // 任务已经取消了
                    continue;
                }
                long calculated = timeout.deadline / tickDuration;
                timeout.remainingRounds = (calculated - tick) / wheel.length;
                // 确保任务放在未走过的时间轮 tick 下
                final long ticks = Math.max(calculated, tick);
                int stopIndex = (int) (ticks & mask);
                HashedWheelBucket bucket = wheel[stopIndex];
                bucket.addTimeout(timeout);
            }
        }

        /**
         * 清除已取消的任务
         */
        private void processCancelledTasks() {
            while (true) {
                HashedWheelTimeout timeout = cancelledTimeouts.poll();
                if (timeout == null) {
                    // 已处理完成
                    break;
                }
                try {
                    timeout.remove();
                } catch (Throwable t) {
                    if (log.isWarnEnabled()) {
                        log.warn("处理取消任务时抛出异常", t);
                    }
                }
            }
        }

        /**
         * 从 startTime 和当前刻度数(tick number)计算目标 nanoTime，然后等到达到该目标
         *
         * @return 如果收到关闭请求，则为 Long.MIN_VALUE，否则为当前时间（Long.MIN_VALUE 更改为 +1）
         */
        private long waitForNextTick() {
            long deadline = tickDuration * (tick + 1);
            while (true) {
                final long currentTime = System.nanoTime() - startTime;
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;
                if (sleepTimeMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    } else {
                        return currentTime;
                    }
                }
                if (PlatformOS.isWindows()) {
                    sleepTimeMs = sleepTimeMs / 10 * 10;
                }
                try {
                    // noinspection BusyWait
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                    if (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        /**
         * 需要执行但未执行的任务
         */
        Set<Timeout> unprocessedTimeouts() {
            return Collections.unmodifiableSet(unprocessedTimeouts);
        }
    }

    private static final class HashedWheelTimeout implements Timeout {
        /**
         * init 状态
         */
        private static final int ST_INIT = 0;
        /**
         * cancelled 状态
         */
        private static final int ST_CANCELLED = 1;
        /**
         * expired 状态
         */
        private static final int ST_EXPIRED = 2;
        /**
         * 用于同步更新 state
         */
        private static final AtomicIntegerFieldUpdater<HashedWheelTimeout> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimeout.class, "state");
        /**
         * 所属的调度器
         */
        private final HashedWheelTimer timer;
        /**
         * 定时任务
         */
        private final TimerTask task;
        /**
         * 计划执行时间，相对于 {@link HashedWheelTimer#startTime} 的时间(纳秒)
         */
        private final long deadline;
        /**
         * 任务状态
         */
        private volatile int state = ST_INIT;
        /**
         * 剩余时间轮的圈数 <br />
         * 在将 HashedWheelTimeout 添加到正确的 HashedWheelBucket 之前，RemainingRounds 将由 Worker.transferTimeoutsToBuckets() 计算和设置。
         */
        long remainingRounds;
        /**
         * 上一个任务 <br />
         * 这将用于通过双向链表链接 HashedWheelTimer Bucket 中的 HashedWheelTimeout。
         * 因为只有 workerThread 会作用于它，所以不需要同步 volatile
         */
        HashedWheelTimeout next;
        /**
         * 下一个任务 <br />
         * 这将用于通过双向链表链接 HashedWheelTimer Bucket 中的 HashedWheelTimeout。
         * 因为只有 workerThread 会作用于它，所以不需要同步 volatile
         */
        HashedWheelTimeout prev;
        /**
         * 所属的时间轮数组项(Bucket)
         */
        HashedWheelBucket bucket;

        HashedWheelTimeout(HashedWheelTimer timer, TimerTask task, long deadline) {
            this.timer = timer;
            this.task = task;
            this.deadline = deadline;
        }

        @Override
        public Timer timer() {
            return timer;
        }

        @Override
        public TimerTask task() {
            return task;
        }

        @Override
        public boolean cancel() {
            // 仅更新状态，将在下一次更新时从 HashedWheelBucket 中删除的状态
            if (!compareAndSetState(ST_INIT, ST_CANCELLED)) {
                return false;
            }
            // 通知调度器当前任务被取消了
            timer.cancelledTimeouts.add(this);
            return true;
        }

        /**
         * 执行当前任务
         */
        public void expire() {
            if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
                return;
            }
            timer.jobExecutor.execute(() -> {
                try {
                    task.run(this);
                } catch (Throwable t) {
                    if (log.isWarnEnabled()) {
                        log.warn("异常被抛出" + task.getClass().getSimpleName() + '。', t);
                    }
                }
            });
        }

        /**
         * 删除当前任务
         */
        void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket != null) {
                bucket.remove(this);
            } else {
                timer.pendingTimeouts.decrementAndGet();
            }
        }

        @SuppressWarnings({"SameParameterValue", "BooleanMethodIsAlwaysInverted"})
        private boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }

        public int state() {
            return state;
        }

        @Override
        public boolean isCancelled() {
            return state() == ST_CANCELLED;
        }

        @Override
        public boolean isExpired() {
            return state() == ST_EXPIRED;
        }

        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            long remaining = deadline - currentTime + timer.startTime;
            String simpleClassName = this.getClass().getSimpleName();
            StringBuilder buf = new StringBuilder(192).append(simpleClassName).append('(').append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining).append(" ns later");
            } else if (remaining < 0) {
                buf.append(-remaining).append(" ns ago");
            } else {
                buf.append("now");
            }
            if (isCancelled()) {
                buf.append(", cancelled");
            }
            return buf.append(", task: ").append(task()).append(')').toString();
        }
    }
}