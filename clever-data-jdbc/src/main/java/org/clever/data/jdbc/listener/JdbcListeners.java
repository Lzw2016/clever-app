package org.clever.data.jdbc.listener;

import org.clever.data.dynamic.sql.dialect.DbType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/05 19:03 <br/>
 */
public class JdbcListeners implements JdbcListener {
    private final List<JdbcListener> listeners = new ArrayList<>();

    public void add(JdbcListener listener) {
        if (listener instanceof JdbcListeners) {
            for (JdbcListener item : ((JdbcListeners) listener).listeners) {
                add(item);
            }
        } else {
            listeners.add(listener);
        }
        // 监听器排序
        listeners.sort(Comparator.comparingDouble(JdbcListener::getOrder));
    }

    @Override
    public void beforeExec(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate) {
        for (JdbcListener listener : listeners) {
            listener.beforeExec(dbType, jdbcTemplate);
        }
    }

    @Override
    public void afterExec(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate, Exception exception) {
        for (JdbcListener listener : listeners) {
            listener.afterExec(dbType, jdbcTemplate, exception);
        }
    }
}
