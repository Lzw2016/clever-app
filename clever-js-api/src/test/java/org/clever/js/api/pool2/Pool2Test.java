package org.clever.js.api.pool2;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/23 21:57 <br/>
 */
@Slf4j
public class Pool2Test {

    private ObjectPool<TupleOne<Long>> pool;

    @BeforeEach
    public void init() {
        // 创建对象池配置
        GenericObjectPoolConfig<TupleOne<Long>> config = new GenericObjectPoolConfig<>();
        config.setMaxWait(Duration.ofMillis(-1));
        config.setMaxTotal(32);
        config.setMinIdle(8);
        // 创建对象工厂
        PooledObjectFactory<TupleOne<Long>> factory = new TupleOneFactory();
        // 创建对象池
        pool = new GenericObjectPool<>(factory, config);
    }

    @AfterEach
    public void close() {
        pool.close();
    }

    public void usePool(int count) {
        for (int i = 0; i < count; i++) {
            TupleOne<Long> tupleOne = null;
            try {
                // 从池中获取对象
                tupleOne = pool.borrowObject();
                // 使用对象
                tupleOne.setValue(tupleOne.getValue() + 1);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (tupleOne != null) {
                    try {
                        // 出现错误将对象置为失效
                        pool.invalidateObject(tupleOne);
                        // 避免 invalidate 之后再 return 抛异常
                        tupleOne = null;
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            } finally {
                try {
                    if (null != tupleOne) {
                        // 使用完后必须 returnObject
                        pool.returnObject(tupleOne);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @SneakyThrows
    @Test
    public void t01() {
        final int threadCount = 10000;
        final Semaphore semaphore = new Semaphore(threadCount);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(256, 256, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(102400));
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            semaphore.acquire();
            executor.execute(() -> {
                usePool(1000);
                semaphore.release();
                // log.info("# --> release");
            });
        }
        semaphore.acquire(threadCount);
        final long endTime = System.currentTimeMillis();
        long sum = 0;
        for (TupleOne<Long> tupleOne : TupleOneFactory.ALL_OBJ) {
            sum += tupleOne.getValue();
        }
        log.info("# --> 耗时: {}ms | sum -> {}", endTime - startTime, sum); // 耗时: 19988ms | sum -> 10000000
        // synchronized 耗时: 2102ms | sum -> 10000000
    }
}

@Data
class TupleOne<T> {
    private T value;

    public TupleOne(T value) {
        this.value = value;
    }
}

@Slf4j
class TupleOneFactory extends BasePooledObjectFactory<TupleOne<Long>> {
    public final static List<TupleOne<Long>> ALL_OBJ = new CopyOnWriteArrayList<>();

    // 创建一个新的对象
    @Override
    public TupleOne<Long> create() {
        // log.info("# create");
        TupleOne<Long> tupleOne = new TupleOne<>(0L);
        ALL_OBJ.add(tupleOne);
        return tupleOne;
    }

    // 封装为池化对象
    @Override
    public PooledObject<TupleOne<Long>> wrap(TupleOne<Long> tupleOne) {
        return new DefaultPooledObject<>(tupleOne);
    }

    // 使用完返还对象
    @Override
    public void passivateObject(PooledObject<TupleOne<Long>> pooledObject) {
        // log.info("# passivateObject");
    }
}
