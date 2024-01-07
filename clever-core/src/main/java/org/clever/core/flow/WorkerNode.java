package org.clever.core.flow;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.OrderComparator;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.tuples.TupleTwo;
import org.clever.util.Assert;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 任务节点定义
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:23 <br/>
 */
@Slf4j
@EqualsAndHashCode
public class WorkerNode {
    private static final AtomicIntegerFieldUpdater<WorkerNode> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(WorkerNode.class, "state");

    /**
     * 任务唯一ID
     */
    @Getter
    private final String id = UUID.randomUUID().toString();
    /**
     * 任务名称
     */
    @Getter
    private String name;
    /**
     * 任务参数
     */
    @Getter
    private final WorkerParam param = new WorkerParam();
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
    private boolean ignoreErr = false;
    /**
     * 当前任务节点之前的任务节点(指向当前任务节点的任务节点)
     */
    private final List<PrevWorker> prevWorkers = new ArrayList<>();
    /**
     * 当前任务节点之后的任务节点(当前任务节点指向的任务节点)
     */
    private final List<NextWorker> nextWorkers = new ArrayList<>();
    /**
     * 节点任务运行状态，参照 {@link WorkerState}
     */
    private volatile int state = WorkerState.INIT;

    /**
     * 应该使用 {@link Builder} 构建 {@link WorkerNode} 实例
     *
     * @param name      任务名称
     * @param worker    任务
     * @param callbacks 任务回调
     * @param ignoreErr 忽略错误(当前任务节点执行错误不影响下一个任务执行)
     */
    public WorkerNode(String name, Worker worker, List<Callback> callbacks, boolean ignoreErr) {
        Assert.isNotBlank(name, "参数 name 不能为空");
        Assert.notNull(worker, "参数 worker 不能为 null");
        Assert.notNull(callbacks, "参数 callbacks 不能为 null");
        OrderComparator.sort(callbacks);
        this.name = name;
        this.worker = worker;
        this.callbacks = callbacks;
        this.ignoreErr = ignoreErr;
    }

    /**
     * 任务名称
     */
    public WorkerNode setName(String name) {
        Assert.isNotBlank(name, "参数 name 不能为空");
        this.name = name;
        return this;
    }

    /**
     * 忽略错误(当前任务节点执行错误不影响下一个任务执行)
     */
    public WorkerNode setIgnoreErr(boolean ignoreErr) {
        this.ignoreErr = ignoreErr;
        return this;
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
     * 新增一个前置的任务节点。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param prev           之前的任务节点
     * @param waitComplete   是否必须等待 prev 任务执行完成，才能继续执行当前任务
     * @param canSkip        如果当前任务已经执行完成，能否跳过 prev 任务执行
     * @param updateIfExists 如果 prev 任务已经在前置的任务节点中，是否直接更新配置
     */
    public WorkerNode addPrev(WorkerNode prev, boolean waitComplete, boolean canSkip, boolean updateIfExists) {
        Assert.notNull(prev, "参数 prev 不能为空");
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
    public WorkerNode prev(WorkerNode prev, boolean waitComplete, boolean canSkip) {
        return addPrev(prev, waitComplete, canSkip, true);
    }

    /**
     * 新增一个前置的任务节点，对于同一个prev，如果调用多次，以最后一次调用为准。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param prev 之前的任务节点
     */
    public WorkerNode prev(WorkerNode prev) {
        return addPrev(prev, true, false, true);
    }

    /**
     * 删除前置的任务节点
     *
     * @param prev 之前的任务节点
     */
    public WorkerNode removePrev(WorkerNode prev) {
        // TODO 删除前置的任务节点
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
    public WorkerNode addNext(WorkerNode next, boolean waitComplete, boolean canSkip, boolean updateIfExists) {
        Assert.notNull(next, "参数 next 不能为空");
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
    public WorkerNode next(WorkerNode next, boolean waitComplete, boolean canSkip) {
        return addNext(next, waitComplete, canSkip, true);
    }

    /**
     * 新增一个后续的任务节点，对于同一个next，如果调用多次，以最后一次调用为准。<br/>
     * 建议统一使用next(或统一使用prev)构建任务节点链，便于理解，效果是一致的
     *
     * @param next 之后的任务节点
     */
    public WorkerNode next(WorkerNode next) {
        return addNext(next, true, false, true);
    }

    /**
     * 删除后续的任务节点
     *
     * @param next 之后的任务节点
     */
    public WorkerNode removeNext(WorkerNode next) {
        // TODO 删除后续的任务节点
        return this;
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
     * 开始执行当前任务节点及其后续的任务节点。<br/>
     * 可重复调用，但只有第一次调用起作用。
     *
     * @param workerContext   任务上下文
     * @param from            发起执行当前任务的任务节点(执行入口任务时为null)
     * @param executorService 任务执行器(线程池)
     */
    public void start(WorkerContext workerContext, WorkerNode from, ExecutorService executorService) {
        Assert.notNull(workerContext, "参数 workerContext 不能为空");
        Assert.notNull(executorService, "参数 executorService 不能为空");
        // 当前任务已经执行完成
        if (isCompleted()) {
            return;
        }
        // 是否必须等待 prev 任务执行完成
        if (prevWorkers.stream().anyMatch(prev -> prev.isWaitComplete() && prev.getPrev().notCompleted())) {
            return;
        }
        // 如果 next 任务已经执行完成，能否跳过 current 任务执行
        if (nextWorkers.stream().allMatch(next -> next.isCanSkip() && next.getNext().isCompleted())) {
            setState(WorkerState.SKIPPED);
            return;
        }
        // 当前任务已经在执行过了
        if (!setState(WorkerState.RUNNING, WorkerState.INIT)) {
            return;
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
        for (NextWorker nextWorker : nextWorkers) {
            CompletableFuture.runAsync(() -> nextWorker.getNext().start(workerContext, this, executorService), executorService);
        }
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
                result = worker.execute(param, this, from, workerContext);
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

    /**
     * 用于创建 WorkerNode 实例
     */
    public static class Builder {

    }
}
