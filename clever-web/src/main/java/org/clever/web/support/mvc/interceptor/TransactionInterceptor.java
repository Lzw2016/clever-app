package org.clever.web.support.mvc.interceptor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.OrderIncrement;
import org.clever.data.jdbc.DataSourceAdmin;
import org.clever.data.jdbc.Jdbc;
import org.clever.jdbc.datasource.DataSourceTransactionManager;
import org.clever.transaction.TransactionStatus;
import org.clever.transaction.support.DefaultTransactionDefinition;
import org.clever.util.Assert;
import org.clever.web.config.MvcConfig;
import org.clever.web.exception.MultiExceptionWrapper;
import org.clever.web.support.mvc.HandlerContext;
import org.clever.web.support.mvc.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC事务处理
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/21 15:43 <br/>
 */
@Slf4j
public class TransactionInterceptor implements HandlerInterceptor {
    private static final ThreadLocal<List<TransactionInfo>> TX_INFO = new ThreadLocal<>();

    private final String defDatasource;
    private final MvcConfig.TransactionalConfig defTransactional;

    public TransactionInterceptor(String defDatasource, MvcConfig.TransactionalConfig defTransactional) {
        Assert.isNotBlank(defDatasource, "参数 defDatasource 不能为空");
        Assert.notNull(defTransactional, "参数 defTransactional 不能为null");
        this.defDatasource = defDatasource;
        this.defTransactional = defTransactional;
    }

    @Override
    public boolean beforeHandle(HandlerContext context) throws Exception {
        boolean disabledTX;
        final Method method = context.getHandleMethod().getMethod();
        final Transactional tx = method.getAnnotation(Transactional.class);
        MvcConfig.TransactionalConfig txConfig;
        if (tx == null) {
            // 使用defTransactional配置
            txConfig = defTransactional;
            disabledTX = txConfig.getDatasource() == null || txConfig.getDatasource().isEmpty();
            // 设置TransactionalConfig默认值
            MvcConfig.TransactionalConfig defTX = new MvcConfig.TransactionalConfig();
            if (txConfig.getPropagation() == null) {
                txConfig.setPropagation(defTX.getPropagation());
            }
            if (txConfig.getIsolation() == null) {
                txConfig.setIsolation(defTX.getIsolation());
            }
        } else {
            // 使用@Transactional注解
            txConfig = new MvcConfig.TransactionalConfig();
            txConfig.setDatasource(Arrays.stream(tx.datasource()).collect(Collectors.toList()));
            // 使用默认的数据源
            if (txConfig.getDatasource() == null || txConfig.getDatasource().isEmpty()) {
                txConfig.setDatasource(Collections.singletonList(defDatasource));
            }
            txConfig.setPropagation(tx.propagation());
            txConfig.setIsolation(tx.isolation());
            txConfig.setTimeout(tx.timeout());
            txConfig.setReadOnly(tx.readOnly());
            disabledTX = tx.disabled();
        }
        // 未启用事务
        if (disabledTX) {
            return true;
        }
        // 保证datasource唯一且顺序不变
        final Set<String> unique = new HashSet<>(txConfig.getDatasource().size());
        final List<String> datasource = new ArrayList<>(txConfig.getDatasource().size());
        for (String ds : txConfig.getDatasource()) {
            if (unique.add(ds)) {
                datasource.add(ds);
            }
        }
        // 启用事务
        List<TransactionInfo> txInfos = new ArrayList<>(datasource.size());
        TX_INFO.set(txInfos);
        for (String ds : datasource) {
            Jdbc jdbc = DataSourceAdmin.getJdbc(ds);
            DataSourceTransactionManager transactionManager = jdbc.getTransactionManager();
            DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
            txDefinition.setName(jdbc.getNextTransactionName());
            txDefinition.setPropagationBehavior(txConfig.getPropagation().value());
            txDefinition.setIsolationLevel(txConfig.getIsolation().value());
            txDefinition.setTimeout(txConfig.getTimeout());
            txDefinition.setReadOnly(txConfig.isReadOnly());
            // 开启事务
            TransactionStatus transactionStatus = transactionManager.getTransaction(txDefinition);
            txInfos.add(TransactionInfo.create(ds, transactionManager, transactionStatus));
        }
        return true;
    }

    @Override
    public void finallyHandle(HandlerContext.Finally context) throws Exception {
        final List<TransactionInfo> txInfos = TX_INFO.get();
        if (txInfos == null) {
            return;
        }
        TX_INFO.remove();
        // 提交事务时需要反转,以开启事务相反的顺序提交事务(先提交内层的事务,再提交外层事务)
        Collections.reverse(txInfos);
        final List<Throwable> errList = new ArrayList<>(txInfos.size());
        final boolean commit = context.getException() == null;
        if (commit) {
            // 需要提交事务
            for (TransactionInfo txInfo : txInfos) {
                try {
                    txInfo.tryCommit();
                } catch (Throwable e) {
                    errList.add(e);
                    log.info("提交事务失败,数据源: [{}]", txInfo.datasource, e);
                }
            }
        } else {
            // 需要回滚事务
            for (TransactionInfo txInfo : txInfos) {
                try {
                    txInfo.rollback();
                } catch (Throwable e) {
                    errList.add(e);
                    log.info("回滚事务失败,数据源: [{}]", txInfo.datasource, e);
                }
            }
        }
        if (!errList.isEmpty()) {
            throw new MultiExceptionWrapper(errList.toArray(new Throwable[0]));
        }
    }

    @Override
    public double getOrder() {
        return OrderIncrement.NORMAL;
    }

    @Data
    protected static class TransactionInfo {
        public static TransactionInfo create(String datasource, DataSourceTransactionManager transactionManager, TransactionStatus transactionStatus) {
            return new TransactionInfo(datasource, transactionManager, transactionStatus);
        }

        private final String datasource;
        private final DataSourceTransactionManager transactionManager;
        private final TransactionStatus transactionStatus;

        public void rollback() {
            if (transactionStatus.isCompleted()) {
                return;
            }
            transactionManager.rollback(transactionStatus);
        }

        public void tryCommit() {
            if (transactionStatus.isCompleted()) {
                return;
            }
            if (transactionStatus.isRollbackOnly()) {
                transactionManager.rollback(transactionStatus);
            } else {
                transactionManager.commit(transactionStatus);
            }
        }
    }
}
