package org.clever.web.mvc.interceptor;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.clever.core.Assert;
import org.clever.core.OrderIncrement;
import org.clever.data.jdbc.DataSourceAdmin;
import org.clever.data.jdbc.Jdbc;
import org.clever.web.config.MvcConfig;
import org.clever.web.exception.MultiExceptionWrapper;
import org.clever.web.mvc.HandlerContext;
import org.clever.web.mvc.annotation.Transactional;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.DefaultTransactionDefinition;

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
    private static final Class<org.springframework.transaction.annotation.Transactional> SPRING_ANNOTATION = org.springframework.transaction.annotation.Transactional.class;
    private static final Propagation DEF_PROPAGATION = Propagation.REQUIRED;
    private static final Isolation DEF_ISOLATION = Isolation.DEFAULT;
    private static final ThreadLocal<List<TransactionInfo>> TX_INFO = new ThreadLocal<>();

    @Getter
    private final List<String> defDatasource;
    @Getter
    private final MvcConfig.TransactionalConfig defTransactional;

    public TransactionInterceptor(MvcConfig mvcConfig) {
        Assert.notNull(mvcConfig, "参数 mvcConfig 不能为null");
        this.defDatasource = Collections.unmodifiableList(mvcConfig.getTransactionalDefDatasource());
        this.defTransactional = Optional.of(mvcConfig.getDefTransactional()).orElseGet(() -> {
            mvcConfig.setDefTransactional(new MvcConfig.TransactionalConfig());
            return mvcConfig.getDefTransactional();
        });
    }

    protected TransactionalAnnotation readTransactionalConfig(final Method method) {
        Transactional tx = method.getAnnotation(Transactional.class);
        if (tx != null) {
            return new TransactionalAnnotation(
                tx.disabled(),
                tx.datasource(),
                tx.propagation(),
                tx.isolation(),
                tx.timeout(),
                tx.readOnly()
            );
        }
        org.springframework.transaction.annotation.Transactional springTX = method.getAnnotation(SPRING_ANNOTATION);
        if (springTX != null) {
            return new TransactionalAnnotation(
                false,
                defDatasource.toArray(new String[0]),
                springTX.propagation(),
                springTX.isolation(),
                NumberUtils.toInt(springTX.timeoutString(), springTX.timeout()),
                springTX.readOnly()
            );
        }
        return null;
    }

    @Override
    public boolean beforeHandle(HandlerContext context) {
        boolean disabledTX;
        final Method method = context.getHandleMethod().getMethod();
        final TransactionalAnnotation tx = readTransactionalConfig(method);
        MvcConfig.TransactionalConfig txConfig;
        if (tx == null) {
            // 使用defTransactional配置
            txConfig = defTransactional;
            disabledTX = txConfig.getDatasource() == null || txConfig.getDatasource().isEmpty();
            // 设置TransactionalConfig默认值
            if (txConfig.getPropagation() == null) {
                txConfig.setPropagation(DEF_PROPAGATION);
            }
            if (txConfig.getIsolation() == null) {
                txConfig.setIsolation(DEF_ISOLATION);
            }
        } else {
            // 使用@Transactional注解
            txConfig = new MvcConfig.TransactionalConfig();
            txConfig.setDatasource(Arrays.stream(tx.datasource).collect(Collectors.toList()));
            // 使用默认的数据源
            if (txConfig.getDatasource() == null || txConfig.getDatasource().isEmpty()) {
                txConfig.setDatasource(defDatasource);
            }
            txConfig.setPropagation(tx.propagation);
            txConfig.setIsolation(tx.isolation);
            txConfig.setTimeout(tx.timeout);
            txConfig.setReadOnly(tx.readOnly);
            disabledTX = tx.disabled;
        }
        // 未启用事务
        if (disabledTX) {
            return true;
        }
        // 保证datasource非空、唯一、顺序不变
        final Set<String> unique = new HashSet<>(txConfig.getDatasource().size());
        final List<String> datasource = new ArrayList<>(txConfig.getDatasource().size());
        for (String ds : txConfig.getDatasource()) {
            if (StringUtils.isBlank(ds)) {
                continue;
            }
            if (unique.add(ds)) {
                datasource.add(ds);
            }
        }
        // datasource 为空则直接返回
        if (datasource.isEmpty()) {
            return true;
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

    protected record TransactionalAnnotation(
        boolean disabled,
        String[] datasource,
        Propagation propagation,
        Isolation isolation,
        int timeout,
        boolean readOnly) {
    }
}
