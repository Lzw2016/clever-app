package org.clever.task.core.support;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.PlatformOS;
import org.clever.core.timer.HashedWheelTimer;
import org.clever.util.Assert;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 时间轮算法定时调度器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/05/23 17:07 <br/>
 */
@Slf4j
public class WheelTimer {
    public interface State {
        int INIT = 0;
        int STARTED = 1;
        int SHUTDOWN = 2;
    }

    public interface TaskState {
        int INIT = 0;
        int CANCELLED = 1;
        int EXECUTED = 2;
    }

    public interface Task {
        void run(TaskInfo taskInfo) throws Exception;
    }

    /**
     * 用于同步更新 workerState
     */
    private static final AtomicIntegerFieldUpdater<WheelTimer> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(WheelTimer.class, "workerState");

    /**
     * 创建时间轮数组
     */
    private static WheelBucket[] createWheel(int ticksPerWheel) {
        Assert.isTrue(ticksPerWheel > 0 && ticksPerWheel <= 1073741824, "参数 ticksPerWheel 必须 > 0 & <=2^30");
        ticksPerWheel = HashedWheelTimer.normalizeTicksPerWheel(ticksPerWheel);
        WheelBucket[] wheel = new WheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new WheelBucket();
        }
        return wheel;
    }

    /**
     * 时间轮的启动时间(纳秒)
     */
    private volatile long startTime;
    /**
     * 时间轮基本时间长度(纳秒)
     */
    private final long tickDuration;
    /**
     * 时间轮数组
     */
    private final WheelBucket[] wheel;
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
    private final ExecutorService taskExecutor;
    /**
     * 当前实例的状态，{@link State}
     */
    private volatile int workerState = State.INIT;
    /**
     * 同步调度线程和使用该类的线程(用于: 初始化通知)
     */
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);
    /**
     * 待处理的 TaskInfo 队列
     */
    private final Queue<TaskInfo> tasks = new LinkedBlockingQueue<>();

    /**
     * 创建一个新的定时任务调度器
     *
     * @param threadFactory 用于创建调度线程的 {@link ThreadFactory}，只会创建一个调度线程
     * @param taskExecutor  定时任务执行线程池
     * @param tickDuration  时间轮基本时间长度
     * @param unit          时间轮基本时间长度单位
     * @param ticksPerWheel 时间轮数组大小
     * @throws IllegalArgumentException 参数值校验失败
     */
    public WheelTimer(ThreadFactory threadFactory, ExecutorService taskExecutor, long tickDuration, TimeUnit unit, int ticksPerWheel) {
        Assert.notNull(threadFactory, "参数 threadFactory 不能为 null");
        Assert.notNull(taskExecutor, "参数 taskExecutor 不能为 null");
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
        this.taskExecutor = taskExecutor;
    }

    /**
     * 显式启动后台线程。即使您没有调用此方法，后台线程也会按需自动启动。
     *
     * @throws IllegalStateException 如果这个计时器已经{@linkplain #stop() stop}了
     */
    public void start() {
        switch (STATE_UPDATER.get(this)) {
            case State.INIT:
                if (STATE_UPDATER.compareAndSet(this, State.INIT, State.STARTED)) {
                    workerThread.start();
                }
                break;
            case State.STARTED:
                break;
            case State.SHUTDOWN:
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

    /**
     * 停止定时任务调度，并释放所有的任务，清空所有集合
     */
    public void stop() {
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(WheelTimer.class.getSimpleName() + ".stop() 不能从 " + Task.class.getSimpleName() + " 上调用");
        }
        if (!STATE_UPDATER.compareAndSet(this, State.STARTED, State.SHUTDOWN)) {
            return;
        }
        boolean interrupted = false;
        while (workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(120);
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 调度指定的 {@link Task} 在指定的延迟后一次性执行
     *
     * @param task  后台任务
     * @param delay 延迟时间
     * @param unit  延迟时间单位
     * @return 与指定任务关联的对象
     * @throws IllegalStateException 如果这个计时器已经{@linkplain #stop() stop}了
     */
    public TaskInfo addTask(Task task, long delay, TimeUnit unit) {
        Assert.notNull(task, "参数 task 不能为 null");
        Assert.notNull(unit, "参数 unit 不能为 null");
        start();
        // 将 TaskInfo 添加到待处理的 Task 队列
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
        // 防止溢出
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        TaskInfo taskInfo = new TaskInfo(this, task, deadline);
        tasks.add(taskInfo);
        return taskInfo;
    }

    /**
     * 调度器是否停止
     */
    public boolean isStop() {
        return State.SHUTDOWN == STATE_UPDATER.get(this);
    }

    /**
     * 返回定时任务调度器状态 {@link State}
     */
    public int getState() {
        return STATE_UPDATER.get(this);
    }

    /**
     * 待处理的任务数量
     */
    public int pendingTasks() {
        return tasks.size();
    }

    /**
     * 时间轮调度逻辑对象
     */
    public Worker getWorker() {
        return worker;
    }

    /**
     * 时间轮刻度上的节点(链表)
     */
    private static final class WheelBucket {
        /**
         * 连表头部，用于链表数据结构
         */
        private TaskInfo head;
        /**
         * 连表尾部，用于链表数据结构
         */
        private TaskInfo tail;

        /**
         * 添加 {@link TaskInfo} 到当前时间轮刻度上
         */
        void addTask(TaskInfo taskInfo) {
            Assert.isTrue(taskInfo.bucket == null, "当前 TaskInfo 已分配到 WheelBucket 中，不能重复分配");
            taskInfo.bucket = this;
            if (head == null) {
                head = tail = taskInfo;
            } else {
                tail.next = taskInfo;
                taskInfo.prev = tail;
                tail = taskInfo;
            }
        }

        /**
         * 在给定的 {@code deadline} 内，执行所有的 {@link TaskInfo}
         */
        void executeTasks(long deadline) {
            TaskInfo taskInfo = head;
            // 处理所有的任务(TaskInfo)
            while (taskInfo != null) {
                TaskInfo next = taskInfo.next;
                if (taskInfo.remainingRounds <= 0) {
                    // 需要执行任务
                    next = remove(taskInfo);
                    if (taskInfo.deadline <= deadline) {
                        taskInfo.execute();
                    } else {
                        // 任务被放置在错误的插槽中。这永远不应该发生。
                        throw new IllegalStateException(String.format("taskInfo.deadline (%d) > deadline (%d)", taskInfo.deadline, deadline));
                    }
                } else if (taskInfo.isCancelled()) {
                    // 任务已取消
                    next = remove(taskInfo);
                } else {
                    // 减少时间轮圈数
                    taskInfo.remainingRounds--;
                }
                taskInfo = next;
            }
        }

        public TaskInfo remove(TaskInfo taskInfo) {
            TaskInfo next = taskInfo.next;
            // 通过更新链接列表删除已处理或取消的任务
            if (taskInfo.prev != null) {
                taskInfo.prev.next = next;
            }
            if (taskInfo.next != null) {
                taskInfo.next.prev = taskInfo.prev;
            }
            if (taskInfo == head) {
                // 如果任务在尾部我们也需要调整条目
                if (taskInfo == tail) {
                    tail = null;
                    head = null;
                } else {
                    head = next;
                }
            } else if (taskInfo == tail) {
                // 如果任务在尾部修改尾部为上一个节点
                tail = taskInfo.prev;
            }
            // 清空 prev、next 和 bucket 以允许 GC
            taskInfo.prev = null;
            taskInfo.next = null;
            taskInfo.bucket = null;
            return next;
        }

        /**
         * 清除所有的任务
         */
        void clearTasks() {
            while (true) {
                TaskInfo taskInfo = pollTaskInfo();
                if (taskInfo == null) {
                    break;
                }
            }
        }

        private TaskInfo pollTaskInfo() {
            TaskInfo head = this.head;
            if (head == null) {
                return null;
            }
            TaskInfo next = head.next;
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

    /**
     * 任务信息(任务上下文)
     */
    public static final class TaskInfo {
        /**
         * 用于同步更新 state
         */
        private static final AtomicIntegerFieldUpdater<TaskInfo> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(TaskInfo.class, "state");
        /**
         * 所属的调度器
         */
        private final WheelTimer timer;
        /**
         * 所属的时间轮数组项(Bucket)
         */
        private WheelBucket bucket;
        /**
         * 上一个任务
         */
        private TaskInfo next;
        /**
         * 下一个任务
         */
        private TaskInfo prev;
//        /**
//         * TODO 任务关联的数据对象
//         */
//        private final Object data;
        /**
         * 定时任务
         */
        private final Task task;
        /**
         * 计划执行时间，相对于 {@link WheelTimer#startTime} 的时间(纳秒)
         */
        private final long deadline;
        /**
         * 剩余时间轮的圈数
         */
        private long remainingRounds;
        /**
         * 任务状态
         */
        private volatile int state = TaskState.INIT;

        TaskInfo(WheelTimer timer, Task task, long deadline) {
            this.timer = timer;
            this.task = task;
            this.deadline = deadline;
        }

        /**
         * 定时任务调度器
         */
        public WheelTimer getTimer() {
            return timer;
        }

        /**
         * 定时任务
         */
        public Task getTask() {
            return task;
        }

        /**
         * 取消定时任务
         *
         * @return 取消成功返回 true
         */
        public boolean cancel() {
            // 仅更新状态，将在下一次更新时从 WheelBucket 中删除的状态
            return compareAndSetState(TaskState.INIT, TaskState.CANCELLED);
        }

        /**
         * 执行当前任务
         */
        public void execute() {
            if (!compareAndSetState(TaskState.INIT, TaskState.EXECUTED)) {
                return;
            }
            try {
                timer.taskExecutor.execute(() -> {
                    try {
                        task.run(this);
                    } catch (Throwable t) {
                        if (log.isWarnEnabled()) {
                            log.warn("throwing an exception while executing a timed task: {}", task.getClass().getSimpleName(), t);
                        }
                    }
                });
            } catch (Throwable e) {
                log.error("scheduled task scheduling failed", e);
            }
        }

        /**
         * 删除当前任务
         */
        void remove() {
            WheelBucket bucket = this.bucket;
            if (bucket != null) {
                bucket.remove(this);
            }
        }

        /**
         * 当前任务是否取消
         */
        public boolean isCancelled() {
            return state == TaskState.CANCELLED;
        }

        /**
         * 当前任务是否已执行
         */
        public boolean isExecuted() {
            return state == TaskState.EXECUTED;
        }

        /**
         * 任务计划执行时间，相对于 startTime 的时间(纳秒)
         */
        public long getDeadline() {
            return deadline;
        }

        /**
         * 当前任务状态 {@link TaskState}
         */
        public int getState() {
            return state;
        }

        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            long remaining = deadline - currentTime + timer.startTime;
            String simpleClassName = this.getClass().getSimpleName();
            StringBuilder buf = new StringBuilder(255).append(simpleClassName).append('(').append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining).append(" ns later");
            } else if (remaining < 0) {
                buf.append(-remaining).append(" ns ago");
            } else {
                buf.append("now");
            }
            if (isExecuted()) {
                buf.append(", executed");
            }
            if (isCancelled()) {
                buf.append(", cancelled");
            }
            return buf.append(", task: ").append(task).append(')').toString();
        }

        @SuppressWarnings({"SameParameterValue", "BooleanMethodIsAlwaysInverted"})
        private boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }
    }

    /**
     * 调度器逻辑
     */
    private final class Worker implements Runnable {
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
                    // 分配待处理的任务到时间轮里
                    transferTasksToBuckets();
                    // 调度执行当前到期的任务
                    WheelBucket bucket = wheel[idx];
                    bucket.executeTasks(deadline);
                    tick++;
                }
            } while (STATE_UPDATER.get(WheelTimer.this) == State.STARTED);
            // 清除未执行的任务
            for (WheelBucket bucket : wheel) {
                bucket.clearTasks();
            }
            // 清除未处理的任务
            while (true) {
                TaskInfo taskInfo = tasks.poll();
                if (taskInfo == null) {
                    break;
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
                long sleepTimeMs = ((deadline - currentTime) + 999999) / 1000000;
                if (sleepTimeMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    } else {
                        return currentTime;
                    }
                }
                // See https://github.com/netty/netty/issues/356
                // 这里是为了处理在windows系统上的一个bug，如果sleep不够10ms则要取整(https://www.javamex.com/tutorials/threads/sleep_issues.shtml)
                if (PlatformOS.isWindows()) {
                    sleepTimeMs = sleepTimeMs / 10 * 10;
                }
                try {
                    // noinspection BusyWait
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                    if (STATE_UPDATER.get(WheelTimer.this) == State.SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        /**
         * 分配待处理的任务到时间轮里
         */
        private void transferTasksToBuckets() {
            // 每个 tick 最多读取 10W 个 TaskInfo，以防止线程在循环中添加新 TaskInfo 时使 workerThread 过时
            for (int i = 0; i < 100000; i++) {
                TaskInfo taskInfo = tasks.poll();
                if (taskInfo == null) {
                    // 已处理完成
                    break;
                }
                if (taskInfo.state == TaskState.CANCELLED) {
                    // 任务已经取消了
                    continue;
                }
                long calculated = taskInfo.deadline / tickDuration;
                taskInfo.remainingRounds = (calculated - tick) / wheel.length;
                // TODO 需要更新 TaskInfo
                // 确保任务放在未走过的时间轮 tick 下
                final long ticks = Math.max(calculated, tick);
                int stopIndex = (int) (ticks & mask);
                WheelBucket bucket = wheel[stopIndex];
                bucket.addTask(taskInfo);
            }
        }
    }
}
