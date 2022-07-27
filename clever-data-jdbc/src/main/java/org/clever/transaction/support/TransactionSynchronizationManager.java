package org.clever.transaction.support;

import org.clever.core.NamedThreadLocal;
import org.clever.core.OrderComparator;
import org.clever.util.Assert;

import java.util.*;

/**
 * 管理每个线程的资源和事务同步的中心委托。由资源管理代码使用，但不由典型应用程序代码使用。
 *
 * <p>支持每个key一个资源而不覆盖，也就是说，在为同一key设置新资源之前，需要删除一个资源。
 * 如果同步处于活动状态，则支持事务同步列表。
 *
 * <p>资源管理代码应该通过{@code getResource}检查线程绑定的资源，例如JDBC连接或Hibernate会话。
 * 这种代码通常不应该将资源绑定到线程，因为这是事务管理器的责任。
 * 另一种选择是，如果事务同步处于活动状态，则在首次使用时延迟绑定，以执行跨任意数量资源的事务。
 *
 * <p>事务管理器必须通过{@link #initSynchronization()}和{@link #clearSynchronization()}激活和停用事务同步。
 * {@link AbstractPlatformTransactionManager}自动支持这一点，
 * 因此所有标准的事务管理器(如{@link org.clever.jdbc.datasource.DataSourceTransactionManager})都支持这一点。
 *
 * <p>资源管理代码应仅在该管理器处于活动状态时注册同步，可通过{@link #isSynchronizationActive}进行检查；它应该立即执行资源清理。
 * 如果事务同步未处于活动状态，则表示当前没有事务，或者事务管理器不支持事务同步。
 *
 * <p>例如，同步用于始终在JTA事务中返回相同的资源，例如，对于任何给定的数据源或SessionFactory，分别返回JDBC连接或Hibernate会话。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:32 <br/>
 *
 * @see #isSynchronizationActive
 * @see #registerSynchronization
 * @see TransactionSynchronization
 * @see AbstractPlatformTransactionManager#setTransactionSynchronization
 * @see org.clever.jdbc.datasource.DataSourceTransactionManager
 * @see org.clever.jdbc.datasource.DataSourceUtils#getConnection
 */
public abstract class TransactionSynchronizationManager {
    private static final ThreadLocal<Map<Object, Object>> resources = new NamedThreadLocal<>("Transactional resources");
    private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations = new NamedThreadLocal<>("Transaction synchronizations");
    private static final ThreadLocal<String> currentTransactionName = new NamedThreadLocal<>("Current transaction name");
    private static final ThreadLocal<Boolean> currentTransactionReadOnly = new NamedThreadLocal<>("Current transaction read-only status");
    private static final ThreadLocal<Integer> currentTransactionIsolationLevel = new NamedThreadLocal<>("Current transaction isolation level");
    private static final ThreadLocal<Boolean> actualTransactionActive = new NamedThreadLocal<>("Actual transaction active");

    //-------------------------------------------------------------------------
    // Management of transaction-associated resource handles
    //-------------------------------------------------------------------------

    /**
     * 返回绑定到当前线程的所有资源。主要用于调试目的。
     * 资源管理器应该始终为他们感兴趣的特定资源键调用{@code hasResource}
     *
     * @see #hasResource
     */
    public static Map<Object, Object> getResourceMap() {
        Map<Object, Object> map = resources.get();
        return (map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap());
    }

    /**
     * 检查给定key是否有绑定到当前线程的资源。
     *
     * @param key 要检查的键（通常是资源工厂）
     * @return 如果有值绑定到当前线程
     * @see ResourceTransactionManager#getResourceFactory()
     */
    public static boolean hasResource(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Object value = doGetResource(actualKey);
        return (value != null);
    }

    /**
     * 检索绑定到当前线程的给定key的资源。
     *
     * @param key 要检查的键（通常是资源工厂）
     * @return 绑定到当前线程（通常是活动资源对象）的值，如果没有，则为null
     * @see ResourceTransactionManager#getResourceFactory()
     */
    public static Object getResource(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        return doGetResource(actualKey);
    }

    /**
     * 实际检查为给定键绑定的资源的值。
     */
    private static Object doGetResource(Object actualKey) {
        Map<Object, Object> map = resources.get();
        if (map == null) {
            return null;
        }
        Object value = map.get(actualKey);
        // Transparently remove ResourceHolder that was marked as void...
        if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
            map.remove(actualKey);
            // Remove entire ThreadLocal if empty...
            if (map.isEmpty()) {
                resources.remove();
            }
            value = null;
        }
        return value;
    }

    /**
     * 将给定key的给定资源绑定到当前线程。
     *
     * @param key   将值绑定到的键（通常是资源工厂）
     * @param value 要绑定的值（通常是活动资源对象）
     * @throws IllegalStateException 如果已经有值绑定到线程
     * @see ResourceTransactionManager#getResourceFactory()
     */
    public static void bindResource(Object key, Object value) throws IllegalStateException {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Assert.notNull(value, "Value must not be null");
        Map<Object, Object> map = resources.get();
        // set ThreadLocal Map if none found
        if (map == null) {
            map = new HashMap<>();
            resources.set(map);
        }
        Object oldValue = map.put(actualKey, value);
        // Transparently suppress a ResourceHolder that was marked as void...
        if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
            oldValue = null;
        }
        if (oldValue != null) {
            throw new IllegalStateException(
                    "Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread"
            );
        }
    }

    /**
     * 从当前线程取消绑定给定key的资源。
     *
     * @param key 解除绑定的键（通常是资源工厂）
     * @return 以前绑定的值（通常是活动资源对象）
     * @throws IllegalStateException 如果没有值绑定到线程
     * @see ResourceTransactionManager#getResourceFactory()
     */
    public static Object unbindResource(Object key) throws IllegalStateException {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        Object value = doUnbindResource(actualKey);
        if (value == null) {
            throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread");
        }
        return value;
    }

    /**
     * 从当前线程取消绑定给定key的资源。
     *
     * @param key 解除绑定的键（通常是资源工厂）
     * @return 以前绑定的值，如果未绑定，则为null
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Object unbindResourceIfPossible(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        return doUnbindResource(actualKey);
    }

    /**
     * 实际删除为给定键绑定的资源的值。
     */
    private static Object doUnbindResource(Object actualKey) {
        Map<Object, Object> map = resources.get();
        if (map == null) {
            return null;
        }
        Object value = map.remove(actualKey);
        // Remove entire ThreadLocal if empty...
        if (map.isEmpty()) {
            resources.remove();
        }
        // Transparently suppress a ResourceHolder that was marked as void...
        if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
            value = null;
        }
        return value;
    }

    //-------------------------------------------------------------------------
    // Management of transaction synchronizations
    //-------------------------------------------------------------------------

    /**
     * 如果当前线程的事务同步处于活动状态，则返回。
     * 可以在注册之前调用，以避免不必要的实例创建。
     *
     * @see #registerSynchronization
     */
    public static boolean isSynchronizationActive() {
        return (synchronizations.get() != null);
    }

    /**
     * 激活当前线程的事务同步。事务管理器在事务开始时调用。
     *
     * @throws IllegalStateException 如果同步已激活
     */
    public static void initSynchronization() throws IllegalStateException {
        if (isSynchronizationActive()) {
            throw new IllegalStateException("Cannot activate transaction synchronization - already active");
        }
        synchronizations.set(new LinkedHashSet<>());
    }

    /**
     * 为当前线程注册新的事务同步。通常由资源管理代码调用。
     * <p>请注意，同步可以实现{@link org.clever.core.Ordered}接口
     * 它们将根据其订单值（如果有）按订单执行。
     *
     * @param synchronization 要注册的同步对象
     * @throws IllegalStateException 如果事务同步未激活
     * @see org.clever.core.Ordered
     */
    public static void registerSynchronization(TransactionSynchronization synchronization) throws IllegalStateException {
        Assert.notNull(synchronization, "TransactionSynchronization must not be null");
        Set<TransactionSynchronization> synchs = synchronizations.get();
        if (synchs == null) {
            throw new IllegalStateException("Transaction synchronization is not active");
        }
        synchs.add(synchronization);
    }

    /**
     * 返回当前线程所有已注册同步的不可修改快照列表。
     *
     * @return 事务同步实例的不可修改列表
     * @throws IllegalStateException 如果同步未激活
     * @see TransactionSynchronization
     */
    public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
        Set<TransactionSynchronization> synchs = synchronizations.get();
        if (synchs == null) {
            throw new IllegalStateException("Transaction synchronization is not active");
        }
        // Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
        // while iterating and invoking synchronization callbacks that in turn
        // might register further synchronizations.
        if (synchs.isEmpty()) {
            return Collections.emptyList();
        } else {
            // Sort lazily here, not in registerSynchronization.
            List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
            OrderComparator.sort(sortedSynchs);
            return Collections.unmodifiableList(sortedSynchs);
        }
    }

    /**
     * 停用当前线程的事务同步。事务管理器在事务清理时调用。
     *
     * @throws IllegalStateException 如果同步未激活
     */
    public static void clearSynchronization() throws IllegalStateException {
        if (!isSynchronizationActive()) {
            throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
        }
        synchronizations.remove();
    }

    //-------------------------------------------------------------------------
    // Exposure of transaction characteristics
    //-------------------------------------------------------------------------

    /**
     * 设置当前事务的名称（如果有）。
     * 事务管理器在事务开始和清理时调用。
     *
     * @param name 事务的名称，或为null以重置它
     * @see org.clever.transaction.TransactionDefinition#getName()
     */
    public static void setCurrentTransactionName(String name) {
        currentTransactionName.set(name);
    }

    /**
     * 返回当前事务的名称，如果未设置，则返回null。
     * 由资源管理代码调用，以优化每个用例，例如优化特定命名事务的获取策略。
     *
     * @see org.clever.transaction.TransactionDefinition#getName()
     */
    public static String getCurrentTransactionName() {
        return currentTransactionName.get();
    }

    /**
     * 为当前事务设置只读标志。事务管理器在事务开始和清理时调用。
     *
     * @param readOnly true将当前事务标记为只读；false重置这样的只读标记
     * @see org.clever.transaction.TransactionDefinition#isReadOnly()
     */
    public static void setCurrentTransactionReadOnly(boolean readOnly) {
        currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
    }

    /**
     * 返回当前事务是否标记为只读。准备新创建的资源（例如，Hibernate会话）时由资源管理代码调用。
     * 请注意，事务同步接收只读标志作为{@code beforeCommit}回调的参数，以便能够抑制提交时的更改检测。
     * 本方法旨在用于早期的只读检查，例如预先将Hibernate会话的刷新模式设置为“FlushMode.MANUAL”。
     *
     * @see org.clever.transaction.TransactionDefinition#isReadOnly()
     * @see TransactionSynchronization#beforeCommit(boolean)
     */
    public static boolean isCurrentTransactionReadOnly() {
        return (currentTransactionReadOnly.get() != null);
    }

    /**
     * 设置当前事务的隔离级别。事务管理器在事务开始和清理时调用。
     *
     * @param isolationLevel 根据JDBC连接常量（相当于相应TransactionDefinition常量），要设置的隔离级别，或为null以重置它
     * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
     * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
     * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
     * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
     * @see org.clever.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
     * @see org.clever.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
     * @see org.clever.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
     * @see org.clever.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
     * @see org.clever.transaction.TransactionDefinition#getIsolationLevel()
     */
    public static void setCurrentTransactionIsolationLevel(Integer isolationLevel) {
        currentTransactionIsolationLevel.set(isolationLevel);
    }

    /**
     * 返回当前事务的隔离级别（如果有）。准备新创建的资源（例如，JDBC连接）时由资源管理代码调用。
     *
     * @return 当前的隔离级别，根据JDBC连接常量（相当于相应的TransactionDefinition常量），如果没有，则为null
     * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
     * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
     * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
     * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
     * @see org.clever.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
     * @see org.clever.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
     * @see org.clever.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
     * @see org.clever.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
     * @see org.clever.transaction.TransactionDefinition#getIsolationLevel()
     */
    public static Integer getCurrentTransactionIsolationLevel() {
        return currentTransactionIsolationLevel.get();
    }

    /**
     * 设置当前是否有实际事务处于活动状态。事务管理器在事务开始和清理时调用。
     *
     * @param active true将当前线程标记为与实际事务关联；false重置该标记
     */
    public static void setActualTransactionActive(boolean active) {
        actualTransactionActive.set(active ? Boolean.TRUE : null);
    }

    /**
     * 返回当前是否有活动的实际事务。这指示当前线程是否与实际事务关联，而不仅仅与活动事务同步关联。
     * <p>由资源管理代码调用，该代码希望区分活动事务同步（有或没有支持资源事务；也支持PROPAGATION_SUPPORTS）
     * 和活动的实际事务（有支持资源事务；需要PROPAGATION_REQUIRED，PROPAGATION_REQUIRES_NEW 等）。
     *
     * @see #isSynchronizationActive()
     */
    public static boolean isActualTransactionActive() {
        return (actualTransactionActive.get() != null);
    }

    /**
     * 清除当前线程的整个事务同步状态：注册的同步以及各种事务特征。
     *
     * @see #clearSynchronization()
     * @see #setCurrentTransactionName
     * @see #setCurrentTransactionReadOnly
     * @see #setCurrentTransactionIsolationLevel
     * @see #setActualTransactionActive
     */
    public static void clear() {
        synchronizations.remove();
        currentTransactionName.remove();
        currentTransactionReadOnly.remove();
        currentTransactionIsolationLevel.remove();
        actualTransactionActive.remove();
    }
}
