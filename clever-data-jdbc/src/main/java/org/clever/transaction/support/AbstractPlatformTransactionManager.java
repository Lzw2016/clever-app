package org.clever.transaction.support;

import org.clever.core.Constants;
import org.clever.transaction.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

/**
 * 实现clever标准事务工作流的抽象基类，作为JtaTransactionManager等具体平台事务管理器的基础。
 * <p>此基类提供以下工作流处理：
 * <ul>
 * <li>确定是否存在现有事务；
 * <li>应用适当的传播行为；
 * <li>必要时暂停和恢复事务；
 * <li>在提交时检查仅回滚标志；
 * <li>对回滚应用适当的修改（仅限实际回滚或设置回滚）；
 * <li>触发已注册的同步回调（如果事务同步处于活动状态）。
 * </ul>
 *
 * <p>子类必须为事务的特定状态实现特定的模板方法，例如：begin, suspend, resume, commit, rollback。
 * 其中最重要的是抽象的，必须通过具体的实现来提供；对于其余部分，提供了默认值，因此重写是可选的。
 *
 * <p>事务同步是一种通用机制，用于注册在事务完成时调用的回调。
 * 在JTA事务中运行时，这主要由JDBC、Hibernate、JPA等的数据访问支持类内部使用：
 * 它们注册在事务中打开的资源，以便在事务完成时关闭，例如允许在事务中重用相同的Hibernate会话。
 * 同样的机制也可以用于应用程序中的自定义同步需求。
 *
 * <p>此类的状态是可序列化的，以允许序列化事务策略以及携带事务拦截器的代理。如果子类希望使其状态也可序列化，则由它们自己决定。
 * 他们应该实现{@code java.io.Serializable}接口在这种情况下是可序列化的标记接口，
 * 如果需要恢复任何瞬态，则可能是一个私有{@code readObject()}方法（根据Java序列化规则）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:22 <br/>
 *
 * @see #setTransactionSynchronization
 * @see TransactionSynchronizationManager
 */
public abstract class AbstractPlatformTransactionManager implements PlatformTransactionManager, Serializable {
    /**
     * 始终激活事务同步，即使对于由于没有现有后端事务的PROPAGATION_SUPPORTS而导致的“empty”事务也是如此。
     *
     * @see TransactionDefinition#PROPAGATION_SUPPORTS
     * @see TransactionDefinition#PROPAGATION_NOT_SUPPORTED
     * @see TransactionDefinition#PROPAGATION_NEVER
     */
    public static final int SYNCHRONIZATION_ALWAYS = 0;
    /**
     * 仅为实际事务激活事务同步，也就是说，不为因没有现有后端事务的PROPAGATION_SUPPORTS而导致的空事务激活事务同步。
     *
     * @see TransactionDefinition#PROPAGATION_REQUIRED
     * @see TransactionDefinition#PROPAGATION_MANDATORY
     * @see TransactionDefinition#PROPAGATION_REQUIRES_NEW
     */
    public static final int SYNCHRONIZATION_ON_ACTUAL_TRANSACTION = 1;
    /**
     * 从不进行活动的事务同步，即使对于实际事务也是如此。
     */
    public static final int SYNCHRONIZATION_NEVER = 2;
    /**
     * AbstractPlatformTransactionManager的常量实例。
     */
    private static final Constants constants = new Constants(AbstractPlatformTransactionManager.class);

    protected transient Logger logger = LoggerFactory.getLogger(getClass());
    private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;
    private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;
    private boolean nestedTransactionAllowed = false;
    private boolean validateExistingTransaction = false;
    private boolean globalRollbackOnParticipationFailure = true;
    private boolean failEarlyOnGlobalRollbackOnly = false;
    private boolean rollbackOnCommitFailure = false;

    /**
     * 使用此类中相应常量的名称设置事务同步，例如“SYNCHRONIZATION_ALWAYS”。
     *
     * @param constantName 常量的名称
     * @see #SYNCHRONIZATION_ALWAYS
     */
    public final void setTransactionSynchronizationName(String constantName) {
        setTransactionSynchronization(constants.asNumber(constantName).intValue());
    }

    /**
     * 设置此事务管理器何时应激活线程绑定事务同步支持。默认值为 "always".
     * <p>请注意，不同事务管理器不支持多个并发事务的事务同步。
     * 任何时候都只允许一个事务管理器激活它。
     *
     * @see #SYNCHRONIZATION_ALWAYS
     * @see #SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
     * @see #SYNCHRONIZATION_NEVER
     * @see TransactionSynchronizationManager
     * @see TransactionSynchronization
     */
    public final void setTransactionSynchronization(int transactionSynchronization) {
        this.transactionSynchronization = transactionSynchronization;
    }

    /**
     * 返回此事务管理器是否应激活线程绑定事务同步支持
     */
    public final int getTransactionSynchronization() {
        return this.transactionSynchronization;
    }

    /**
     * 指定如果在事务级别未指定超时，则此事务管理器应应用的默认超时（秒）。
     * <p>默认值是基础事务基础结构的默认超时，例如，对于JTA提供程序，通常为30秒，
     * 由{@code TransactionDefinition.TIMEOUT_DEFAULT}值。
     *
     * @see TransactionDefinition#TIMEOUT_DEFAULT
     */
    public final void setDefaultTimeout(int defaultTimeout) {
        if (defaultTimeout < TransactionDefinition.TIMEOUT_DEFAULT) {
            throw new InvalidTimeoutException("Invalid default timeout", defaultTimeout);
        }
        this.defaultTimeout = defaultTimeout;
    }

    /**
     * 返回此事务管理器在事务级别未指定超时时应应用的默认超时（秒）。
     * <p>返回{@code TransactionDefinition.TIMEOUT_DEFAULT}指示基础事务基础结构的默认超时
     */
    public final int getDefaultTimeout() {
        return this.defaultTimeout;
    }

    /**
     * 设置是否允许嵌套事务。默认值为“false”。
     * <p>通常由具体事务管理器子类使用适当的默认值初始化。
     */
    public final void setNestedTransactionAllowed(boolean nestedTransactionAllowed) {
        this.nestedTransactionAllowed = nestedTransactionAllowed;
    }

    /**
     * 返回是否允许嵌套事务。
     */
    public final boolean isNestedTransactionAllowed() {
        return this.nestedTransactionAllowed;
    }

    /**
     * 设置是否应在参与现有事务之前对其进行验证。
     * <p>当参与现有事务时（例如，需要PROPAGATION_REQUIRED或PROPAGATION_SUPPORTS遇到现有事务），
     * 此外部事务的特征甚至会应用于内部事务范围。验证将检测内部事务定义上不兼容的隔离级别和只读设置，
     * 并通过引发相应的异常相应地拒绝参与。
     * <p>默认值为“false”，轻松地忽略内部事务设置，只需使用外部事务的特性覆盖它们。
     * 将此标志切换为“true”，以强制执行严格的验证。
     */
    public final void setValidateExistingTransaction(boolean validateExistingTransaction) {
        this.validateExistingTransaction = validateExistingTransaction;
    }

    /**
     * 返回是否应在参与现有事务之前对其进行验证。
     */
    public final boolean isValidateExistingTransaction() {
        return this.validateExistingTransaction;
    }

    /**
     * 设置是否仅在参与事务失败后才将现有事务全局标记为回滚。
     * <p>默认值为“true”：如果参与的事务（例如，需要PROPAGATION_REQUIRED或PROPAGATION_SUPPORTS遇到现有事务）失败，
     * 则该事务将全局标记为仅回滚。此类事务的唯一可能结果是回滚：事务发起人无法再提交事务。
     * <p>将其切换为“false”，让事务发起人做出回滚决策。如果参与事务因异常而失败，调用方仍然可以决定继续使用事务中的其他路径。
     * 但是，请注意，只有在所有参与资源都能够继续进行事务提交的情况下，即使在数据访问失败之后，
     * 这才有效：例如，Hibernate会话通常不是这种情况；对于一系列JDBC insert/update/delete操作也是如此。
     * <p>注意：此标志仅适用于子事务的显式回滚尝试，通常由数据访问操作引发的异常引起
     * （其中TransactionInterceptor将根据回滚规则触发{@code PlatformTransactionManager.rollback()}调用）。
     * 如果该标志处于关闭状态，则调用方可以处理异常并决定回滚，而与子事务的回滚规则无关。
     * 但是，此标志不适用于{@code TransactionStatus}上的显式{@code setRollbackOnly}调用，
     * 这将始终导致最终的全局回滚（因为它可能不会在仅回滚调用之后引发异常）。
     * <p>处理子事务失败的建议解决方案是“嵌套事务”，其中全局事务可以回滚到子事务开始时的保存点。
     * PROPAGATION_NESTED正好提供了这些语义；然而，只有当嵌套事务支持可用时，它才起作用。
     * DataSourceTransactionManager就是这种情况，但JtaTransactionManager却不是这样。
     *
     * @see #setNestedTransactionAllowed
     */
    public final void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
        this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
    }

    /**
     * 返回是否仅在参与事务失败后才将现有事务全局标记为回滚。
     */
    public final boolean isGlobalRollbackOnParticipationFailure() {
        return this.globalRollbackOnParticipationFailure;
    }

    /**
     * 设置事务被全局标记为仅回滚时是否提前失败。
     * <p>默认值为“false”，只会在最外层的事务边界处导致意外的回滚异常。
     * 打开此标志可在第一次检测到仅全局回滚标记时（甚至从内部事务边界内）引发意外回滚异常。
     * <p>请注意，仅全局回滚标记的早期失败行为已经统一：默认情况下，所有事务管理器只会在最外层的事务边界引发意外回滚异常。
     * 例如，这允许在操作失败后继续单元测试，并且事务永远不会完成。
     * 只有当此标志显式设置为“true”时，所有事务管理器才会更早失败。
     *
     * @see UnexpectedRollbackException
     */
    public final void setFailEarlyOnGlobalRollbackOnly(boolean failEarlyOnGlobalRollbackOnly) {
        this.failEarlyOnGlobalRollbackOnly = failEarlyOnGlobalRollbackOnly;
    }

    /**
     * 如果事务被全局标记为仅回滚，则返回是否提前失败。
     */
    public final boolean isFailEarlyOnGlobalRollbackOnly() {
        return this.failEarlyOnGlobalRollbackOnly;
    }

    /**
     * 设置{@code doCommit}调用失败时是否应执行{@code doRollback}。
     * 通常没有必要，因此需要避免，因为它可能会用后续回滚异常覆盖提交异常。
     * <p>默认值为 "false".
     *
     * @see #doCommit
     * @see #doRollback
     */
    public final void setRollbackOnCommitFailure(boolean rollbackOnCommitFailure) {
        this.rollbackOnCommitFailure = rollbackOnCommitFailure;
    }

    /**
     * 返回{@code doCommit}调用失败时是否应执行{@code doRollback}。
     */
    public final boolean isRollbackOnCommitFailure() {
        return this.rollbackOnCommitFailure;
    }

    //---------------------------------------------------------------------
    // Implementation of PlatformTransactionManager
    //---------------------------------------------------------------------

    /**
     * 此实现处理传播行为{@code doGetTransaction}的代表{@code isExistingTransaction}和{@code doBegin}。
     *
     * @see #doGetTransaction
     * @see #isExistingTransaction
     * @see #doBegin
     */
    @Override
    public final TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        // Use defaults if no transaction definition given.
        TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
        Object transaction = doGetTransaction();
        boolean debugEnabled = logger.isDebugEnabled();
        if (isExistingTransaction(transaction)) {
            // Existing transaction found -> check propagation behavior to find out how to behave.
            return handleExistingTransaction(def, transaction, debugEnabled);
        }
        // Check definition settings for new transaction.
        if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
            throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
        }
        // No existing transaction found -> check propagation behavior to find out how to proceed.
        if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
            throw new IllegalTransactionStateException(
                    "No existing transaction found for transaction marked with propagation 'mandatory'"
            );
        } else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED
                || def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW
                || def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
            SuspendedResourcesHolder suspendedResources = suspend(null);
            if (debugEnabled) {
                logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
            }
            try {
                return startTransaction(def, transaction, debugEnabled, suspendedResources);
            } catch (RuntimeException | Error ex) {
                resume(null, suspendedResources);
                throw ex;
            }
        } else {
            // Create "empty" transaction: no actual transaction, but potentially synchronization.
            if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
                logger.warn("Custom isolation level specified but no actual transaction initiated; " +
                        "isolation level will effectively be ignored: " + def
                );
            }
            boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
            return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
        }
    }

    /**
     * 启动新事务
     */
    private TransactionStatus startTransaction(TransactionDefinition definition,
                                               Object transaction,
                                               boolean debugEnabled,
                                               SuspendedResourcesHolder suspendedResources) {
        boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
        DefaultTransactionStatus status = newTransactionStatus(
                definition, transaction, true, newSynchronization, debugEnabled, suspendedResources
        );
        doBegin(transaction, definition);
        prepareSynchronization(status, definition);
        return status;
    }

    /**
     * 为现有事务创建TransactionStatus
     */
    private TransactionStatus handleExistingTransaction(TransactionDefinition definition,
                                                        Object transaction,
                                                        boolean debugEnabled) throws TransactionException {
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
            throw new IllegalTransactionStateException(
                    "Existing transaction found for transaction marked with propagation 'never'"
            );
        }
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
            if (debugEnabled) {
                logger.debug("Suspending current transaction");
            }
            Object suspendedResources = suspend(transaction);
            boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
            return prepareTransactionStatus(
                    definition, null, false, newSynchronization, debugEnabled, suspendedResources
            );
        }
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
            if (debugEnabled) {
                logger.debug(
                        "Suspending current transaction, creating new transaction with name [" + definition.getName() + "]"
                );
            }
            SuspendedResourcesHolder suspendedResources = suspend(transaction);
            try {
                return startTransaction(definition, transaction, debugEnabled, suspendedResources);
            } catch (RuntimeException | Error beginEx) {
                resumeAfterBeginException(transaction, suspendedResources, beginEx);
                throw beginEx;
            }
        }
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
            if (!isNestedTransactionAllowed()) {
                throw new NestedTransactionNotSupportedException(
                        "Transaction manager does not allow nested transactions by default - " +
                                "specify 'nestedTransactionAllowed' property with value 'true'"
                );
            }
            if (debugEnabled) {
                logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
            }
            if (useSavepointForNestedTransaction()) {
                // Create savepoint within existing clever-managed transaction,
                // through the SavepointManager API implemented by TransactionStatus.
                // Usually uses JDBC 3.0 savepoints. Never activates clever synchronization.
                DefaultTransactionStatus status = prepareTransactionStatus(
                        definition, transaction, false, false, debugEnabled, null
                );
                status.createAndHoldSavepoint();
                return status;
            } else {
                // Nested transaction through nested begin and commit/rollback calls.
                // Usually only for JTA: clever synchronization might get activated here
                // in case of a pre-existing JTA transaction.
                return startTransaction(definition, transaction, debugEnabled, null);
            }
        }
        // Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
        if (debugEnabled) {
            logger.debug("Participating in existing transaction");
        }
        if (isValidateExistingTransaction()) {
            if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
                Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
                if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {
                    Constants isoConstants = DefaultTransactionDefinition.constants;
                    throw new IllegalTransactionStateException(
                            "Participating transaction with definition [" +
                                    definition + "] specifies isolation level which is incompatible with existing transaction: " +
                                    (currentIsolationLevel != null ? isoConstants.toCode(currentIsolationLevel, DefaultTransactionDefinition.PREFIX_ISOLATION) : "(unknown)")
                    );
                }
            }
            if (!definition.isReadOnly()) {
                if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                    throw new IllegalTransactionStateException(
                            "Participating transaction with definition [" + definition + "] is not marked as read-only but existing transaction is"
                    );
                }
            }
        }
        boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
        return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
    }

    /**
     * 为给定参数创建新的TransactionStatus，并根据需要初始化事务同步
     *
     * @see #newTransactionStatus
     * @see #prepareTransactionStatus
     */
    protected final DefaultTransactionStatus prepareTransactionStatus(TransactionDefinition definition,
                                                                      Object transaction,
                                                                      boolean newTransaction,
                                                                      boolean newSynchronization,
                                                                      boolean debug, Object suspendedResources) {
        DefaultTransactionStatus status = newTransactionStatus(
                definition, transaction, newTransaction, newSynchronization, debug, suspendedResources
        );
        prepareSynchronization(status, definition);
        return status;
    }

    /**
     * 为给定参数创建TransactionStatus实例
     */
    protected DefaultTransactionStatus newTransactionStatus(TransactionDefinition definition,
                                                            Object transaction,
                                                            boolean newTransaction,
                                                            boolean newSynchronization,
                                                            boolean debug,
                                                            Object suspendedResources) {
        boolean actualNewSynchronization = newSynchronization && !TransactionSynchronizationManager.isSynchronizationActive();
        return new DefaultTransactionStatus(
                transaction, newTransaction, actualNewSynchronization, definition.isReadOnly(), debug, suspendedResources
        );
    }

    /**
     * 根据需要初始化事务同步
     */
    protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
        if (status.isNewSynchronization()) {
            TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
                    definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ? definition.getIsolationLevel() : null
            );
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
            TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    /**
     * 确定用于给定定义的实际超时。
     * 如果事务定义未指定非默认值，则将返回到此管理器的默认超时。
     *
     * @param definition 事务定义
     * @return 要使用的实际超时
     * @see TransactionDefinition#getTimeout()
     * @see #setDefaultTimeout
     */
    protected int determineTimeout(TransactionDefinition definition) {
        if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
            return definition.getTimeout();
        }
        return getDefaultTimeout();
    }

    /**
     * 挂起给定的事务。首先挂起事务同步，然后委托给{@code doSuspend}模板方法。
     *
     * @param transaction 当前事务对象（或null仅挂起活动同步，如果有）
     * @return 保存挂起资源的对象（如果事务和同步都不活动，则为null）
     * @see #doSuspend
     * @see #resume
     */
    protected final SuspendedResourcesHolder suspend(Object transaction) throws TransactionException {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
            try {
                Object suspendedResources = null;
                if (transaction != null) {
                    suspendedResources = doSuspend(transaction);
                }
                String name = TransactionSynchronizationManager.getCurrentTransactionName();
                TransactionSynchronizationManager.setCurrentTransactionName(null);
                boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
                Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
                TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
                boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
                TransactionSynchronizationManager.setActualTransactionActive(false);
                return new SuspendedResourcesHolder(
                        suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive
                );
            } catch (RuntimeException | Error ex) {
                // doSuspend failed - original transaction is still active...
                doResumeSynchronization(suspendedSynchronizations);
                throw ex;
            }
        } else if (transaction != null) {
            // Transaction active but no synchronization active.
            Object suspendedResources = doSuspend(transaction);
            return new SuspendedResourcesHolder(suspendedResources);
        } else {
            // Neither transaction nor synchronization active.
            return null;
        }
    }

    /**
     * 恢复给定的事务。首先委托给{@code doResume}模板方法，然后恢复事务同步。
     *
     * @param transaction     当前事务对象
     * @param resourcesHolder 保存挂起资源的对象，由suspend返回（或null仅恢复同步，如果有的话）
     * @see #doResume
     * @see #suspend
     */
    protected final void resume(Object transaction, SuspendedResourcesHolder resourcesHolder) throws TransactionException {
        if (resourcesHolder != null) {
            Object suspendedResources = resourcesHolder.suspendedResources;
            if (suspendedResources != null) {
                doResume(transaction, suspendedResources);
            }
            List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
            if (suspendedSynchronizations != null) {
                TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
                TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
                TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
                doResumeSynchronization(suspendedSynchronizations);
            }
        }
    }

    /**
     * 内部事务开始失败后恢复外部事务
     */
    private void resumeAfterBeginException(Object transaction, SuspendedResourcesHolder suspendedResources, Throwable beginEx) {
        try {
            resume(transaction, suspendedResources);
        } catch (RuntimeException | Error resumeEx) {
            String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
            logger.error(exMessage, beginEx);
            throw resumeEx;
        }
    }

    /**
     * 挂起所有当前同步并停用当前线程的事务同步
     *
     * @return 挂起的TransactionSynchronization对象列表
     */
    private List<TransactionSynchronization> doSuspendSynchronization() {
        List<TransactionSynchronization> suspendedSynchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : suspendedSynchronizations) {
            synchronization.suspend();
        }
        TransactionSynchronizationManager.clearSynchronization();
        return suspendedSynchronizations;
    }

    /**
     * 重新激活当前线程的事务同步，并恢复所有给定的同步
     *
     * @param suspendedSynchronizations TransactionSynchronization对象的列表
     */
    private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
        TransactionSynchronizationManager.initSynchronization();
        for (TransactionSynchronization synchronization : suspendedSynchronizations) {
            synchronization.resume();
            TransactionSynchronizationManager.registerSynchronization(synchronization);
        }
    }

    /**
     * 此提交实现处理参与现有事务和编程回滚请求的情况。委托{@code isRollbackOnly}, {@code doCommit}和{@code rollback}。
     *
     * @see TransactionStatus#isRollbackOnly()
     * @see #doCommit
     * @see #rollback
     */
    @Override
    public final void commit(TransactionStatus status) throws TransactionException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException(
                    "Transaction is already completed - do not call commit or rollback more than once per transaction"
            );
        }
        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        if (defStatus.isLocalRollbackOnly()) {
            if (defStatus.isDebug()) {
                logger.debug("Transactional code has requested rollback");
            }
            processRollback(defStatus, false);
            return;
        }
        if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
            if (defStatus.isDebug()) {
                logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
            }
            processRollback(defStatus, true);
            return;
        }
        processCommit(defStatus);
    }

    /**
     * 处理实际提交。已检查并应用仅回滚标志。
     *
     * @param status 表示事务的对象
     * @throws TransactionException 如果提交失败
     */
    private void processCommit(DefaultTransactionStatus status) throws TransactionException {
        try {
            boolean beforeCompletionInvoked = false;
            try {
                boolean unexpectedRollback = false;
                prepareForCommit(status);
                triggerBeforeCommit(status);
                triggerBeforeCompletion(status);
                beforeCompletionInvoked = true;
                if (status.hasSavepoint()) {
                    if (status.isDebug()) {
                        logger.debug("Releasing transaction savepoint");
                    }
                    unexpectedRollback = status.isGlobalRollbackOnly();
                    status.releaseHeldSavepoint();
                } else if (status.isNewTransaction()) {
                    if (status.isDebug()) {
                        logger.debug("Initiating transaction commit");
                    }
                    unexpectedRollback = status.isGlobalRollbackOnly();
                    doCommit(status);
                } else if (isFailEarlyOnGlobalRollbackOnly()) {
                    unexpectedRollback = status.isGlobalRollbackOnly();
                }
                // Throw UnexpectedRollbackException if we have a global rollback-only
                // marker but still didn't get a corresponding exception from commit.
                if (unexpectedRollback) {
                    throw new UnexpectedRollbackException(
                            "Transaction silently rolled back because it has been marked as rollback-only"
                    );
                }
            } catch (UnexpectedRollbackException ex) {
                // can only be caused by doCommit
                triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
                throw ex;
            } catch (TransactionException ex) {
                // can only be caused by doCommit
                if (isRollbackOnCommitFailure()) {
                    doRollbackOnCommitException(status, ex);
                } else {
                    triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
                }
                throw ex;
            } catch (RuntimeException | Error ex) {
                if (!beforeCompletionInvoked) {
                    triggerBeforeCompletion(status);
                }
                doRollbackOnCommitException(status, ex);
                throw ex;
            }
            // Trigger afterCommit callbacks, with an exception thrown there
            // propagated to callers but the transaction still considered as committed.
            try {
                triggerAfterCommit(status);
            } finally {
                triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
            }
        } finally {
            cleanupAfterCompletion(status);
        }
    }

    /**
     * 回滚的这种实现处理参与现有事务的情况。仅代表{@code doRollback}和{@code doSetRollbackOnly}。
     *
     * @see #doRollback
     * @see #doSetRollbackOnly
     */
    @Override
    public final void rollback(TransactionStatus status) throws TransactionException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException(
                    "Transaction is already completed - do not call commit or rollback more than once per transaction"
            );
        }
        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        processRollback(defStatus, false);
    }

    /**
     * 处理实际回滚。已检查完成标志
     *
     * @param status 表示事务的对象
     * @throws TransactionException 如果回滚失败
     */
    private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
        try {
            boolean unexpectedRollback = unexpected;
            try {
                triggerBeforeCompletion(status);
                if (status.hasSavepoint()) {
                    if (status.isDebug()) {
                        logger.debug("Rolling back transaction to savepoint");
                    }
                    status.rollbackToHeldSavepoint();
                } else if (status.isNewTransaction()) {
                    if (status.isDebug()) {
                        logger.debug("Initiating transaction rollback");
                    }
                    doRollback(status);
                } else {
                    // Participating in larger transaction
                    if (status.hasTransaction()) {
                        if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
                            if (status.isDebug()) {
                                logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
                            }
                            doSetRollbackOnly(status);
                        } else {
                            if (status.isDebug()) {
                                logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
                            }
                        }
                    } else {
                        logger.debug("Should roll back transaction but cannot - no transaction available");
                    }
                    // Unexpected rollback only matters here if we're asked to fail early
                    if (!isFailEarlyOnGlobalRollbackOnly()) {
                        unexpectedRollback = false;
                    }
                }
            } catch (RuntimeException | Error ex) {
                triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
                throw ex;
            }
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
            // Raise UnexpectedRollbackException if we had a global rollback-only marker
            if (unexpectedRollback) {
                throw new UnexpectedRollbackException("Transaction rolled back because it has been marked as rollback-only");
            }
        } finally {
            cleanupAfterCompletion(status);
        }
    }

    /**
     * 调用{@code doRollback}，正确处理回滚异常。
     *
     * @param status 表示事务的对象
     * @param ex     引发的应用程序异常或错误
     * @throws TransactionException 如果回滚失败
     * @see #doRollback
     */
    private void doRollbackOnCommitException(DefaultTransactionStatus status, Throwable ex) throws TransactionException {
        try {
            if (status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.debug("Initiating transaction rollback after commit exception", ex);
                }
                doRollback(status);
            } else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
                if (status.isDebug()) {
                    logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
                }
                doSetRollbackOnly(status);
            }
        } catch (RuntimeException | Error rbex) {
            logger.error("Commit exception overridden by rollback exception", ex);
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
            throw rbex;
        }
        triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
    }

    /**
     * 触发 {@code beforeCommit} 回调
     *
     * @param status 表示事务的对象
     */
    protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
        }
    }

    /**
     * 触发 {@code beforeCompletion} 回调。
     *
     * @param status 表示事务的对象
     */
    protected final void triggerBeforeCompletion(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            TransactionSynchronizationUtils.triggerBeforeCompletion();
        }
    }

    /**
     * 触发 {@code afterCommit} 回调。
     *
     * @param status 表示事务的对象
     */
    private void triggerAfterCommit(DefaultTransactionStatus status) {
        if (status.isNewSynchronization()) {
            TransactionSynchronizationUtils.triggerAfterCommit();
        }
    }

    /**
     * 触发 {@code afterCompletion} 回调。
     *
     * @param status           表示事务的对象
     * @param completionStatus 根据TransactionSynchronization常量的完成状态
     */
    private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus) {
        if (status.isNewSynchronization()) {
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationManager.clearSynchronization();
            if (!status.hasTransaction() || status.isNewTransaction()) {
                // No transaction or new transaction for the current scope ->
                // invoke the afterCompletion callbacks immediately
                invokeAfterCompletion(synchronizations, completionStatus);
            } else if (!synchronizations.isEmpty()) {
                // Existing transaction that we participate in, controlled outside
                // of the scope of this clever transaction manager -> try to register
                // an afterCompletion callback with the existing (JTA) transaction.
                registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
            }
        }
    }

    /**
     * 实际调用给定TransactionSynchronization对象的{@code afterCompletion}方法。
     * <p>由此抽象管理器本身调用，或由{@code registerAfterCompletionWithExistingTransaction}回调的特殊实现调用。
     *
     * @param synchronizations TransactionSynchronization对象的列表
     * @param completionStatus 根据TransactionSynchronization接口中的常量的完成状态
     * @see #registerAfterCompletionWithExistingTransaction(Object, List)
     * @see TransactionSynchronization#STATUS_COMMITTED
     * @see TransactionSynchronization#STATUS_ROLLED_BACK
     * @see TransactionSynchronization#STATUS_UNKNOWN
     */
    protected final void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
        TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
    }

    /**
     * 完成后进行清理，必要时清除同步，并调用 doCleanupAfterCompletion
     *
     * @param status 表示事务的对象
     * @see #doCleanupAfterCompletion
     */
    private void cleanupAfterCompletion(DefaultTransactionStatus status) {
        status.setCompleted();
        if (status.isNewSynchronization()) {
            TransactionSynchronizationManager.clear();
        }
        if (status.isNewTransaction()) {
            doCleanupAfterCompletion(status.getTransaction());
        }
        if (status.getSuspendedResources() != null) {
            if (status.isDebug()) {
                logger.debug("Resuming suspended transaction after completion of inner transaction");
            }
            Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
            resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
        }
    }

    //---------------------------------------------------------------------
    // Template methods to be implemented in subclasses
    //---------------------------------------------------------------------

    /**
     * 返回当前事务状态的事务对象。
     * <p>返回的对象通常特定于具体的事务管理器实现，以可修改的方式携带相应的事务状态。
     * 此对象将直接或作为DefaultTransactionStatus实例的一部分传递到其他模板方法（例如doBegin和doCommit）。
     * <p>返回的对象应该包含关于任何现有事务的信息，即在事务管理器上当前{@code getTransaction}调用之前已经启动的事务。
     * 因此，{@code doGetTransaction}实现通常会查找现有事务，并在返回的事务对象中存储相应的状态。
     *
     * @return 当前事务对象
     * @throws CannotCreateTransactionException 如果事务支持不可用
     * @throws TransactionException                                    查找或系统错误时
     * @see #doBegin
     * @see #doCommit
     * @see #doRollback
     * @see DefaultTransactionStatus#getTransaction
     */
    protected abstract Object doGetTransaction() throws TransactionException;

    /**
     * 检查给定的事务对象是否指示现有事务（即已启动的事务）。
     * <p>将根据新事务的指定传播行为评估结果。
     * 现有事务可能会被挂起（如果是PROPAGATION_REQUIRES_NEW），
     * 或者新事务可能会参与现有事务（如果是PROPAGATION_REQUIRED）。
     * 默认实现返回false，假设通常不支持参与现有事务。当然，我们鼓励子类提供这种支持。
     *
     * @param transaction doGetTransaction返回的事务对象
     * @return 如果存在现有事务
     * @throws TransactionException 如果出现系统错误
     * @see #doGetTransaction
     */
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        return false;
    }

    /**
     * 返回是否对嵌套事务使用保存点。
     * <p>默认值为true，这会导致委派到DefaultTransactionStatus以创建和保存点。
     * 如果事务对象未实现SavepointManager接口，则将引发NestedTransactionNotSupportedException。
     * 否则，将要求SavepointManager创建一个新的保存点，以划分嵌套事务的开始。
     * <p>子类可以重写此项以返回false，从而在现有事务的上下文中进一步调用{@code doBegin}。
     * 在这种情况下，{@code doBegin}实现需要相应地处理这个问题。例如，这适用于JTA。
     *
     * @see DefaultTransactionStatus#createAndHoldSavepoint
     * @see DefaultTransactionStatus#rollbackToHeldSavepoint
     * @see DefaultTransactionStatus#releaseHeldSavepoint
     * @see #doBegin
     */
    protected boolean useSavepointForNestedTransaction() {
        return true;
    }

    /**
     * 根据给定的事务定义，使用语义开始新事务。不必关心应用传播行为，因为这已经由这个抽象管理器处理了。
     * <p>当事务管理器决定实际启动新事务时，将调用此方法。之前没有任何事务，或者之前的事务已暂停。
     * <p>一个特殊的场景是没有保存点的嵌套事务：如果{@code useSavepointForNestedTransaction()}返回“false”，
     * 则在必要时将调用此方法来启动嵌套事务。在这样的上下文中，将有一个活动事务：此方法的实现必须检测到这一点并启动适当的嵌套事务。
     *
     * @param transaction 返回的事务对象 {@code doGetTransaction}
     * @param definition  TransactionDefinition实例，描述传播行为、隔离级别、只读标志、超时和事务名称
     * @throws TransactionException                                          如果出现创建或系统错误
     * @throws NestedTransactionNotSupportedException 如果基础事务不支持嵌套
     */
    protected abstract void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException;

    /**
     * 挂起当前事务的资源。事务同步将已挂起。
     * <p>默认实现抛出TransactionSuspensionNotSupportedException，假设通常不支持事务暂停。
     *
     * @param transaction 返回的事务对象 {@code doGetTransaction}
     * @return 保存挂起资源的对象（在将其传递到doResume时将保持未经检查）
     * @throws TransactionSuspensionNotSupportedException 如果事务管理器实现不支持挂起
     * @throws TransactionException                                              如果出现系统错误
     * @see #doResume
     */
    protected Object doSuspend(Object transaction) throws TransactionException {
        throw new TransactionSuspensionNotSupportedException(
                "Transaction manager [" + getClass().getName() + "] does not support transaction suspension"
        );
    }

    /**
     * 恢复当前事务的资源。之后将恢复事务同步。
     * <p>默认实现抛出TransactionSuspensionNotSupportedException，假设通常不支持事务暂停。
     *
     * @param transaction        返回的事务对象 {@code doGetTransaction}
     * @param suspendedResources 保存挂起资源的对象，如doSuspend返回的
     * @throws TransactionSuspensionNotSupportedException 如果事务管理器实现不支持恢复
     * @throws TransactionException                                              如果出现系统错误
     * @see #doSuspend
     */
    protected void doResume(Object transaction, Object suspendedResources) throws TransactionException {
        throw new TransactionSuspensionNotSupportedException(
                "Transaction manager [" + getClass().getName() + "] does not support transaction suspension"
        );
    }

    /**
     * 返回是否对仅以全局方式标记为回滚的事务调用{@code doCommit}。
     * <p>如果应用程序在本地仅通过TransactionStatus将事务设置为回滚，而仅由事务协调器将事务本身标记为回滚，则不适用。
     * <p>默认值为“false”：本地事务策略通常不在事务本身中保留仅回滚标记，因此它们不能将仅回滚事务作为事务提交的一部分来处理。
     * 因此，在这种情况下，AbstractPlatformTransactionManager将触发回滚，随后引发意外的回滚异常。
     * <p>如果具体事务管理器期望{@code doCommit}调用，即使对于仅回滚的事务，也会重写此选项以返回“true”，从而允许在那里进行特殊处理。
     * 例如，JTA就是这样，其中{@code UserTransaction.commit}将检查只读标志本身并引发相应的回滚异常，该异常可能包括特定原因（例如事务超时）。
     * <p>如果此方法返回“true”，但{@code doCommit}实现未引发异常，则此事务管理器本身将引发意外回滚异常。
     * 这不应该是典型的情况；它主要用于检查行为不端的JTA提供程序，即使调用代码没有请求回滚，这些提供程序也会自动回滚。
     *
     * @see #doCommit
     * @see DefaultTransactionStatus#isGlobalRollbackOnly()
     * @see DefaultTransactionStatus#isLocalRollbackOnly()
     * @see TransactionStatus#setRollbackOnly()
     * @see UnexpectedRollbackException
     */
    protected boolean shouldCommitOnGlobalRollbackOnly() {
        return false;
    }

    /**
     * 在{@code beforeCommit}同步回调发生之前，为提交做好准备。
     * <p>请注意，异常将传播到提交调用方，并导致事务回滚。
     *
     * @param status 事务状态
     * @throws RuntimeException 如有错误；将传播到调用方（注意：不要在此处抛出TransactionException子类！）
     */
    protected void prepareForCommit(DefaultTransactionStatus status) {
    }

    /**
     * 执行给定事务的实际提交。
     * <p>实现不需要检查“new transaction”标志或rollback only标志；这之前已经处理过了。
     * 通常，将对传入状态中包含的事务对象执行直接提交。
     *
     * @param status 事务状态
     * @throws TransactionException 如果发生提交或系统错误
     * @see DefaultTransactionStatus#getTransaction
     */
    protected abstract void doCommit(DefaultTransactionStatus status) throws TransactionException;

    /**
     * 执行给定事务的实际回滚。
     * <p>实现不需要检查“new transaction”标志；这之前已经处理过了。
     * 通常，将对传入状态中包含的事务对象执行直接回滚。
     *
     * @param status 事务状态
     * @throws TransactionException 如果出现系统错误
     * @see DefaultTransactionStatus#getTransaction
     */
    protected abstract void doRollback(DefaultTransactionStatus status) throws TransactionException;

    /**
     * 仅设置给定的事务回滚。仅当当前事务参与现有事务时，才在回滚时调用。
     * <p>默认实现抛出非法TransactionStateException，假设通常不支持参与现有事务。当然，我们鼓励子类提供这种支持。
     *
     * @param status 事务状态
     * @throws TransactionException 如果出现系统错误
     */
    protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
        throw new IllegalTransactionStateException(
                "Participating in existing transactions is not supported - when 'isExistingTransaction' " +
                        "returns true, appropriate 'doSetRollbackOnly' behavior must be provided"
        );
    }

    /**
     * 将给定的事务同步列表注册到现有事务。
     * <p>当clever事务管理器的控制以及所有事务同步结束时调用，而事务尚未完成。
     * 例如，当参与现有JTA或EJB CMT事务时就是这种情况。
     * <p>默认实现只是立即调用{@code afterCompletion}方法，并传入“STATUS_UNKNOWN”。如果没有机会确定外部事务的实际结果，这是我们能做的最好的事情。
     *
     * @param transaction      返回的事务对象 {@code doGetTransaction}
     * @param synchronizations TransactionSynchronization对象的列表
     * @throws TransactionException 如果出现系统错误
     * @see #invokeAfterCompletion(List, int)
     * @see TransactionSynchronization#afterCompletion(int)
     * @see TransactionSynchronization#STATUS_UNKNOWN
     */
    protected void registerAfterCompletionWithExistingTransaction(
            Object transaction,
            List<TransactionSynchronization> synchronizations) throws TransactionException {
        logger.debug(
                "Cannot register clever after-completion synchronization with existing transaction - " +
                        "processing clever after-completion callbacks immediately, with outcome status 'unknown'"
        );
        invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
    }

    /**
     * 事务完成后清理资源。
     * <p>在{@code doCommit}和{@code doRollback}执行后，对任何结果调用。
     * <p>默认实现不执行任何操作。不应引发任何异常，而应仅对错误发出警告
     *
     * @param transaction 返回的事务对象 {@code doGetTransaction}
     */
    protected void doCleanupAfterCompletion(Object transaction) {
    }

    //---------------------------------------------------------------------
    // Serialization support
    //---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization; just initialize state after deserialization.
        ois.defaultReadObject();
        // Initialize transient fields.
        this.logger = LoggerFactory.getLogger(getClass());
    }

    /**
     * 暂停资源的持有人。{@code suspend}和{@code resume}在内部使用
     */
    protected static final class SuspendedResourcesHolder {
        private final Object suspendedResources;
        private List<TransactionSynchronization> suspendedSynchronizations;
        private String name;
        private boolean readOnly;
        private Integer isolationLevel;
        private boolean wasActive;

        private SuspendedResourcesHolder(Object suspendedResources) {
            this.suspendedResources = suspendedResources;
        }

        private SuspendedResourcesHolder(Object suspendedResources,
                                         List<TransactionSynchronization> suspendedSynchronizations,
                                         String name,
                                         boolean readOnly,
                                         Integer isolationLevel,
                                         boolean wasActive) {
            this.suspendedResources = suspendedResources;
            this.suspendedSynchronizations = suspendedSynchronizations;
            this.name = name;
            this.readOnly = readOnly;
            this.isolationLevel = isolationLevel;
            this.wasActive = wasActive;
        }
    }
}
