package org.clever.core.flow;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.OrderComparator;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.tuples.TupleTwo;
import org.clever.util.Assert;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 任务节点定义
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:23 <br/>
 */
@Slf4j
public class WorkerNode {
    private static final AtomicIntegerFieldUpdater<WorkerNode> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(WorkerNode.class, "state");

    /**
     * WorkerNode唯一ID
     */
    @Getter
    private final String id;
    /**
     * 任务名称
     */
    @Getter
    private final String name;
    /**
     * 任务参数
     */
    @Getter
    private final WorkerVariable param;
    /**
     * 任务
     */
    @Getter
    private final Worker worker;
    /**
     * 任务回调
     */
    private final List<Callback> callbacks;
    /**
     * 忽略错误(当前任务节点执行错误不影响下一个任务执行)
     */
    @Getter
    private final boolean ignoreErr;
    /**
     * 当前任务节点之前的任务节点(指向当前任务节点的任务节点)
     */
    private final List<PrevWorker> prevWorkers;
    /**
     * 当前任务节点之后的任务节点(当前任务节点指向的任务节点)
     */
    private final List<NextWorker> nextWorkers;
    /**
     * 节点任务运行状态，参照 {@link WorkerState}
     */
    private volatile int state = WorkerState.INIT;

    /**
     * 应该使用 {@link Builder} 构建 {@link WorkerNode} 实例
     *
     * @param id          WorkerNode唯一ID
     * @param name        任务名称
     * @param param       任务参数
     * @param worker      任务
     * @param callbacks   任务回调
     * @param ignoreErr   忽略错误(当前任务节点执行错误不影响下一个任务执行)
     * @param prevWorkers 当前任务节点之前的任务节点(指向当前任务节点的任务节点)
     * @param nextWorkers 当前任务节点之后的任务节点(当前任务节点指向的任务节点)
     */
    public WorkerNode(String id,
                      String name,
                      WorkerVariable param,
                      Worker worker,
                      List<Callback> callbacks,
                      boolean ignoreErr,
                      List<PrevWorker> prevWorkers,
                      List<NextWorker> nextWorkers) {
        Assert.isNotBlank(id, "参数 id 不能为空");
        Assert.isNotBlank(name, "参数 name 不能为空");
        Assert.notNull(param, "参数 param 不能为 null");
        Assert.notNull(worker, "参数 worker 不能为 null");
        Assert.notNull(callbacks, "参数 callbacks 不能为 null");
        Assert.notNull(prevWorkers, "参数 prevWorkers 不能为 null");
        Assert.notNull(nextWorkers, "参数 nextWorkers 不能为 null");
        OrderComparator.sort(callbacks);
        this.id = StringUtils.trim(id);
        this.name = StringUtils.trim(name);
        this.param = param.toUnmodifiable();
        this.worker = worker;
        this.callbacks = callbacks;
        this.ignoreErr = ignoreErr;
        this.prevWorkers = prevWorkers;
        this.nextWorkers = nextWorkers;
    }

    /**
     * 任务回调
     */
    public List<Callback> getCallbacks() {
        return Collections.unmodifiableList(callbacks);
    }

    /**
     * 当前任务节点之前的任务节点(指向当前任务节点的任务节点)
     */
    public List<PrevWorker> getPrevWorkers() {
        return Collections.unmodifiableList(prevWorkers);
    }

    /**
     * 当前任务节点之后的任务节点(当前任务节点指向的任务节点)
     */
    public List<NextWorker> getNextWorkers() {
        return Collections.unmodifiableList(nextWorkers);
    }

    /**
     * 节点任务运行状态，参照 {@link WorkerState}
     */
    public int getState() {
        return STATE_UPDATER.get(this);
    }

    /**
     * 判断当前 WorkerNode 是否可修改配置，只有是 WorkerState.INIT 状态才能修改配置
     */
    public boolean isModifiable() {
        return Objects.equals(getState(), WorkerState.INIT);
    }

    /**
     * 检查当前 WorkerNode 是否可修改，如果不能修改就抛出 IllegalArgumentException 异常
     */
    public void checkModifiable() {
        if (!isModifiable()) {
            throw new IllegalArgumentException("WorkerNode已启动过,不可修改");
        }
    }

    /**
     * 判断当前任务节点是否是入口任务(不存在前置任务)
     */
    public boolean isEntryWorker() {
        return prevWorkers.isEmpty();
    }

    /**
     * 当前任务节点是否未结束
     */
    public boolean notCompleted() {
        int state = getState();
        return Objects.equals(state, WorkerState.INIT) || Objects.equals(state, WorkerState.RUNNING);
    }

    /**
     * 当前任务节点是否已经结束
     */
    public boolean isCompleted() {
        return !notCompleted();
    }

    /**
     * 新增一个前置的任务节点。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param prev           之前的任务节点
     * @param waitComplete   是否必须等待 prev 任务执行完成，才能继续执行当前任务
     * @param canSkip        如果当前任务已经执行完成，能否跳过 prev 任务执行
     * @param updateIfExists 如果 prev 任务已经在前置的任务节点中，是否直接更新配置
     */
    public void addPrev(WorkerNode prev, boolean waitComplete, boolean canSkip, boolean updateIfExists) {
        checkModifiable();
        Assert.notNull(prev, "参数 prev 不能为空");
        Assert.isFalse(Objects.equals(prev, this), "无效的参数 prev==this");
        NextWorker nextExists = prev.nextWorkers.stream().filter(worker -> worker.getNext() == this).findFirst().orElse(null);
        PrevWorker prevExists = prevWorkers.stream().filter(worker -> worker.getPrev() == prev).findFirst().orElse(null);
        if (nextExists == null || updateIfExists) {
            if (nextExists != null) {
                prev.nextWorkers.remove(nextExists);
            }
            prev.nextWorkers.add(new NextWorker(prev, this, canSkip));
        }
        if (prevExists == null || updateIfExists) {
            if (prevExists != null) {
                prevWorkers.remove(prevExists);
            }
            prevWorkers.add(new PrevWorker(this, prev, waitComplete));
        }
    }

    /**
     * 新增一个前置的任务节点，对于同一个prev，如果调用多次，以最后一次调用为准。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param prev         之前的任务节点
     * @param waitComplete 是否必须等待 prev 任务执行完成，才能继续执行当前任务
     * @param canSkip      如果当前任务已经执行完成，能否跳过 prev 任务执行
     */
    public void addPrev(WorkerNode prev, boolean waitComplete, boolean canSkip) {
        addPrev(prev, waitComplete, canSkip, true);
    }

    /**
     * 新增一个前置的任务节点，对于同一个prev，如果调用多次，以最后一次调用为准。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param prev 之前的任务节点
     */
    public void addPrev(WorkerNode... prev) {
        if (prev != null) {
            for (WorkerNode worker : prev) {
                addPrev(worker, true, false, true);
            }
        }
    }

    /**
     * 删除前置的任务节点
     *
     * @param prev 之前的任务节点
     */
    public void removePrev(WorkerNode prev) {
        checkModifiable();
        if (prev == null) {
            return;
        }
        PrevWorker prevWorker = prevWorkers.stream().filter(worker -> Objects.equals(worker.getPrev(), prev)).findFirst().orElse(null);
        if (prevWorker == null) {
            return;
        }
        prevWorkers.remove(prevWorker);
        NextWorker nextWorker = prev.nextWorkers.stream().filter(worker -> Objects.equals(worker.getNext(), this)).findFirst().orElse(null);
        if (nextWorker == null) {
            log.warn("WorkerNode(id={}, name={}) 与 prev WorkerNode(id={}, name={}) 缺少 next 关系", id, name, prev.id, prev.name);
            return;
        }
        prev.nextWorkers.remove(nextWorker);
    }

    /**
     * 新增一个后续的任务节点。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param next           之后的任务节点
     * @param waitComplete   是否必须等待当前任务执行完成，才能继续执行 next 任务
     * @param canSkip        如果 next 任务已经执行完成，能否跳过当前任务执行
     * @param updateIfExists 如果 next 任务已经在后续的任务节点中，是否直接更新配置
     */
    public void addNext(WorkerNode next, boolean waitComplete, boolean canSkip, boolean updateIfExists) {
        checkModifiable();
        Assert.notNull(next, "参数 next 不能为空");
        Assert.isFalse(Objects.equals(next, this), "无效的参数 next==this");
        PrevWorker prevExists = next.prevWorkers.stream().filter(worker -> worker.getPrev() == this).findFirst().orElse(null);
        NextWorker nextExists = nextWorkers.stream().filter(worker -> worker.getNext() == next).findFirst().orElse(null);
        if (prevExists == null || updateIfExists) {
            if (prevExists != null) {
                next.prevWorkers.remove(prevExists);
            }
            next.prevWorkers.add(new PrevWorker(next, this, waitComplete));
        }
        if (nextExists == null || updateIfExists) {
            if (nextExists != null) {
                nextWorkers.remove(nextExists);
            }
            nextWorkers.add(new NextWorker(this, next, canSkip));
        }
    }

    /**
     * 新增一个后续的任务节点，对于同一个next，如果调用多次，以最后一次调用为准。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param next         之后的任务节点
     * @param waitComplete 是否必须等待当前任务执行完成，才能继续执行 next 任务
     * @param canSkip      如果 next 任务已经执行完成，能否跳过当前任务执行
     */
    public void addNext(WorkerNode next, boolean waitComplete, boolean canSkip) {
        addNext(next, waitComplete, canSkip, true);
    }

    /**
     * 新增一个后续的任务节点，对于同一个next，如果调用多次，以最后一次调用为准。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param next 之后的任务节点
     */
    public void addNext(WorkerNode... next) {
        if (next != null) {
            for (WorkerNode worker : next) {
                addNext(worker, true, false, true);
            }
        }
    }

    /**
     * 删除后续的任务节点
     *
     * @param next 之后的任务节点
     */
    public void removeNext(WorkerNode next) {
        checkModifiable();
        if (next == null) {
            return;
        }
        NextWorker nextWorker = nextWorkers.stream().filter(worker -> Objects.equals(worker.getNext(), next)).findFirst().orElse(null);
        if (nextWorker == null) {
            return;
        }
        nextWorkers.remove(nextWorker);
        PrevWorker prevWorker = next.prevWorkers.stream().filter(worker -> Objects.equals(worker.getPrev(), this)).findFirst().orElse(null);
        if (prevWorker == null) {
            log.warn("WorkerNode(id={}, name={}) 与 next WorkerNode(id={}, name={}) 缺少 prev 关系", id, name, next.id, next.name);
            return;
        }
        next.prevWorkers.remove(prevWorker);
    }

    /**
     * 新增任务回调
     */
    public void addCallback(Callback... callbacks) {
        checkModifiable();
        if (callbacks != null) {
            this.callbacks.addAll(Arrays.asList(callbacks));
        }
    }

    /**
     * 删除任务回调
     */
    public void removeCallback(Callback... callbacks) {
        checkModifiable();
        if (callbacks != null) {
            this.callbacks.removeAll(Arrays.asList(callbacks));
        }
    }

    /**
     * 开始执行当前任务节点及其后续的任务节点。<br/>
     * 可重复调用，但只有第一次调用起作用。
     *
     * @param executor 任务执行器(线程池)
     */
    public CompletableFuture<WorkerContext> start(ExecutorService executor) {
        return WorkerFlow.start(executor, this);
    }

    /**
     * 开始执行当前任务节点及其后续的任务节点。<br/>
     * 可重复调用，但只有第一次调用起作用。
     */
    public CompletableFuture<WorkerContext> start() {
        return WorkerFlow.start(this);
    }

    /**
     * 开始执行当前任务节点及其后续的任务节点。<br/>
     * 可重复调用，但只有第一次调用起作用。
     *
     * @param workerContext 任务上下文
     * @param from          发起执行当前任务的任务节点(执行入口任务时为null)
     * @param executor      任务执行器(线程池)
     */
    CompletableFuture<Void> start(WorkerContext workerContext, WorkerNode from, ExecutorService executor) {
        Assert.notNull(workerContext, "参数 workerContext 不能为空");
        Assert.notNull(executor, "参数 executor 不能为空");
        final CompletableFuture<Void> nullFuture = CompletableFuture.completedFuture(null);
        // 当前任务已经执行完成
        if (isCompleted()) {
            return nullFuture;
        }
        // 是否必须等待 prev 任务执行完成
        if (from != null && !prevWorkers.isEmpty() && prevWorkers.stream().anyMatch(prev -> prev.isWaitComplete() && prev.getPrev().notCompleted())) {
            return nullFuture;
        }
        // 如果 next 任务已经执行完成，能否跳过 current 任务执行
        if (!nextWorkers.isEmpty() && nextWorkers.stream().allMatch(next -> next.isCanSkip() && next.getNext().isCompleted())) {
            setState(WorkerState.SKIPPED);
            return nullFuture;
        }
        // 当前任务已经在执行过了
        if (!setState(WorkerState.RUNNING, WorkerState.INIT)) {
            return nullFuture;
        }
        // 执行当前任务节点逻辑
        TupleTwo<Object, Throwable> tuple = executeWorker(workerContext, from);
        final Object result = tuple.getValue1();
        final Throwable err = tuple.getValue2();
        // 更新任务状态
        setState(err == null ? WorkerState.SUCCESS : WorkerState.ERROR);
        workerContext.setResult(this, result);
        if (err != null && !ignoreErr) {
            throw ExceptionUtils.unchecked(err);
        }
        if (err != null) {
            log.warn("WorkerNode(id={}, name={}) 执行失败", id, name, err);
        }
        // 触发后续任务节点执行
        List<CompletableFuture<CompletableFuture<Void>>> futures = new ArrayList<>(nextWorkers.size());
        for (NextWorker nextWorker : nextWorkers) {
            CompletableFuture<CompletableFuture<Void>> future = CompletableFuture.supplyAsync(
                () -> nextWorker.getNext().start(workerContext, this, executor), executor
            );
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(unused -> {
            // 等待所有的 nextWorkers 执行完毕
            CompletableFuture<Void> innerFuture = CompletableFuture.allOf(futures.stream().map(CompletableFuture::join).toArray(CompletableFuture[]::new));
            innerFuture.join();
            log.info("@@@ 等待所有的 nextWorkers 执行完毕");
        });
    }

    /**
     * 设置新的 state 值, 如果设置成功返回 true。<br/>
     * 参考 {@link WorkerState}
     *
     * @param state  新的 state 值
     * @param expect 预期当前的 state 值
     */
    private boolean setState(int state, int expect) {
        return STATE_UPDATER.compareAndSet(this, expect, state);
    }

    /**
     * 设置新的 state 值
     *
     * @param state 新的 state 值
     */
    private void setState(int state) {
        STATE_UPDATER.set(this, state);
    }

    /**
     * 执行当前任务节点逻辑
     *
     * @param workerContext 任务上下文
     * @param from          发起执行当前任务的任务节点(执行入口任务时为null)
     * @return {@code TupleTwo<result, err>}
     */
    private TupleTwo<Object, Throwable> executeWorker(WorkerContext workerContext, WorkerNode from) {
        final List<Callback> follows = new ArrayList<>(callbacks.size());
        Object result = null;
        Throwable err = null;
        // 执行 Callback.before
        final CallbackContext beforeContext = new CallbackContext(workerContext, this, from);
        for (Callback callback : callbacks) {
            try {
                follows.add(callback);
                callback.before(beforeContext);
            } catch (Throwable e) {
                err = e;
                break;
            }
        }
        Collections.reverse(follows);
        if (err == null) {
            // 执行 Worker.execute
            try {
                result = worker.execute(from, this, workerContext);
            } catch (Throwable e) {
                err = e;
            }
            // 执行 Callback.after
            if (err == null) {
                CallbackContext.After afterContext = new CallbackContext.After(workerContext, this, from, result);
                for (Callback callback : follows) {
                    try {
                        callback.after(afterContext);
                        result = afterContext.getResult();
                    } catch (Throwable e) {
                        err = e;
                        break;
                    }
                }
            }
        }
        // 执行 Callback.finallyHandle
        CallbackContext.Finally finallyContext = new CallbackContext.Finally(workerContext, this, from, result, err);
        for (Callback callback : follows) {
            try {
                callback.finallyHandle(finallyContext);
            } catch (Throwable e) {
                log.warn("Callback.finallyHandle 执行失败", e);
            }
        }
        return TupleTwo.creat(result, err);
    }

    private static class PrevOrNextWorker {
        private final boolean prev;
        private final WorkerNode prevOrNext;
        private final boolean waitComplete;
        private final boolean canSkip;
        private final boolean updateIfExists;

        private PrevOrNextWorker(boolean prev, WorkerNode prevOrNext, boolean waitComplete, boolean canSkip, boolean updateIfExists) {
            this.prev = prev;
            this.prevOrNext = prevOrNext;
            this.waitComplete = waitComplete;
            this.canSkip = canSkip;
            this.updateIfExists = updateIfExists;
        }
    }

    /**
     * 用于创建 WorkerNode 实例
     */
    public static class Builder {
        /**
         * WorkerNode唯一ID
         */
        private String id;
        /**
         * 任务名称
         */
        private String name;
        /**
         * 任务参数
         */
        private final WorkerVariable param = new WorkerVariable();
        /**
         * 任务
         */
        private Worker worker;
        /**
         * 任务回调
         */
        private final List<Callback> callbacks = new ArrayList<>();
        /**
         * 忽略错误(当前任务节点执行错误不影响下一个任务执行)
         */
        private boolean ignoreErr = false;
        /**
         * 用于存储 prevWorkers 和 nextWorkers
         */
        private final List<PrevOrNextWorker> prevOrNextWorkers = new ArrayList<>();

        public Builder() {
        }

        /**
         * @param id        WorkerNode唯一ID
         * @param name      任务名称
         * @param worker    任务
         * @param ignoreErr 忽略错误(当前任务节点执行错误不影响下一个任务执行)
         */
        public Builder(String id, String name, Worker worker, boolean ignoreErr) {
            this.id = id;
            this.name = name;
            this.worker = worker;
            this.ignoreErr = ignoreErr;
        }

        /**
         * @param id        WorkerNode唯一ID
         * @param name      任务名称
         * @param worker    任务
         * @param ignoreErr 忽略错误(当前任务节点执行错误不影响下一个任务执行)
         */
        public static Builder create(String id, String name, Worker worker, boolean ignoreErr) {
            return new Builder(id, name, worker, ignoreErr);
        }

        /**
         * @param id     WorkerNode唯一ID
         * @param name   任务名称
         * @param worker 任务
         */
        public static Builder create(String id, String name, Worker worker) {
            return new Builder(id, name, worker, false);
        }

        /**
         * @param name   任务名称
         * @param worker 任务
         */
        public static Builder create(String name, Worker worker) {
            return new Builder(UUID.randomUUID().toString(), name, worker, false);
        }

        /**
         * @param name 任务名称
         */
        public static Builder create(String name) {
            return new Builder(UUID.randomUUID().toString(), name, null, false);
        }

        public static Builder create() {
            return new Builder(UUID.randomUUID().toString(), null, null, false);
        }

        /**
         * 基于现有的 WorkerNode 创建新的 Builder(继承现有的 WorkerNode 配置，除了ID属性)
         *
         * @param workerNode 任务节点
         */
        public static Builder mutate(WorkerNode workerNode) {
            Assert.notNull(workerNode, "参数 workerNode 不能为空");
            Builder builder = create(workerNode.name, workerNode.worker);
            builder.param.getVariables().putAll(workerNode.param.getVariables());
            builder.worker = workerNode.worker;
            builder.callbacks.addAll(workerNode.callbacks);
            builder.ignoreErr = workerNode.ignoreErr;
            for (PrevWorker prevWorker : workerNode.prevWorkers) {
                WorkerNode prev = prevWorker.getPrev();
                NextWorker prevNext = prev.nextWorkers.stream().filter(next -> Objects.equals(next.getNext(), workerNode)).findFirst().orElse(null);
                if (prevNext != null) {
                    log.warn("WorkerNode(id={}, name={}) 与 prev WorkerNode(id={}, name={}) 缺少 next 关系", workerNode.id, workerNode.name, prev.id, prev.name);
                }
                builder.addPrev(prev, prevWorker.isWaitComplete(), prevNext != null && prevNext.isCanSkip());
            }
            for (NextWorker nextWorker : workerNode.nextWorkers) {
                WorkerNode next = nextWorker.getNext();
                PrevWorker nextPrev = next.prevWorkers.stream().filter(prev -> Objects.equals(prev.getPrev(), workerNode)).findFirst().orElse(null);
                if (nextPrev != null) {
                    log.warn("WorkerNode(id={}, name={}) 与 next WorkerNode(id={}, name={}) 缺少 prev 关系", workerNode.id, workerNode.name, next.id, next.name);
                }
                builder.addNext(next, nextPrev == null || nextPrev.isWaitComplete(), nextWorker.isCanSkip());
            }
            return builder;
        }

        public WorkerNode build() {
            final List<PrevWorker> prevWorkers = new ArrayList<>();
            final List<NextWorker> nextWorkers = new ArrayList<>();
            WorkerNode workerNode = new WorkerNode(id, name, param, worker, callbacks, ignoreErr, prevWorkers, nextWorkers);
            for (PrevOrNextWorker prevOrNextWorker : prevOrNextWorkers) {
                if (prevOrNextWorker.prev) {
                    workerNode.addPrev(prevOrNextWorker.prevOrNext, prevOrNextWorker.waitComplete, prevOrNextWorker.canSkip, prevOrNextWorker.updateIfExists);
                } else {
                    workerNode.addNext(prevOrNextWorker.prevOrNext, prevOrNextWorker.waitComplete, prevOrNextWorker.canSkip, prevOrNextWorker.updateIfExists);
                }
            }
            return workerNode;
        }

        /**
         * WorkerNode唯一ID
         */
        public Builder setId(String id) {
            Assert.isNotBlank(id, "参数 id 不能为空");
            this.id = id;
            return this;
        }

        /**
         * 任务名称
         */
        public Builder setName(String name) {
            Assert.isNotBlank(name, "参数 name 不能为空");
            this.name = name;
            return this;
        }

        /**
         * 设置任务参数
         *
         * @param name  参数名
         * @param value 参数值
         */
        public Builder setParam(String name, Object value) {
            param.set(name, value);
            return this;
        }

        /**
         * 设置默认的任务参数
         *
         * @param value 参数值
         */
        public Builder setDefParam(Object value) {
            param.def(value);
            return this;
        }

        /**
         * 删除指定的任务参数
         *
         * @param names 参数名
         */
        public Builder removeParam(String... names) {
            param.remove(names);
            return this;
        }

        /**
         * 忽略错误(当前任务节点执行错误不影响下一个任务执行)
         */
        public Builder ignoreErr(boolean ignoreErr) {
            this.ignoreErr = ignoreErr;
            return this;
        }

        /**
         * 设置任务
         */
        public Builder worker(Worker worker) {
            Assert.notNull(worker, "参数 worker 不能为空");
            this.worker = worker;
            return this;
        }

        /**
         * 设置任务
         */
        public Builder worker(Consumer<WorkerContext> worker) {
            return worker(toWorker(worker));
        }

        /**
         * 设置任务
         */
        public Builder worker(BiConsumer<WorkerVariable, WorkerContext> worker) {
            return worker(toWorker(worker));
        }

        /**
         * 设置任务
         */
        public Builder worker(Function<WorkerContext, Object> worker) {
            return worker(toWorker(worker));
        }

        /**
         * 设置任务
         */
        public Builder worker(BiFunction<WorkerVariable, WorkerContext, Object> worker) {
            return worker(toWorker(worker));
        }

        /**
         * 新增任务回调
         */
        public Builder addCallback(Callback... callbacks) {
            if (callbacks != null) {
                this.callbacks.addAll(Arrays.asList(callbacks));
            }
            return this;
        }

        /**
         * 删除任务回调
         */
        public Builder removeCallback(Callback... callbacks) {
            if (callbacks != null) {
                this.callbacks.removeAll(Arrays.asList(callbacks));
            }
            return this;
        }

        /**
         * 新增一个前置的任务节点。<br/>
         * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
         *
         * @param prev           之前的任务节点
         * @param waitComplete   是否必须等待 prev 任务执行完成，才能继续执行当前任务
         * @param canSkip        如果当前任务已经执行完成，能否跳过 prev 任务执行
         * @param updateIfExists 如果 prev 任务已经在前置的任务节点中，是否直接更新配置
         */
        public Builder addPrev(WorkerNode prev, boolean waitComplete, boolean canSkip, boolean updateIfExists) {
            Assert.notNull(prev, "参数 prev 不能为空");
            prevOrNextWorkers.add(new PrevOrNextWorker(true, prev, waitComplete, canSkip, updateIfExists));
            return this;
        }

        /**
         * 新增一个前置的任务节点，对于同一个prev，如果调用多次，以最后一次调用为准。<br/>
         * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
         *
         * @param prev         之前的任务节点
         * @param waitComplete 是否必须等待 prev 任务执行完成，才能继续执行当前任务
         * @param canSkip      如果当前任务已经执行完成，能否跳过 prev 任务执行
         */
        public Builder addPrev(WorkerNode prev, boolean waitComplete, boolean canSkip) {
            return addPrev(prev, waitComplete, canSkip, true);
        }

        /**
         * 新增一个前置的任务节点，对于同一个prev，如果调用多次，以最后一次调用为准。<br/>
         * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
         *
         * @param prev 之前的任务节点
         */
        public Builder addPrev(WorkerNode... prev) {
            if (prev != null) {
                for (WorkerNode worker : prev) {
                    addPrev(worker, true, false, true);
                }
            }
            return this;
        }

        /**
         * 删除前置的任务节点
         *
         * @param prev 之前的任务节点
         */
        public Builder removePrev(WorkerNode prev) {
            prevOrNextWorkers.removeIf(worker -> worker.prev && Objects.equals(worker.prevOrNext, prev));
            return this;
        }

        /**
         * 新增一个后续的任务节点。<br/>
         * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
         *
         * @param next           之后的任务节点
         * @param waitComplete   是否必须等待当前任务执行完成，才能继续执行 next 任务
         * @param canSkip        如果 next 任务已经执行完成，能否跳过当前任务执行
         * @param updateIfExists 如果 next 任务已经在后续的任务节点中，是否直接更新配置
         */
        public Builder addNext(WorkerNode next, boolean waitComplete, boolean canSkip, boolean updateIfExists) {
            Assert.notNull(next, "参数 next 不能为空");
            prevOrNextWorkers.add(new PrevOrNextWorker(false, next, waitComplete, canSkip, updateIfExists));
            return this;
        }

        /**
         * 新增一个后续的任务节点，对于同一个next，如果调用多次，以最后一次调用为准。<br/>
         * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
         *
         * @param next         之后的任务节点
         * @param waitComplete 是否必须等待当前任务执行完成，才能继续执行 next 任务
         * @param canSkip      如果 next 任务已经执行完成，能否跳过当前任务执行
         */
        public Builder addNext(WorkerNode next, boolean waitComplete, boolean canSkip) {
            return addNext(next, waitComplete, canSkip, true);
        }

        /**
         * 新增一个后续的任务节点，对于同一个next，如果调用多次，以最后一次调用为准。<br/>
         * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
         *
         * @param next 之后的任务节点
         */
        public Builder addNext(WorkerNode... next) {
            if (next != null) {
                for (WorkerNode worker : next) {
                    addNext(worker, true, false, true);
                }
            }
            return this;
        }

        /**
         * 删除后续的任务节点
         *
         * @param next 之后的任务节点
         */
        public Builder removeNext(WorkerNode next) {
            prevOrNextWorkers.removeIf(worker -> !worker.prev && Objects.equals(worker.prevOrNext, next));
            return this;
        }

        private static Worker toWorker(Consumer<WorkerContext> worker) {
            Assert.notNull(worker, "参数 worker 不能为空");
            return (from, current, context) -> {
                worker.accept(context);
                return null;
            };
        }

        private static Worker toWorker(BiConsumer<WorkerVariable, WorkerContext> worker) {
            Assert.notNull(worker, "参数 worker 不能为空");
            return (from, current, context) -> {
                worker.accept(current.param, context);
                return null;
            };
        }

        private static Worker toWorker(Function<WorkerContext, Object> worker) {
            Assert.notNull(worker, "参数 worker 不能为空");
            return (from, current, context) -> worker.apply(context);
        }

        private static Worker toWorker(BiFunction<WorkerVariable, WorkerContext, Object> worker) {
            Assert.notNull(worker, "参数 worker 不能为空");
            return (from, current, context) -> worker.apply(current.param, context);
        }
    }
}
