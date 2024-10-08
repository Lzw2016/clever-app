package org.clever.data.jdbc.support;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.listener.JdbcListeners;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 调用存储过程或函数的JDBC封装<br/>
 * 1.[bugfix]Postgresql不支持 {call procedure_name(...params)} 的形式<br/>
 * 2.支持 JdbcListeners
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/19 10:50 <br/>
 */
public class ProcedureJdbcCall extends SimpleJdbcCall {
    private final Jdbc jdbc;
    private final JdbcListeners listeners;

    public ProcedureJdbcCall(Jdbc jdbc) {
        super(jdbc.getJdbcTemplate().getJdbcTemplate());
        this.jdbc = jdbc;
        this.listeners = ReflectionsUtils.getFieldValue(jdbc, "listeners");
    }

    /**
     * bug fix 标准sql调用存贮过程使用 {call procedure_name(...params)} 但是 postgresql 不支持，这里改造一下
     */
    @Override
    protected void onCompileInternal() {
        String callString = this.getCallString();
        if (DbType.POSTGRE_SQL.equals(jdbc.getDbType())
            && callString != null
            && StringUtils.startsWith(callString, "{call")
            && StringUtils.endsWith(callString, ")}")) {
            callString = callString.substring(1, callString.length() - 1);
            ReflectionsUtils.setFieldValue(this, "callString", callString);
            CallableStatementCreatorFactory cscf = ReflectionsUtils.getFieldValue(this, "callableStatementFactory");
            if (!Objects.equals(cscf.getCallString(), callString)) {
                ReflectionsUtils.setFieldValue(
                    this,
                    "callableStatementFactory",
                    mutate(cscf, callString)
                );
            }
        }
    }

    protected CallableStatementCreatorFactory mutate(CallableStatementCreatorFactory cscf, String callString) {
        CallableStatementCreatorFactory result = new CallableStatementCreatorFactory(
            callString,
            ReflectionsUtils.getFieldValue(cscf, "declaredParameters")
        );
        ReflectionsUtils.setFieldValue(
            result,
            "resultSetType",
            ReflectionsUtils.getFieldValue(cscf, "resultSetType")
        );
        ReflectionsUtils.setFieldValue(
            result,
            "updatableResults",
            ReflectionsUtils.getFieldValue(cscf, "updatableResults")
        );
        return result;
    }

    @NotNull
    @Override
    protected Map<String, Object> doExecute(@NotNull SqlParameterSource parameterSource) {
        return doExecute(() -> super.doExecute(parameterSource));
    }

    @NotNull
    @Override
    protected Map<String, Object> doExecute(@NotNull Object... args) {
        return doExecute(() -> super.doExecute(args));
    }

    @NotNull
    @Override
    protected Map<String, Object> doExecute(@NotNull Map<String, ?> args) {
        return doExecute(() -> super.doExecute(args));
    }

    /**
     * 支持 JdbcListeners
     */
    private <T> T doExecute(Supplier<T> supplier) {
        Exception exception = null;
        try {
            listeners.beforeExec(jdbc.getDbType(), jdbc.getJdbcTemplate());
            return supplier.get();
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            listeners.afterExec(jdbc.getDbType(), jdbc.getJdbcTemplate(), exception);
        }
    }
}
