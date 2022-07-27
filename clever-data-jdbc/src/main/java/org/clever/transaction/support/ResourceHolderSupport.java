package org.clever.transaction.support;

import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.TransactionTimedOutException;

import java.util.Date;

/**
 * 为资源持有者提供方便的基类。
 * <p>仅支持参与事务的功能回滚。可以在特定的秒数或毫秒数后过期，以确定事务超时。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:53 <br/>
 *
 * @see org.clever.jdbc.datasource.DataSourceTransactionManager#doBegin(Object, TransactionDefinition)
 * @see org.clever.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
 */
@SuppressWarnings("JavadocReference")
public abstract class ResourceHolderSupport implements ResourceHolder {
    private boolean synchronizedWithTransaction = false;
    private boolean rollbackOnly = false;
    private Date deadline;
    private int referenceCount = 0;
    private boolean isVoid = false;

    /**
     * 将资源标记为与事务同步
     */
    public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
        this.synchronizedWithTransaction = synchronizedWithTransaction;
    }

    /**
     * 返回资源是否与事务同步
     */
    public boolean isSynchronizedWithTransaction() {
        return this.synchronizedWithTransaction;
    }

    /**
     * 将资源事务标记为仅回滚
     */
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    /**
     * 重置此资源事务的仅回滚状态。
     * <p>仅真正打算在保持原始资源运行的自定义回滚步骤之后调用，例如在保存点的情况下。
     *
     * @see org.clever.transaction.SavepointManager#rollbackToSavepoint
     */
    public void resetRollbackOnly() {
        this.rollbackOnly = false;
    }

    /**
     * 返回资源事务是否标记为仅回滚。
     */
    public boolean isRollbackOnly() {
        return this.rollbackOnly;
    }

    /**
     * 设置此对象的超时（秒）。
     *
     * @param seconds 到期前的秒数
     */
    public void setTimeoutInSeconds(int seconds) {
        setTimeoutInMillis(seconds * 1000L);
    }

    /**
     * 设置此对象的超时（毫秒）。
     *
     * @param millis 到期前的毫秒数
     */
    public void setTimeoutInMillis(long millis) {
        this.deadline = new Date(System.currentTimeMillis() + millis);
    }

    /**
     * 返回此对象是否有关联的超时。
     */
    public boolean hasTimeout() {
        return (this.deadline != null);
    }

    /**
     * 返回此对象的过期截止日期。
     *
     * @return 截止日期作为日期对象
     */
    public Date getDeadline() {
        return this.deadline;
    }

    /**
     * 返回此对象的生存时间（秒）。
     * 急切地四舍五入，例如9.00001仍为10。
     *
     * @return 到期前的秒数
     * @throws TransactionTimedOutException 如果截止日期已经到了
     */
    public int getTimeToLiveInSeconds() {
        double diff = ((double) getTimeToLiveInMillis()) / 1000;
        int secs = (int) Math.ceil(diff);
        checkTransactionTimeout(secs <= 0);
        return secs;
    }

    /**
     * 返回此对象的生存时间（毫秒）。
     *
     * @return 到期前的毫秒数
     * @throws TransactionTimedOutException 如果截止日期已经到了
     */
    public long getTimeToLiveInMillis() throws TransactionTimedOutException {
        if (this.deadline == null) {
            throw new IllegalStateException("No timeout specified for this resource holder");
        }
        long timeToLive = this.deadline.getTime() - System.currentTimeMillis();
        checkTransactionTimeout(timeToLive <= 0);
        return timeToLive;
    }

    /**
     * 仅在达到截止日期时设置事务回滚，并引发TransactionTimedOutException。
     */
    private void checkTransactionTimeout(boolean deadlineReached) throws TransactionTimedOutException {
        if (deadlineReached) {
            setRollbackOnly();
            throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.deadline);
        }
    }

    /**
     * 将引用计数增加1，因为持有人已被请求（即有人请求其持有的资源）。
     */
    public void requested() {
        this.referenceCount++;
    }

    /**
     * 将引用计数减少1，因为持有者已被释放（即某人释放了其所持有的资源）。
     */
    public void released() {
        this.referenceCount--;
    }

    /**
     * 返回是否仍有对此保持架的打开引用。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isOpen() {
        return (this.referenceCount > 0);
    }

    /**
     * 清除此资源持有者的事务状态。
     */
    public void clear() {
        this.synchronizedWithTransaction = false;
        this.rollbackOnly = false;
        this.deadline = null;
    }

    /**
     * 重置此资源持有者-事务状态以及引用计数。
     */
    @Override
    public void reset() {
        clear();
        this.referenceCount = 0;
    }

    @Override
    public void unbound() {
        this.isVoid = true;
    }

    @Override
    public boolean isVoid() {
        return this.isVoid;
    }
}
